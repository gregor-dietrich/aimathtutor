package de.vptr.aimathtutor.dto;

/**
 * Result object returned by authentication operations. Encapsulates a status
 * and an optional user-facing message.
 */
public class AuthResultDto {

    /**
     * Status codes for authentication results.
     */
    public enum Status {
        SUCCESS,
        INVALID_CREDENTIALS,
        BACKEND_UNAVAILABLE,
        INVALID_INPUT
    }

    private final Status status;
    private final String message;

    private AuthResultDto(final Status status, final String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Convenience factory for a successful authentication result.
     */
    public static AuthResultDto success() {
        return new AuthResultDto(Status.SUCCESS, "Authentication successful");
    }

    /**
     * Convenience factory for invalid credential result.
     */
    public static AuthResultDto invalidCredentials() {
        return new AuthResultDto(Status.INVALID_CREDENTIALS, "Invalid username or password");
    }

    /**
     * Convenience factory for backend-unavailable result with details.
     */
    public static AuthResultDto backendUnavailable(final String details) {
        return new AuthResultDto(Status.BACKEND_UNAVAILABLE, "Backend service unavailable: " + details);
    }

    /**
     * Convenience factory for invalid input result.
     */
    public static AuthResultDto invalidInput() {
        return new AuthResultDto(Status.INVALID_INPUT, "Username and password are required");
    }

    /**
     * Return the authentication status code.
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * Return the user-facing message associated with this result.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Convenience boolean indicating whether authentication succeeded.
     *
     * @return true if status == SUCCESS
     */
    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }
}
