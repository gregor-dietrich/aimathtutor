package de.vptr.aimathtutor.dto;

/**
 * TODO: Class documentation.
 */
public class AuthResultDto {

    /**
     * TODO: Class documentation.
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
     * TODO: Document success().
     */
    public static AuthResultDto success() {
        return new AuthResultDto(Status.SUCCESS, "Authentication successful");
    }

    /**
     * TODO: Document invalidCredentials().
     */
    public static AuthResultDto invalidCredentials() {
        return new AuthResultDto(Status.INVALID_CREDENTIALS, "Invalid username or password");
    }

    /**
     * TODO: Document backendUnavailable().
     */
    public static AuthResultDto backendUnavailable(final String details) {
        return new AuthResultDto(Status.BACKEND_UNAVAILABLE, "Backend service unavailable: " + details);
    }

    /**
     * TODO: Document invalidInput().
     */
    public static AuthResultDto invalidInput() {
        return new AuthResultDto(Status.INVALID_INPUT, "Username and password are required");
    }

    /**
     * TODO: Document getStatus().
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * TODO: Document getMessage().
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * TODO: Document isSuccess().
     */
    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }
}
