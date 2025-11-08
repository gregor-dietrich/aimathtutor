package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AiConfigDto;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing AI configuration at runtime.
 * Provides methods to retrieve, validate, and update AI settings from the
 * database.
 * Supports caching to avoid frequent database hits.
 */
@ApplicationScoped
public class AiConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(AiConfigService.class);

    // Internal cache for configuration values to reduce database hits
    private final Map<String, String> configCache = new HashMap<>();

    @Inject
    private AiConfigRepository aiConfigRepository;

    @Inject
    private UserRepository userRepository;

    /**
     * Retrieves a configuration value as a String.
     * Falls back to defaultValue if not found.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    public String getConfigValue(final String key, final String defaultValue) {
        if (key == null) {
            return defaultValue;
        }

        // Check cache first
        if (this.configCache.containsKey(key)) {
            return this.configCache.get(key);
        }

        // Query database
        final Optional<AiConfigEntity> entity = this.aiConfigRepository.findByConfigKey(key);
        if (entity.isPresent()) {
            final String value = entity.get().configValue;
            this.configCache.put(key, value);
            return value;
        }

        return defaultValue;
    }

    /**
     * Retrieves a configuration value as an Integer.
     * Falls back to defaultValue if not found or if parsing fails.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found or parsing fails
     * @return the configuration value or default
     */
    public Integer getConfigValueAsInt(final String key, final Integer defaultValue) {
        final String value = this.getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            LOG.warn("Failed to parse integer config '{}' with value '{}': {}", key, value, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration value as a Double.
     * Falls back to defaultValue if not found or if parsing fails.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found or parsing fails
     * @return the configuration value or default
     */
    public Double getConfigValueAsDouble(final String key, final Double defaultValue) {
        final String value = this.getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException e) {
            LOG.warn("Failed to parse double config '{}' with value '{}': {}", key, value, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration value as a Boolean.
     * Falls back to defaultValue if not found.
     * Accepts "true", "false" (case-insensitive) and "1", "0".
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found
     * @return the configuration value or default
     */
    public Boolean getConfigValueAsBoolean(final String key, final Boolean defaultValue) {
        final String value = this.getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        final String lower = value.toLowerCase().trim();
        if ("true".equals(lower) || "1".equals(lower)) {
            return true;
        }
        if ("false".equals(lower) || "0".equals(lower)) {
            return false;
        }
        LOG.warn("Failed to parse boolean config '{}' with value '{}', using default", key, value);
        return defaultValue;
    }

    /**
     * Retrieves all configuration entries in a specific category as a key-value
     * map.
     * Useful for populating UI forms.
     *
     * @param category the category to retrieve
     * @return a map of all config keys and values in the category
     */
    public Map<String, String> getAllConfigsByCategory(final String category) {
        if (category == null) {
            return new HashMap<>();
        }
        final Map<String, String> result = new HashMap<>();
        final List<AiConfigEntity> entities = this.aiConfigRepository.findByCategory(category);
        for (final AiConfigEntity entity : entities) {
            result.put(entity.configKey, entity.configValue);
        }
        return result;
    }

    /**
     * Retrieves all configuration entries as DTOs.
     *
     * @return a list of all {@link AiConfigDto} objects
     */
    public List<AiConfigDto> getAllConfigs() {
        return this.aiConfigRepository.findAll().stream()
                .map(this::entityToDto)
                .toList();
    }

    /**
     * Retrieves all configuration entries in a category as DTOs.
     *
     * @param category the category to retrieve
     * @return a list of {@link AiConfigDto} objects in the category
     */
    public List<AiConfigDto> getConfigsByCategory(final String category) {
        return this.aiConfigRepository.findByCategory(category).stream()
                .map(this::entityToDto)
                .toList();
    }

    /**
     * Updates a single configuration value.
     * Validates the input before persisting.
     *
     * @param configKey   the configuration key to update
     * @param configValue the new value
     * @param userId      the ID of the user making the update (for audit trail)
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if user is not an admin
     */
    @Transactional
    public void updateConfig(final String configKey, final String configValue, final Long userId) {
        // Validate the input first (tests expect validation errors before auth errors)
        if (configKey == null || configValue == null) {
            throw new IllegalArgumentException("Configuration key and value cannot be null");
        }

        this.validateConfigValue(configKey, configValue);

        // Verify admin permission
        final UserEntity user = this.userRepository.findById(userId);
        if (user == null || user.rank == null || user.rank.id != 1L) {
            throw new IllegalStateException("Only admins can update configuration");
        }

        // Find existing or create new
        final Optional<AiConfigEntity> existing = this.aiConfigRepository.findByConfigKey(configKey);
        final AiConfigEntity entity = existing.orElseGet(() -> {
            final AiConfigEntity newEntity = new AiConfigEntity();
            newEntity.configKey = configKey;
            return newEntity;
        });

        entity.configValue = configValue;
        entity.lastUpdatedAt = LocalDateTime.now();
        entity.lastUpdatedBy = user;

        if (existing.isEmpty()) {
            this.aiConfigRepository.persist(entity);
        } else {
            this.aiConfigRepository.update(entity);
        }

        // Invalidate cache
        this.configCache.remove(configKey);

        LOG.info("Configuration updated: key='{}', updatedBy='{}'", configKey, user.username);
    }

    /**
     * Updates multiple configuration values at once.
     * All updates are validated before any are persisted.
     *
     * @param updates a list of {@link AiConfigUpdateDto} objects containing
     *                key-value pairs
     * @param userId  the ID of the user making the updates
     * @throws IllegalArgumentException if any validation fails
     * @throws IllegalStateException    if user is not an admin
     */
    @Transactional
    public void updateMultipleConfigs(final List<AiConfigUpdateDto> updates, final Long userId) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        // Verify admin permission once
        final UserEntity user = this.userRepository.findById(userId);
        if (user == null || user.rank == null || user.rank.id != 1L) {
            throw new IllegalStateException("Only admins can update configuration");
        }

        // Validate all updates first
        for (final AiConfigUpdateDto update : updates) {
            if (update.configKey == null || update.configValue == null) {
                throw new IllegalArgumentException("Configuration key and value cannot be null");
            }
            this.validateConfigValue(update.configKey, update.configValue);
        }

        // Persist all updates
        for (final AiConfigUpdateDto update : updates) {
            final Optional<AiConfigEntity> existing = this.aiConfigRepository.findByConfigKey(update.configKey);
            final AiConfigEntity entity = existing.orElseGet(() -> {
                final AiConfigEntity newEntity = new AiConfigEntity();
                newEntity.configKey = update.configKey;
                return newEntity;
            });

            entity.configValue = update.configValue;
            entity.lastUpdatedAt = LocalDateTime.now();
            entity.lastUpdatedBy = user;

            if (existing.isEmpty()) {
                this.aiConfigRepository.persist(entity);
            } else {
                this.aiConfigRepository.update(entity);
            }

            // Invalidate cache
            this.configCache.remove(update.configKey);
        }

        LOG.info("Multiple configurations updated: count={}, updatedBy='{}'", updates.size(), user.username);
    }

    /**
     * Validates a configuration value based on its key and expected type.
     * Throws IllegalArgumentException if validation fails.
     *
     * @param configKey   the configuration key
     * @param configValue the value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateConfigValue(final String configKey, final String configValue) {
        if (configValue == null || configValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration value cannot be empty: " + configKey);
        }

        // Type-specific validation based on config key
        if (configKey.contains("temperature")) {
            try {
                final double temp = Double.parseDouble(configValue);
                if (temp < 0.0 || temp > 2.0) {
                    throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0, got: " + temp);
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Temperature must be a valid decimal number, got: " + configValue);
            }
        }

        if (configKey.contains("max-tokens")) {
            try {
                final int tokens = Integer.parseInt(configValue);
                if (tokens < 1 || tokens > 8192) {
                    throw new IllegalArgumentException("Max tokens must be between 1 and 8192, got: " + tokens);
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Max tokens must be a valid integer, got: " + configValue);
            }
        }

        if (configKey.contains("timeout-seconds")) {
            try {
                final int seconds = Integer.parseInt(configValue);
                if (seconds < 1 || seconds > 300) {
                    throw new IllegalArgumentException("Timeout must be between 1 and 300 seconds, got: " + seconds);
                }
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Timeout must be a valid integer, got: " + configValue);
            }
        }

        // URL validation for API endpoints
        if (configKey.contains("api.base-url") || configKey.contains("api.url")) {
            if (!configValue.startsWith("http://") && !configValue.startsWith("https://")) {
                throw new IllegalArgumentException("API URL must start with http:// or https://, got: " + configValue);
            }
        }

        // Boolean validation
        if (configKey.contains("enabled")) {
            final String lower = configValue.toLowerCase().trim();
            if (!("true".equals(lower) || "false".equals(lower) || "1".equals(lower) || "0".equals(lower))) {
                throw new IllegalArgumentException(
                        "Boolean value must be 'true', 'false', '1', or '0', got: " + configValue);
            }
        }

        // Prompt length validation
        if (configKey.contains("prompt")) {
            if (configValue.length() < 10 || configValue.length() > 5000) {
                throw new IllegalArgumentException(
                        "Prompt length must be between 10 and 5000 characters, got: " + configValue.length());
            }
        }
    }

    /**
     * Clears the configuration cache.
     * Called when configurations are updated or when cache needs to be refreshed.
     */
    public void clearCache() {
        this.configCache.clear();
        LOG.info("Configuration cache cleared");
    }

    /**
     * Converts an AiConfigEntity to an AiConfigDto.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    private AiConfigDto entityToDto(final AiConfigEntity entity) {
        final String lastUpdatedByName = entity.lastUpdatedBy != null ? entity.lastUpdatedBy.username : "system";
        return new AiConfigDto(entity.id, entity.configKey, entity.configValue, entity.configType,
                entity.category, entity.description, entity.lastUpdatedAt, lastUpdatedByName);
    }
}
