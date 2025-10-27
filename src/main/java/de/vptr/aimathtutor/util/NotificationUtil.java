package de.vptr.aimathtutor.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * TODO: Class documentation.
 */
public final class NotificationUtil {

    private NotificationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Shows a success notification
     * 
     * @param message The message to display
     */
    public static void showSuccess(final String message) {
        showNotification(message, NotificationVariant.LUMO_SUCCESS, 3000);
    }

    /**
     * Shows an error notification
     * 
     * @param message The message to display
     */
    public static void showError(final String message) {
        showNotification(message, NotificationVariant.LUMO_ERROR, 5000);
    }

    /**
     * Shows a warning notification
     * 
     * @param message The message to display
     */
    public static void showWarning(final String message) {
        showNotification(message, NotificationVariant.LUMO_CONTRAST, 4000);
    }

    /**
     * Shows an info notification
     * 
     * @param message The message to display
     */
    public static void showInfo(final String message) {
        showNotification(message, NotificationVariant.LUMO_PRIMARY, 3000);
    }

    /**
     * Shows a custom notification with specified duration
     * 
     * @param message  The message to display
     * @param variant  The notification variant
     * @param duration Duration in milliseconds
     */
    public static void showNotification(final String message, final NotificationVariant variant, final int duration) {
        final var notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setText(message);
        notification.setDuration(duration);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.open();
    }

    /**
     * Shows a notification with custom position
     * 
     * @param message  The message to display
     * @param variant  The notification variant
     * @param duration Duration in milliseconds
     * @param position The position of the notification
     */
    public static void showNotification(final String message, final NotificationVariant variant,
            final int duration, final Notification.Position position) {
        final var notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setText(message);
        notification.setDuration(duration);
        notification.setPosition(position);
        notification.open();
    }
}
