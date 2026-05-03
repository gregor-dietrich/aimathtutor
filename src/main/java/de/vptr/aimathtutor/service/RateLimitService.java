package de.vptr.aimathtutor.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory per-user rate limiting service using a sliding window.
 * Tracks timestamps of user actions and enforces maximum calls per time window.
 * Periodically evicts stale user entries to prevent unbounded memory growth.
 */
@ApplicationScoped
public class RateLimitService {

    private static final int AI_CALLS_PER_MINUTE = 10;
    private static final long WINDOW_SECONDS = 60;
    private static final long CLEANUP_INTERVAL_SECONDS = 300; // Run cleanup every 5 minutes

    private final Map<String, CopyOnWriteArrayList<Instant>> userCallTimestamps = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * Creates a new RateLimitService and starts the cleanup executor.
     */
    public RateLimitService() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        // Schedule periodic cleanup of stale user entries
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupStaleEntries,
                CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * Removes user entries where all timestamps are older than the rate window.
     * This prevents unbounded memory growth from one-off user IDs.
     */
    private void cleanupStaleEntries() {
        final Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        this.userCallTimestamps.entrySet().removeIf(entry -> {
            final CopyOnWriteArrayList<Instant> timestamps = entry.getValue();
            if (timestamps == null || timestamps.isEmpty()) {
                return true;
            }
            // Check if all timestamps are stale
            return timestamps.stream().allMatch(ts -> ts.isBefore(cutoff));
        });
    }

    /**
     * Shuts down the cleanup executor when the service is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        if (this.cleanupExecutor != null) {
            this.cleanupExecutor.shutdown();
            try {
                if (!this.cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.cleanupExecutor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                this.cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Atomically checks if the user is allowed to make another AI tutor call,
     * and if so, records it. This prevents race conditions between check and
     * record.
     * Uses map.compute to ensure no orphaned list writes can occur.
     *
     * @param userId the user identifier
     * @return true if the call was allowed and recorded, false if rate-limited
     */
    public boolean tryConsume(final String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        final Instant now = Instant.now();
        final Instant windowStart = now.minusSeconds(WINDOW_SECONDS);

        final boolean[] allowed = { false };

        this.userCallTimestamps.compute(userId, (ignored, timestamps) -> {
            CopyOnWriteArrayList<Instant> list = timestamps;
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }

            // Prune expired timestamps
            list.removeIf(ts -> ts.isBefore(windowStart));

            // Check if allowed and add timestamp if so
            if (list.size() < AI_CALLS_PER_MINUTE) {
                list.add(now);
                allowed[0] = true;
            }

            // Return null to remove empty lists, otherwise return the list
            return list.isEmpty() ? null : list;
        });

        return allowed[0];
    }

    /**
     * Returns the number of seconds until the next call is allowed,
     * or 0 if calls are currently allowed.
     * Uses map.compute to avoid race conditions with list modifications.
     *
     * @param userId the user identifier
     * @return remaining cooldown in seconds
     */
    public long getRemainingCooldownSeconds(final String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }

        final long[] cooldown = { 0 };

        this.userCallTimestamps.computeIfPresent(userId, (ignored, timestamps) -> {
            final Instant now = Instant.now();
            final Instant windowStart = now.minusSeconds(WINDOW_SECONDS);

            // Prune expired timestamps
            timestamps.removeIf(ts -> ts.isBefore(windowStart));

            if (timestamps.isEmpty()) {
                cooldown[0] = 0;
                return null; // Remove empty list
            }

            if (timestamps.size() < AI_CALLS_PER_MINUTE) {
                cooldown[0] = 0;
            } else {
                final Instant oldest = timestamps.get(0);
                final long elapsed = Duration.between(oldest, now).getSeconds();
                cooldown[0] = Math.max(0, WINDOW_SECONDS - elapsed);
            }

            return timestamps;
        });

        return cooldown[0];
    }
}
