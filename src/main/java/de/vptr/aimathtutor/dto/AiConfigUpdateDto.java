package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Data Transfer Object for updating AI configuration.
 * Minimal DTO used when submitting configuration updates from the frontend.
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "DTO used for JSON mapping and UI binding; public fields are intentional")
public class AiConfigUpdateDto {

    @JsonProperty("config_key")
    public String configKey;

    @JsonProperty("config_value")
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
