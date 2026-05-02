package de.vptr.aimathtutor.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;

/**
 * Utility for loading data asynchronously into Vaadin views with consistent
 * timeout, error handling, and UI access patterns.
 */
public final class AsyncDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncDataLoader.class);

    private AsyncDataLoader() {
        // Utility class
    }

    /**
     * Loads data asynchronously and updates the UI on completion.
     *
     * @param <T>          the type of data being loaded
     * @param dataSupplier supplier that fetches the data
     * @param component    Vaadin component used to access the UI thread
     * @param onSuccess    callback invoked with the loaded data on success
     * @param errorMessage user-facing message shown if loading fails
     */
    public static <T> void load(final Supplier<T> dataSupplier,
            final Component component,
            final Consumer<T> onSuccess,
            final String errorMessage) {
        load(dataSupplier, component, onSuccess, null, errorMessage);
    }

    /**
     * Loads data asynchronously and updates the UI on completion.
     *
     * @param <T>          the type of data being loaded
     * @param dataSupplier supplier that fetches the data
     * @param component    Vaadin component used to access the UI thread
     * @param onSuccess    callback invoked with the loaded data on success
     * @param onError      callback invoked on error (before the notification is shown)
     * @param errorMessage user-facing message shown if loading fails
     */
    public static <T> void load(final Supplier<T> dataSupplier,
            final Component component,
            final Consumer<T> onSuccess,
            final Runnable onError,
            final String errorMessage) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return dataSupplier.get();
            } catch (final Exception e) {
                LOG.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }).orTimeout(AppConstants.ADMIN_ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((data, throwable) -> {
                    component.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Async load failed: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError(errorMessage);
                            if (onError != null) {
                                onError.run();
                            }
                        } else {
                            onSuccess.accept(data);
                        }
                    }));
                });
    }
}
