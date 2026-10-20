package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.UI;

import de.vptr.aimathtutor.view.LoginView;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

/**
 * Shows why a service rejected a write, such as a save or delete. Services reject invalid input with
 * {@link ValidationException}, which shares its simple name with the binder's exception that the views import, so the
 * views catch it through this helper. The message mapping is shared with {@link AsyncDataLoader}, which shows the same
 * text for a rejection thrown inside an asynchronous load.
 */
public final class ServiceRejectionUtil {

    private static final Logger LOG = Logger.getLogger(ServiceRejectionUtil.class);

    private static final String INVALID_INPUT = "Invalid input";

    private ServiceRejectionUtil() {
    }

    /**
     * Runs a service call and shows the message of a rejection it throws (see {@link #isRejection}). The rejection is
     * expected user input, so it is logged at DEBUG only, and without the message, which can contain user data. A
     * rejection that the session ended also returns to the login page. Any other {@link ValidationException}
     * propagates.
     *
     * @param serviceCall
     *            the call that writes, such as saving the form's data or deleting an item
     * @return true if the call completed, false if the service rejected it
     */
    public static boolean runOrShowRejection(final Runnable serviceCall) {
        try {
            serviceCall.run();
            return true;
        } catch (final ValidationException e) {
            if (!isRejection(e)) {
                throw e;
            }
            showRejection(UI.getCurrent(), rejectionMessage(e));
            return false;
        }
    }

    /**
     * Tells a service rejection, a plain {@link ValidationException} or a {@link ConstraintViolationException}, from
     * the other subclasses, which report a broken constraint or validator setup: a defect to log, not a message to
     * show.
     *
     * @param e
     *            the exception thrown by a service
     * @return true if the exception rejects the input
     */
    public static boolean isRejection(final ValidationException e) {
        return e.getClass() == ValidationException.class || e instanceof ConstraintViolationException;
    }

    /**
     * Maps a service rejection to the text shown to the user: the violations of a {@link ConstraintViolationException}
     * joined with "; ", otherwise the exception's message. Falls back to "Invalid input" when there is nothing to show.
     *
     * @param e
     *            the rejection thrown by a service
     * @return the user-facing message
     */
    public static String rejectionMessage(final ValidationException e) {
        if (e instanceof final ConstraintViolationException cve) {
            return cve.getConstraintViolations().stream().map(ServiceRejectionUtil::describe)
                    .reduce((a, b) -> a + "; " + b).orElse(INVALID_INPUT);
        }
        return e.getMessage() != null ? e.getMessage() : INVALID_INPUT;
    }

    private static String describe(final ConstraintViolation<?> violation) {
        String fieldName = null;
        for (final var node : violation.getPropertyPath()) {
            fieldName = node.getName();
        }
        return (fieldName != null ? fieldName : "field") + ": " + violation.getMessage();
    }

    /**
     * Shows a rejection's message and, when it says the session ended, reloads the UI onto the login page. Logs at
     * DEBUG without the message, which can contain user data.
     *
     * @param ui
     *            the UI to redirect, or null if there is none (the message is still shown)
     * @param message
     *            the user-facing message
     */
    static void showRejection(@Nullable final UI ui, final String message) {
        LOG.debug("Service rejected a write");
        NotificationUtil.showError(message);
        if (ui != null && AppConstants.SESSION_ENDED_MESSAGE.equals(message)) {
            LoginView.reloadToLogin(ui);
        }
    }
}
