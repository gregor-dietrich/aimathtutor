package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.AiConfigDto;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
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

    // Internal cache for configuration values to reduce database hits.
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

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
        // Only require configKey to be non-null. Values can be null or empty
        if (configKey == null) {
            throw new IllegalArgumentException("Configuration key cannot be null");
        }

        this.validateConfigValue(configKey, configValue);

        // Verify permission - user must have exercise or lesson management permissions
        final var user = this.userRepository.findById(userId);
        if (user == null || user.rank == null) {
            throw new IllegalStateException("User not found or has no rank assigned");
        }

        final var userRank = new UserRankViewDto(user.rank);
        final var hasPermission = userRank.hasAnyExercisePermission() || userRank.hasAnyLessonPermission();
        if (!hasPermission) {
            throw new IllegalStateException(
                    "Only users with exercise or lesson management permissions can update configuration");
        }

        // Find existing or create new
        final var existing = this.aiConfigRepository.findByConfigKey(configKey);
        final var entity = existing.orElseGet(() -> {
            final var newEntity = new AiConfigEntity();
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

        // Verify permission once
        final UserEntity user = this.userRepository.findById(userId);
        if (user == null || user.rank == null) {
            throw new IllegalStateException("User not found or has no rank assigned");
        }

        final var userRank = new UserRankViewDto(user.rank);
        final var hasPermission = userRank.hasAnyExercisePermission() || userRank.hasAnyLessonPermission();
        if (!hasPermission) {
            throw new IllegalStateException(
                    "Only users with exercise or lesson management permissions can update configuration");
        }

        // Validate all updates first
        for (final AiConfigUpdateDto update : updates) {
            if (update.configKey == null) {
                throw new IllegalArgumentException("Configuration key cannot be null");
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
     * Validates a configuration value based on its type and optionality.
     * Type validation is determined by the entity's configType field.
     * Optionality is determined by the entity's isOptional flag.
     *
     * @param configKey   the configuration key
     * @param configValue the value to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateConfigValue(final String configKey, final String configValue) {
        // Fetch the entity to get its declared type and optionality
        final var existingEntity = this.aiConfigRepository.findByConfigKey(configKey);

        // Check if value is empty
        final var isEmpty = configValue == null || configValue.isBlank();

        if (isEmpty) {
            // If empty, check if the config allows empty values
            if (existingEntity.isPresent() && !existingEntity.get().isOptional) {
                throw new IllegalArgumentException("Configuration '" + configKey + "' does not allow empty values");
            }
            // If optional or doesn't exist yet, empty is allowed
            return;
        }

        // Value is not empty, proceed with type validation
        if (existingEntity.isEmpty()) {
            // New config - no type constraints yet
            return;
        }

        final var configType = existingEntity.get().configType;
        if (configType == null) {
            // No type constraint defined
            return;
        }

        // Type-based validation
        switch (configType.toUpperCase()) {
            case "INTEGER" -> {
                try {
                    Integer.parseInt(configValue);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Value must be a valid integer for key '" + configKey + "', got: " + configValue);
                }
            }
            case "DOUBLE" -> {
                try {
                    Double.parseDouble(configValue);
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Value must be a valid decimal for key '" + configKey + "', got: " + configValue);
                }
            }
            case "BOOLEAN" -> {
                @SuppressWarnings("null") // false positive
                final var lower = configValue.toLowerCase().trim();
                if (!("true".equals(lower) || "false".equals(lower) || "1".equals(lower) || "0".equals(lower))) {
                    throw new IllegalArgumentException(
                            "Value must be boolean (true/false/1/0) for key '" + configKey + "', got: " + configValue);
                }
            }
            // STRING and TEXT types accept anything
            default -> {
                // No specific validation
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
