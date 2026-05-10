package de.vptr.aimathtutor.dto;

import jakarta.annotation.Nullable;

/**
 * Result of testing an AI provider connection.
 */
public class ProviderTestResultDto {
    public boolean success;
    @Nullable
    public String message;

    /**
     * Default constructor for serialization.
     */
    public ProviderTestResultDto() {
    }

    /**
     * Constructor with all fields.
     */
    public ProviderTestResultDto(final boolean success, final String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Creates a successful result.
     */
    public static ProviderTestResultDto ok(final String message) {
        return new ProviderTestResultDto(true, message);
    }

    /**
     * Creates a failed result.
     */
    public static ProviderTestResultDto fail(final String message) {
        return new ProviderTestResultDto(false, message);
    }
}
