package de.vptr.aimathtutor.dto;

/**
 * Result of testing an AI provider connection.
 */
public class AiProviderTestResultDto {

    public boolean success;
    public String message;

    /**
     * Default constructor for serialization.
     */
    public AiProviderTestResultDto() {
    }

    /**
     * Constructor with all fields.
     */
    public AiProviderTestResultDto(final boolean success, final String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Creates a successful result.
     */
    public static AiProviderTestResultDto ok(final String message) {
        return new AiProviderTestResultDto(true, message);
    }

    /**
     * Creates a failed result.
     */
    public static AiProviderTestResultDto fail(final String message) {
        return new AiProviderTestResultDto(false, message);
    }
}
