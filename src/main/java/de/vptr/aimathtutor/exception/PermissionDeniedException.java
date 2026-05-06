package de.vptr.aimathtutor.exception;

/**
 * Exception thrown when an authenticated user attempts an action
 * for which they lack the required permission.
 */
public class PermissionDeniedException extends RuntimeException {

    /**
     * Constructs a new permission denied exception with the specified detail message.
     *
     * @param message the detail message describing the missing permission
     */
    public PermissionDeniedException(final String message) {
        super(message);
    }
}
