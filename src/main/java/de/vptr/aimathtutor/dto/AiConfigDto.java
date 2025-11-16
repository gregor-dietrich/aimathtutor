package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Data Transfer Object for AI configuration.
 * Used for transferring AI configuration data between backend and frontend.
 */
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "DTO used for JSON mapping and UI binding; public fields are intentional")
public class AiConfigDto {

    public Long id;

    @JsonProperty("config_key")
    public String configKey;

    @JsonProperty("config_value")
    public String configValue;

    @JsonProperty("config_type")
    public String configType;

    public String category;

    public String description;

    @JsonProperty("last_updated_at")
    public LocalDateTime lastUpdatedAt;

    @JsonProperty("last_updated_by")
    public String lastUpdatedBy; // Username for display

    /**
     * Default constructor for serialization.
     */
    public AiConfigDto() {
    }

    /**
     * Constructor with required fields.
     */
    public AiConfigDto(final String configKey, final String configValue, final String configType,
            final String category) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with all fields.
     */
    public AiConfigDto(final Long id, final String configKey, final String configValue,
            final String configType, final String category, final String description,
            final LocalDateTime lastUpdatedAt, final String lastUpdatedBy) {
        this.id = id;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.description = description;
        this.lastUpdatedAt = lastUpdatedAt;
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
