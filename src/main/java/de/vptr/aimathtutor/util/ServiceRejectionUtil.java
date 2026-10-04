package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

/**
 * Shows why a service rejected a save. Services reject invalid input with {@link ValidationException}, which shares its
 * simple name with the binder's exception that the views import, so the views catch it through this helper.
 */
public final class ServiceRejectionUtil {

    private static final Logger LOG = Logger.getLogger(ServiceRejectionUtil.class);

    private ServiceRejectionUtil() {
    }

    /**
     * Runs a service call and shows the message of a {@link ValidationException} it throws. The rejection is expected
     * user input, so it is logged at DEBUG only, and without the message, which can contain user data.
     *
     * @param serviceCall
     *            the call that saves the form's data
     * @return true if the call completed, false if the service rejected it
     */
    public static boolean runOrShowRejection(final Runnable serviceCall) {
        try {
            serviceCall.run();
            return true;
        } catch (final ConstraintViolationException e) {
            return showRejection(e.getConstraintViolations().stream().map(ServiceRejectionUtil::describe)
                    .reduce((a, b) -> a + "; " + b).orElse("Invalid input"));
        } catch (final ValidationException e) {
            return showRejection(e.getMessage() != null ? e.getMessage() : "Invalid input");
        }
    }

    private static String describe(final ConstraintViolation<?> violation) {
        String fieldName = null;
        for (final var node : violation.getPropertyPath()) {
            fieldName = node.getName();
        }
        return (fieldName != null ? fieldName : "field") + ": " + violation.getMessage();
    }

    private static boolean showRejection(final String message) {
        LOG.debug("Service rejected a save");
        NotificationUtil.showError(message);
        return false;
    }
}
