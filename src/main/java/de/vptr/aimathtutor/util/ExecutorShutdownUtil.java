package de.vptr.aimathtutor.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility for gracefully shutting down executor services.
 */
public final class ExecutorShutdownUtil {

    private ExecutorShutdownUtil() {}

    /**
     * Shuts down the given executor service, waiting up to the specified timeout for tasks to complete before forcing
     * shutdown.
     *
     * @param executor
     *            the executor service to shut down
     * @param timeoutSeconds
     *            maximum time to wait for termination
     */
    public static void shutdownGracefully(final ExecutorService executor, final long timeoutSeconds) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
