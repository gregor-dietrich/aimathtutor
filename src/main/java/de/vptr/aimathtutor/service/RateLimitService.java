package de.vptr.aimathtutor.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory per-user rate limiting service using a sliding window.
 * Tracks timestamps of user actions and enforces maximum calls per time window.
 */
@ApplicationScoped
public class RateLimitService {

    private static final int AI_CALLS_PER_MINUTE = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, CopyOnWriteArrayList<Instant>> userCallTimestamps = new ConcurrentHashMap<>();

    /**
     * Checks if the user is allowed to make another AI tutor call.
     * Evicts expired entries automatically.
     *
     * @param userId the user identifier
     * @return true if the call is allowed
     */
    public boolean isAllowed(final String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        final Instant now = Instant.now();
        final Instant windowStart = now.minusSeconds(WINDOW_SECONDS);

        final CopyOnWriteArrayList<Instant> timestamps = this.userCallTimestamps.computeIfAbsent(userId,
                k -> new CopyOnWriteArrayList<>());

        // Remove expired timestamps
        timestamps.removeIf(ts -> ts.isBefore(windowStart));

        return timestamps.size() < AI_CALLS_PER_MINUTE;
    }

    /**
     * Records a successful AI tutor call for the user.
     *
     * @param userId the user identifier
     */
    public void recordCall(final String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        this.userCallTimestamps.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(Instant.now());
    }

    /**
     * Returns the number of seconds until the next call is allowed,
     * or 0 if calls are currently allowed.
     *
     * @param userId the user identifier
     * @return remaining cooldown in seconds
     */
    public long getRemainingCooldownSeconds(final String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }

        final CopyOnWriteArrayList<Instant> timestamps = this.userCallTimestamps.get(userId);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        final Instant windowStart = Instant.now().minusSeconds(WINDOW_SECONDS);
        timestamps.removeIf(ts -> ts.isBefore(windowStart));

        if (timestamps.size() < AI_CALLS_PER_MINUTE) {
            return 0;
        }

        final Instant oldest = timestamps.get(0);
        final long elapsed = java.time.Duration.between(oldest, Instant.now()).getSeconds();
        return Math.max(0, WINDOW_SECONDS - elapsed);
    }
}
