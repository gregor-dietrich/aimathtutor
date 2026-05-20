package de.vptr.aimathtutor.util;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.Component;

import jakarta.annotation.Nullable;

/**
 * Utility for loading data asynchronously into Vaadin views with consistent timeout, error handling, and UI access
 * patterns.
 */
public final class AsyncDataLoader {

    private static final Logger LOG = Logger.getLogger(AsyncDataLoader.class);

    private AsyncDataLoader() {
        // Utility class
    }

    /**
     * Loads data asynchronously and updates the UI on completion. Uses {@link AppConstants#ADMIN_ASYNC_TIMEOUT_SECONDS}
     * as the timeout. Prefer the {@link #load(Supplier, Component, Consumer, Runnable, Duration, String)} overload for
     * dashboard panels that should fail fast.
     */
    public static <T> void load(final Supplier<T> dataSupplier, final Component component, final Consumer<T> onSuccess,
            final String errorMessage) {
        load(dataSupplier, component, onSuccess, null, errorMessage);
    }

    /**
     * Loads data asynchronously and updates the UI on completion. Uses {@link AppConstants#ADMIN_ASYNC_TIMEOUT_SECONDS}
     * as the timeout.
     */
    public static <T> void load(final Supplier<T> dataSupplier, final Component component, final Consumer<T> onSuccess,
            @Nullable final Runnable onError, final String errorMessage) {
        load(dataSupplier, component, onSuccess, onError,
                Duration.ofSeconds(AppConstants.ADMIN_ASYNC_TIMEOUT_SECONDS), errorMessage);
    }

    /**
     * Loads data asynchronously with a caller-specified timeout. Use a short timeout (e.g. 5 s) for fast dashboard tile
     * queries that should not hold the whole panel hostage if the DB is slow; use a longer timeout for heavy reports
     * or batch summaries.
     *
     * @param <T>
     *            the type of data being loaded
     * @param dataSupplier
     *            supplier that fetches the data
     * @param component
     *            Vaadin component used to access the UI thread
     * @param onSuccess
     *            callback invoked with the loaded data on success
     * @param onError
     *            callback invoked on error (before the notification is shown)
     * @param timeout
     *            maximum time to wait before aborting and notifying the user; must be positive
     * @param errorMessage
     *            user-facing message shown if loading fails
     */
    public static <T> void load(final Supplier<T> dataSupplier, final Component component, final Consumer<T> onSuccess,
            @Nullable final Runnable onError, final Duration timeout, final String errorMessage) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be a positive duration");
        }
        final var _ = CompletableFuture.supplyAsync(() -> {
            try {
                return dataSupplier.get();
            } catch (final RuntimeException e) {
                LOG.error(errorMessage, e);
                throw e;
            }
        }).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).whenComplete((data, throwable) -> {
            component.getUI().ifPresent(ui -> {
                final var _ = ui.access(() -> {
                    if (throwable == null) {
                        onSuccess.accept(data);
                        return;
                    }
                    LOG.errorf(throwable, "Async load failed: %s", throwable.getMessage());
                    NotificationUtil.showError(errorMessage);
                    if (onError == null) {
                        return;
                    }
                    onError.run();
                });
            });
        });
    }
}
