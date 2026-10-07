package de.vptr.aimathtutor.service.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import de.vptr.aimathtutor.util.ExecutorShutdownUtil;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory service for tracking and throttling login attempts. Implements per-username and per-IP exponential backoff
 * to mitigate brute-force attacks. Uses bounded cache with automatic cleanup to prevent DoS via unbounded growth.
 */
@ApplicationScoped
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_LOCKOUT_SECONDS = 30;
    private static final long MAX_LOCKOUT_SECONDS = 3600; // 1 hour
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final long CLEANUP_INTERVAL_SECONDS = 300; // 5 minutes

    // Windowed limits: at most maxAttempts from the first attempt of a window, then locked for lockSeconds after the
    // last one; the window restarts once it or the lock has passed.
    private static final Policy ACCOUNT_POLICY = new Policy(25, 600, 900); // 10 min lock, 15 min window
    // Password changes, per session: 5 guesses per 15 minutes, so the cap cannot reset every minute
    private static final Policy PASSWORD_CHANGE_POLICY = new Policy(5, 900, 900);

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, WindowedAttempt> accountAttempts = new ConcurrentHashMap<>();
    // Own key space: login keys include client-supplied X-Forwarded-For text, so they must never share a map with the
    // per-session password-change tokens.
    private final Map<String, WindowedAttempt> passwordChangeAttempts = new ConcurrentHashMap<>();

    /** Time source; replaced in tests to cross the throttle windows without waiting. */
    Clock clock = Clock.systemUTC();

    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    void init() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "LoginAttemptService-Cleanup");
            t.setDaemon(true);
            return t;
        });
        final var _ = this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void destroy() {
        ExecutorShutdownUtil.shutdownGracefully(this.cleanupExecutor, 5);
    }

    /**
     * Periodically removes expired entries to prevent unbounded memory growth.
     */
    private void cleanupExpiredEntries() {
        this.attempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
        final var now = this.clock.instant();
        this.accountAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired(ACCOUNT_POLICY, now));
        this.passwordChangeAttempts.entrySet()
                .removeIf(entry -> entry.getValue().isExpired(PASSWORD_CHANGE_POLICY, now));

        // If still over limit after cleanup, remove oldest entries
        if (this.attempts.size() > MAX_CACHE_SIZE) {
            this.attempts.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().lastAttempt.compareTo(e2.getValue().lastAttempt))
                    .limit(this.attempts.size() - MAX_CACHE_SIZE)
                    .forEach(entry -> this.attempts.remove(entry.getKey(), entry.getValue()));
        }
        trimOldest(this.accountAttempts, attempt -> true);
        // Never lift a password-change lock: a locked bucket stops updating, so it would be the oldest, and fresh
        // sessions could flood it out. Its entries need a signed-in session each, so keeping them stays bounded.
        trimOldest(this.passwordChangeAttempts, attempt -> attempt.count < PASSWORD_CHANGE_POLICY.maxAttempts);
    }

    private static void trimOldest(final Map<String, WindowedAttempt> map, final Predicate<WindowedAttempt> evictable) {
        if (map.size() > MAX_CACHE_SIZE) {
            map.entrySet().stream().filter(entry -> evictable.test(entry.getValue()))
                    .sorted((e1, e2) -> e1.getValue().lastAttempt.compareTo(e2.getValue().lastAttempt))
                    .limit(map.size() - MAX_CACHE_SIZE).forEach(entry -> map.remove(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Records a failed login attempt for the given key (username or IP). Returns the number of seconds the client
     * should wait before retrying. Enforces maximum cache size to prevent DoS attacks.
     *
     * @param key
     *            the username or IP address
     * @return lockout duration in seconds
     */
    public long recordFailedAttempt(final String key) {
        // Enforce size limit before adding new entries
        if (this.attempts.size() >= MAX_CACHE_SIZE && !this.attempts.containsKey(key)) {
            this.cleanupExpiredEntries();
        }

        final LoginAttempt attempt = this.attempts.compute(key, (k, v) -> {
            if (v == null || v.isExpired()) {
                return new LoginAttempt(1, Instant.now());
            }
            return new LoginAttempt(v.count + 1, Instant.now());
        });

        return this.calculateLockoutSeconds(attempt.count);
    }

    /**
     * Records a failed login attempt for the account (username only).
     *
     * @param username
     *            the username
     */
    public void recordFailedAccountAttempt(final String username) {
        if (this.accountAttempts.size() >= MAX_CACHE_SIZE && !this.accountAttempts.containsKey(username)) {
            this.cleanupExpiredEntries();
        }

        final var now = this.clock.instant();
        this.accountAttempts.compute(username, (k, v) -> WindowedAttempt.after(v, ACCOUNT_POLICY, now));
    }

    /**
     * Counts a password-change attempt of a session, unless the session is locked out. Check and count happen in one
     * atomic step, so concurrent attempts cannot exceed the cap. Each session has its own bucket, apart from the login
     * throttles: at most 5 attempts per 15 minutes from the first, and once 5 are used the session is locked until 15
     * minutes after the last.
     *
     * @param sessionToken
     *            the random per-session token
     * @return true if the attempt may proceed, false if the session is locked out (nothing is counted then)
     */
    public boolean tryRecordPasswordChangeAttempt(final String sessionToken) {
        if (this.passwordChangeAttempts.size() >= MAX_CACHE_SIZE
                && !this.passwordChangeAttempts.containsKey(sessionToken)) {
            this.cleanupExpiredEntries();
        }
        final var now = this.clock.instant();
        final var allowed = new AtomicBoolean();
        this.passwordChangeAttempts.compute(sessionToken, (k, v) -> {
            if (v != null && !v.isExpired(PASSWORD_CHANGE_POLICY, now)
                    && v.count >= PASSWORD_CHANGE_POLICY.maxAttempts) {
                return v;
            }
            allowed.set(true);
            return WindowedAttempt.after(v, PASSWORD_CHANGE_POLICY, now);
        });
        return allowed.get();
    }

    /**
     * Clears the password-change attempts of a session, after it proved the current password.
     *
     * @param sessionToken
     *            the random per-session token
     */
    public void clearPasswordChangeAttempts(final String sessionToken) {
        this.passwordChangeAttempts.remove(sessionToken);
    }

    /**
     * Records a successful login, clearing any previous failed attempts.
     *
     * @param key
     *            the username or IP address
     */
    public void recordSuccessfulLogin(final String key) {
        this.attempts.remove(key);
    }

    /**
     * Records a successful login for an account, clearing previous failed account attempts.
     *
     * @param username
     *            the username
     */
    public void recordSuccessfulAccountLogin(final String username) {
        this.accountAttempts.remove(username);
    }

    /**
     * Checks whether the given key is currently locked out due to too many failed attempts.
     *
     * @param key
     *            the username or IP address
     * @return true if the key is locked out
     */
    public boolean isLockedOut(final String key) {
        final LoginAttempt attempt = this.attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired()) {
            this.attempts.remove(key, attempt);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    /**
     * Checks whether the account is currently locked out.
     *
     * @param username
     *            the username
     * @return true if locked out
     */
    public boolean isAccountLockedOut(final String username) {
        final WindowedAttempt attempt = this.accountAttempts.get(username);
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired(ACCOUNT_POLICY, this.clock.instant())) {
            this.accountAttempts.remove(username, attempt);
            return false;
        }
        return attempt.count >= ACCOUNT_POLICY.maxAttempts;
    }

    /**
     * Returns the remaining lockout duration in seconds for the given key.
     *
     * @param key
     *            the username or IP address
     * @return remaining lockout seconds, or 0 if not locked out
     */
    public long getRemainingLockoutSeconds(final String key) {
        final LoginAttempt attempt = this.attempts.get(key);
        if (attempt == null || attempt.isExpired() || attempt.count < MAX_ATTEMPTS) {
            return 0;
        }
        final long lockoutSeconds = this.calculateLockoutSeconds(attempt.count);
        final long elapsed = ChronoUnit.SECONDS.between(attempt.lastAttempt, Instant.now());
        return Math.max(0, lockoutSeconds - elapsed);
    }

    /**
     * Returns the remaining lockout duration in seconds for the account.
     *
     * @param username
     *            the username
     * @return remaining lockout seconds, or 0 if not locked out
     */
    public long getRemainingAccountLockoutSeconds(final String username) {
        final var now = this.clock.instant();
        final WindowedAttempt attempt = this.accountAttempts.get(username);
        if (attempt == null || attempt.isExpired(ACCOUNT_POLICY, now) || attempt.count < ACCOUNT_POLICY.maxAttempts) {
            return 0;
        }
        final long elapsed = ChronoUnit.SECONDS.between(attempt.lastAttempt, now);
        return Math.max(0, ACCOUNT_POLICY.lockSeconds - elapsed);
    }

    private long calculateLockoutSeconds(final int attemptCount) {
        if (attemptCount < MAX_ATTEMPTS) {
            return 0;
        }
        final long multiplier = 1L << (attemptCount - MAX_ATTEMPTS); // exponential: 1, 2, 4, 8...
        return Math.min(BASE_LOCKOUT_SECONDS * multiplier, MAX_LOCKOUT_SECONDS);
    }

    private static final class LoginAttempt {
        final int count;
        final Instant lastAttempt;

        LoginAttempt(final int count, final Instant lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }

        boolean isExpired() {
            final long lockoutSeconds = calculateLockoutSeconds(this.count);
            final long elapsed = ChronoUnit.SECONDS.between(this.lastAttempt, Instant.now());
            return elapsed > lockoutSeconds + BASE_LOCKOUT_SECONDS;
        }

        private static long calculateLockoutSeconds(final int attemptCount) {
            if (attemptCount < MAX_ATTEMPTS) {
                return 0;
            }
            final long multiplier = 1L << (attemptCount - MAX_ATTEMPTS);
            return Math.min(BASE_LOCKOUT_SECONDS * multiplier, MAX_LOCKOUT_SECONDS);
        }
    }

    /** Limits of a windowed throttle. */
    private record Policy(int maxAttempts, long lockSeconds, long windowSeconds) {
    }

    private static final class WindowedAttempt {
        final int count;
        final Instant firstAttempt;
        final Instant lastAttempt;

        WindowedAttempt(final int count, final Instant firstAttempt, final Instant lastAttempt) {
            this.count = count;
            this.firstAttempt = firstAttempt;
            this.lastAttempt = lastAttempt;
        }

        /** The state after one more attempt at {@code now}, restarting the window if the previous one is over. */
        static WindowedAttempt after(@Nullable final WindowedAttempt previous, final Policy policy, final Instant now) {
            if (previous == null || previous.isExpired(policy, now)
                    || ChronoUnit.SECONDS.between(previous.firstAttempt, now) > policy.windowSeconds) {
                return new WindowedAttempt(1, now, now);
            }
            return new WindowedAttempt(previous.count + 1, previous.firstAttempt, now);
        }

        boolean isExpired(final Policy policy, final Instant now) {
            if (this.count >= policy.maxAttempts) {
                return ChronoUnit.SECONDS.between(this.lastAttempt, now) > policy.lockSeconds;
            }
            return ChronoUnit.SECONDS.between(this.firstAttempt, now) > policy.windowSeconds;
        }
    }
}
