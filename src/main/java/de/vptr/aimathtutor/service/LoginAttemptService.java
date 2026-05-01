package de.vptr.aimathtutor.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory service for tracking and throttling login attempts.
 * Implements per-username and per-IP exponential backoff to mitigate
 * brute-force attacks.
 */
@ApplicationScoped
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_LOCKOUT_SECONDS = 30;
    private static final long MAX_LOCKOUT_SECONDS = 3600; // 1 hour

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    /**
     * Records a failed login attempt for the given key (username or IP).
     * Returns the number of seconds the client should wait before retrying.
     *
     * @param key the username or IP address
     * @return lockout duration in seconds
     */
    public long recordFailedAttempt(final String key) {
        final LoginAttempt attempt = this.attempts.compute(key, (k, v) -> {
            if (v == null || v.isExpired()) {
                return new LoginAttempt(1, Instant.now());
            }
            return new LoginAttempt(v.count + 1, Instant.now());
        });

        return this.calculateLockoutSeconds(attempt.count);
    }

    /**
     * Records a successful login, clearing any previous failed attempts.
     *
     * @param key the username or IP address
     */
    public void recordSuccessfulLogin(final String key) {
        this.attempts.remove(key);
    }

    /**
     * Checks whether the given key is currently locked out due to too many
     * failed attempts.
     *
     * @param key the username or IP address
     * @return true if the key is locked out
     */
    public boolean isLockedOut(final String key) {
        final LoginAttempt attempt = this.attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired()) {
            this.attempts.remove(key);
            return false;
        }
        if (attempt.count < MAX_ATTEMPTS) {
            return false;
        }
        return true;
    }

    /**
     * Returns the remaining lockout duration in seconds for the given key.
     *
     * @param key the username or IP address
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
}
