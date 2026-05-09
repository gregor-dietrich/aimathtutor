package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/**
 * Data Transfer Object for updating AI configuration. Minimal DTO used when submitting configuration updates from the
 * frontend.
 */
public class AiConfigUpdateDto {
    @JsonProperty("config_key")
    @Nullable
    public String configKey;

    @JsonProperty("config_value")
    @Nullable
    public String configValue;

    /**
     * Default constructor for deserialization.
     */
    public AiConfigUpdateDto() {
    }

    /**
     * Constructor with key and value.
     */
    public AiConfigUpdateDto(final String configKey, final String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }
}
