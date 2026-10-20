package de.vptr.aimathtutor.service.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import de.vptr.aimathtutor.util.ExecutorShutdownUtil;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory service for tracking and throttling login attempts. Implements per-username and per-IP exponential backoff
 * to mitigate brute-force attacks: the failure count of a key survives its lock and is forgotten only an hour after the
 * lock ends, so repeated lockouts escalate up to the one-hour cap. Uses bounded cache with automatic cleanup to prevent
 * DoS via unbounded growth.
 */
@ApplicationScoped
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_LOCKOUT_SECONDS = 30;
    private static final long MAX_LOCKOUT_SECONDS = 3600; // 1 hour
    // A key's failures are forgotten this long after its lock ends, so that the next lock is longer
    private static final long FORGET_AFTER_SECONDS = 3600;
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
        final var now = this.clock.instant();
        this.attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        this.accountAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired(ACCOUNT_POLICY, now));
        this.passwordChangeAttempts.entrySet()
                .removeIf(entry -> entry.getValue().isExpired(PASSWORD_CHANGE_POLICY, now));

        // If still over the limit, remove the oldest unlocked entries. A size cap must never lift a lock: a locked
        // bucket stops updating, so it would be the oldest, and a flood of fresh keys could wipe it. The locked
        // population stays bounded by bcrypt throughput times lock length, because a locked bucket records nothing.
        trimOldest(this.attempts, attempt -> !attempt.isLocked(now), attempt -> attempt.lastAttempt);
        trimOldest(this.accountAttempts, attempt -> attempt.count < ACCOUNT_POLICY.maxAttempts,
                attempt -> attempt.lastAttempt);
        trimOldest(this.passwordChangeAttempts, attempt -> attempt.count < PASSWORD_CHANGE_POLICY.maxAttempts,
                attempt -> attempt.lastAttempt);
    }

    private static <V> void trimOldest(final Map<String, V> map, final Predicate<V> evictable,
            final Function<V, Instant> lastAttempt) {
        if (map.size() <= MAX_CACHE_SIZE) {
            return;
        }
        final var candidates = map.entrySet().stream().filter(entry -> evictable.test(entry.getValue()))
                .sorted(Comparator.comparing(entry -> lastAttempt.apply(entry.getValue()))).toList();
        if (candidates.size() > MAX_CACHE_SIZE) {
            candidates.stream().limit(candidates.size() - MAX_CACHE_SIZE)
                    .forEach(entry -> map.remove(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Records a failed login attempt for the given key (username or IP). Returns the number of seconds the client
     * should wait before retrying. The count keeps growing while the key is locked and is forgotten only
     * {@value #FORGET_AFTER_SECONDS} seconds after the lock ends, so each lock lasts longer than the one before.
     * Enforces maximum cache size to prevent DoS attacks.
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

        final var now = this.clock.instant();
        final LoginAttempt attempt = this.attempts.compute(key, (k, v) -> {
            if (v == null || v.isExpired(now)) {
                return new LoginAttempt(1, now);
            }
            return new LoginAttempt(v.count + 1, now);
        });

        return lockoutSeconds(attempt.count);
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
        final var now = this.clock.instant();
        if (attempt.isExpired(now)) {
            this.attempts.remove(key, attempt);
            return false;
        }
        return attempt.isLocked(now);
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
        if (attempt == null) {
            return 0;
        }
        final var now = this.clock.instant();
        return attempt.isExpired(now) ? 0 : attempt.remainingLockoutSeconds(now);
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

    /** Lock length after {@code count} failures: none below the threshold, then doubling from 30 s up to the cap. */
    private static long lockoutSeconds(final int count) {
        return count < MAX_ATTEMPTS ? 0
                : Math.min(BASE_LOCKOUT_SECONDS * (1L << Math.min(count - MAX_ATTEMPTS, 31)), MAX_LOCKOUT_SECONDS);
    }

    private static final class LoginAttempt {
        final int count;
        final Instant lastAttempt;

        LoginAttempt(final int count, final Instant lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }

        long remainingLockoutSeconds(final Instant now) {
            return Math.max(0, lockoutSeconds(this.count) - ChronoUnit.SECONDS.between(this.lastAttempt, now));
        }

        boolean isLocked(final Instant now) {
            return this.remainingLockoutSeconds(now) > 0;
        }

        boolean isExpired(final Instant now) {
            return ChronoUnit.SECONDS.between(this.lastAttempt, now) > lockoutSeconds(this.count)
                    + FORGET_AFTER_SECONDS;
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
