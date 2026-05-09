package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.annotation.Nullable;

/**
 * Data Transfer Object for AI configuration. Used for transferring AI configuration data between backend and frontend.
 */
public class AiConfigDto {

    /**
     * Enumeration of configuration value types. Maps to string values stored in the database and used in UI components.
     */
    public enum ConfigType {
        STRING("STRING"), INTEGER("INTEGER"), DOUBLE("DOUBLE"), BOOLEAN("BOOLEAN"), TEXT("TEXT");

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
         * @param value
         *            the string value to convert
         * @return the matching ConfigType, or null if no match
         */
        @Nullable
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
     * Enumeration of configuration categories. Maps to string values stored in the database and used in UI components.
     */
    public enum ConfigCategory {
        GENERAL("GENERAL"), GEMINI("GEMINI"), OPENAI("OPENAI"), OLLAMA("OLLAMA"), PROMPTS("PROMPTS");

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
         * @param value
         *            the string value to convert
         * @return the matching ConfigCategory, or null if no match
         */
        @Nullable
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

    @Nullable
    public String publicId;

    @JsonProperty("config_key")
    @Nullable
    public String configKey;

    @JsonProperty("config_value")
    @Nullable
    public String configValue;

    @JsonProperty("config_type")
    @Nullable
    public ConfigType configType;

    @Nullable
    public ConfigCategory category;

    @Nullable
    public String description;

    @JsonProperty("last_updated_at")
    @Nullable
    public LocalDateTime lastUpdatedAt;

    @JsonProperty("last_updated_by")
    @Nullable
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
        this.lastUpdatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    /**
     * Constructor with all fields.
     */
    public AiConfigDto(@Nullable final String publicId, @Nullable final String configKey,
            @Nullable final String configValue, @Nullable final ConfigType configType,
            @Nullable final ConfigCategory category, @Nullable final String description,
            @Nullable final LocalDateTime lastUpdatedAt, @Nullable final String lastUpdatedBy) {
        this.publicId = publicId;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.description = description;
        this.lastUpdatedAt = lastUpdatedAt;
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
