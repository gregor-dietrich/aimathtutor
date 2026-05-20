package de.vptr.aimathtutor.service.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.vptr.aimathtutor.util.ExecutorShutdownUtil;
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

    // Account-wide limits
    private static final int ACCOUNT_MAX_ATTEMPTS = 25;
    private static final long ACCOUNT_SOFT_LOCK_SECONDS = 600; // 10 minutes
    private static final long ACCOUNT_WINDOW_SECONDS = 900; // 15 minutes

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, AccountLoginAttempt> accountAttempts = new ConcurrentHashMap<>();
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
        this.accountAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // If still over limit after cleanup, remove oldest entries
        if (this.attempts.size() > MAX_CACHE_SIZE) {
            this.attempts.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().lastAttempt.compareTo(e2.getValue().lastAttempt))
                    .limit(this.attempts.size() - MAX_CACHE_SIZE)
                    .forEach(entry -> this.attempts.remove(entry.getKey(), entry.getValue()));
        }
        if (this.accountAttempts.size() > MAX_CACHE_SIZE) {
            this.accountAttempts.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().lastAttempt.compareTo(e2.getValue().lastAttempt))
                    .limit(this.accountAttempts.size() - MAX_CACHE_SIZE)
                    .forEach(entry -> this.accountAttempts.remove(entry.getKey(), entry.getValue()));
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

        this.accountAttempts.compute(username, (k, v) -> {
            if (v == null || v.isExpired()) {
                return new AccountLoginAttempt(1, Instant.now(), Instant.now());
            }
            // If the window has passed since the *first* attempt in this window, reset
            if (ChronoUnit.SECONDS.between(v.firstAttempt, Instant.now()) > ACCOUNT_WINDOW_SECONDS) {
                return new AccountLoginAttempt(1, Instant.now(), Instant.now());
            }
            return new AccountLoginAttempt(v.count + 1, v.firstAttempt, Instant.now());
        });
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
        final AccountLoginAttempt attempt = this.accountAttempts.get(username);
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired()) {
            this.accountAttempts.remove(username, attempt);
            return false;
        }
        return attempt.count >= ACCOUNT_MAX_ATTEMPTS;
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
        final AccountLoginAttempt attempt = this.accountAttempts.get(username);
        if (attempt == null || attempt.isExpired() || attempt.count < ACCOUNT_MAX_ATTEMPTS) {
            return 0;
        }
        final long elapsed = ChronoUnit.SECONDS.between(attempt.lastAttempt, Instant.now());
        return Math.max(0, ACCOUNT_SOFT_LOCK_SECONDS - elapsed);
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

    private static final class AccountLoginAttempt {
        final int count;
        final Instant firstAttempt;
        final Instant lastAttempt;

        AccountLoginAttempt(final int count, final Instant firstAttempt, final Instant lastAttempt) {
            this.count = count;
            this.firstAttempt = firstAttempt;
            this.lastAttempt = lastAttempt;
        }

        boolean isExpired() {
            if (this.count >= ACCOUNT_MAX_ATTEMPTS) {
                final long elapsed = ChronoUnit.SECONDS.between(this.lastAttempt, Instant.now());
                return elapsed > ACCOUNT_SOFT_LOCK_SECONDS;
            } else {
                final long elapsed = ChronoUnit.SECONDS.between(this.firstAttempt, Instant.now());
                return elapsed > ACCOUNT_WINDOW_SECONDS;
            }
        }
    }
}
