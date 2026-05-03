package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Data Transfer Object for AI configuration.
 * Used for transferring AI configuration data between backend and frontend.
 */
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "DTO used for JSON mapping and UI binding; public fields are intentional")
public class AiConfigDto {

    /**
     * Enumeration of configuration value types.
     * Maps to string values stored in the database and used in UI components.
     */
    public enum ConfigType {
        STRING("STRING"),
        INTEGER("INTEGER"),
        DOUBLE("DOUBLE"),
        BOOLEAN("BOOLEAN"),
        TEXT("TEXT");

        private final String value;

        ConfigType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        /**
         * Converts a string value to the corresponding ConfigType enum.
         *
         * @param value the string value to convert
         * @return the matching ConfigType, or null if no match
         */
        public static ConfigType fromString(final String value) {
            if (value == null) {
                return null;
            }
            for (final ConfigType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Enumeration of configuration categories.
     * Maps to string values stored in the database and used in UI components.
     */
    public enum ConfigCategory {
        GENERAL("GENERAL"),
        GEMINI("GEMINI"),
        OPENAI("OPENAI"),
        OLLAMA("OLLAMA"),
        PROMPTS("PROMPTS");

        private final String value;

        ConfigCategory(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        /**
         * Converts a string value to the corresponding ConfigCategory enum.
         *
         * @param value the string value to convert
         * @return the matching ConfigCategory, or null if no match
         */
        public static ConfigCategory fromString(final String value) {
            if (value == null) {
                return null;
            }
            for (final ConfigCategory category : values()) {
                if (category.value.equalsIgnoreCase(value)) {
                    return category;
                }
            }
            return null;
        }
    }

    public Long id;

    @JsonProperty("config_key")
    public String configKey;

    @JsonProperty("config_value")
    public String configValue;

    @JsonProperty("config_type")
    public ConfigType configType;

    public ConfigCategory category;

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
    public AiConfigDto(final String configKey, final String configValue, final ConfigType configType,
            final ConfigCategory category) {
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
            final ConfigType configType, final ConfigCategory category, final String description,
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
