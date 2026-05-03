package de.vptr.aimathtutor.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
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

    // Default values for runtime reset to factory defaults.
    private static final Map<String, String> DEFAULT_VALUES = Map.ofEntries(
            Map.entry("ai.tutor.enabled", "true"),
            Map.entry("ai.tutor.provider", "mock"),
            Map.entry("gemini.model", "gemma-3-27b-it"),
            Map.entry("gemini.api.base-url", "https://generativelanguage.googleapis.com"),
            Map.entry("gemini.temperature", "0.7"),
            Map.entry("gemini.max-tokens", "2000"),
            Map.entry("openai.model", "gpt-5-nano"),
            Map.entry("openai.organization-id", ""),
            Map.entry("openai.api.base-url", "https://api.openai.com/v1"),
            Map.entry("openai.temperature", "0.7"),
            Map.entry("openai.max-tokens", "2000"),
            Map.entry("ollama.api.url", "http://ollama:11434"),
            Map.entry("ollama.model", "llama3.2:3b"),
            Map.entry("ollama.temperature", "0.7"),
            Map.entry("ollama.max-tokens", "2000"),
            Map.entry("ollama.timeout-seconds", "30"),
            Map.entry("ai.prompt.question.answering.prefix",
                    "You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question."),
            Map.entry("ai.prompt.question.answering.postfix",
                    "Provide a helpful, encouraging answer that:\n"
                            + "- Guides the student's thinking without solving it for them\n"
                            + "- Is concise (2-3 sentences max)\n"
                            + "- Relates to their current problem if possible\n"
                            + "- Uses clear, simple language\n"
                            + "- Encourages them to try the next step\n\nYour answer:"),
            Map.entry("ai.prompt.math.tutoring.prefix",
                    "You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student's action and provide brief, helpful feedback."),
            Map.entry("ai.prompt.math.tutoring.postfix",
                    "Provide feedback in the following JSON format:\n"
                            + "{\n"
                            + "  \"type\": \"POSITIVE\" or \"CORRECTIVE\" or \"HINT\" or \"SUGGESTION\",\n"
                            + "  \"message\": \"Your brief, encouraging feedback (ONE sentence only)\",\n"
                            + "  \"hints\": [],\n"
                            + "  \"suggestedNextSteps\": [],\n"
                            + "  \"confidence\": 0.0 to 1.0\n"
                            + "}\n\n"
                            + "IMPORTANT Guidelines:\n"
                            + "- Keep message to ONE SHORT sentence (max 15 words)\n"
                            + "- Be encouraging but not overly enthusiastic\n"
                            + "- If the action is correct, give brief praise\n"
                            + "- If incorrect, point out the error gently\n"
                            + "- Only provide hints array if student made a mistake (max 1-2 hints)\n"
                            + "- Do NOT provide hints for correct actions\n"
                            + "- Leave suggestedNextSteps empty unless specifically needed\n"
                            + "- Be specific about what they did, not generic"));

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
     * Retrieves a temperature configuration value clamped to the valid range
     * [0.0, 2.0].
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found or parsing fails
     * @return the clamped temperature value
     */
    public double getClampedTemperature(final String key, final double defaultValue) {
        final Double value = this.getConfigValueAsDouble(key, defaultValue);
        return (value != null) ? Math.max(0.0, Math.min(2.0, value)) : defaultValue;
    }

    /**
     * Retrieves a max-tokens configuration value clamped to the valid range
     * [1, 8192].
     *
     * @param key          the configuration key
     * @param defaultValue the default value if not found or parsing fails
     * @return the clamped token limit
     */
    public int getClampedTokens(final String key, final int defaultValue) {
        final Integer value = this.getConfigValueAsInt(key, defaultValue);
        return (value != null) ? Math.max(1, Math.min(8192, value)) : defaultValue;
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
    public void updateConfig(final String configKey, final String configValue, final Long userId) {
        // Only require configKey to be non-null. Values can be null or empty
        if (configKey == null) {
            throw new IllegalArgumentException("Configuration key cannot be null");
        }

        this.validateConfigValue(configKey, configValue);

        // Perform URL validation outside of any DB transaction to avoid holding
        // a connection during potentially slow DNS resolution.
        if (configValue != null && !configValue.isBlank()
                && (configKey.endsWith(".base-url") || configKey.endsWith(".url"))) {
            this.validateUrlSafe(configKey, configValue);
        }

        this.persistConfigUpdate(configKey, configValue, userId);
    }

    @Transactional
    void persistConfigUpdate(final String configKey, final String configValue, final Long userId) {
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
            newEntity.configType = "STRING";
            newEntity.category = "UNKNOWN";
            return newEntity;
        });

        entity.configValue = configValue;
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
    public void updateMultipleConfigs(final List<AiConfigUpdateDto> updates, final Long userId) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        // Validate all updates first (outside any DB transaction)
        for (final AiConfigUpdateDto update : updates) {
            if (update.configKey == null) {
                throw new IllegalArgumentException("Configuration key cannot be null");
            }
            this.validateConfigValue(update.configKey, update.configValue);
            if (update.configValue != null && !update.configValue.isBlank()
                    && (update.configKey.endsWith(".base-url") || update.configKey.endsWith(".url"))) {
                this.validateUrlSafe(update.configKey, update.configValue);
            }
        }

        this.persistMultipleConfigUpdates(updates, userId);
    }

    @Transactional
    void persistMultipleConfigUpdates(final List<AiConfigUpdateDto> updates, final Long userId) {
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

        // Persist all updates
        for (final AiConfigUpdateDto update : updates) {
            final Optional<AiConfigEntity> existing = this.aiConfigRepository.findByConfigKey(update.configKey);
            final AiConfigEntity entity = existing.orElseGet(() -> {
                final AiConfigEntity newEntity = new AiConfigEntity();
                newEntity.configKey = update.configKey;
                newEntity.configType = "STRING";
                newEntity.category = "UNKNOWN";
                return newEntity;
            });

            entity.configValue = update.configValue;
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
                    final int intValue = Integer.parseInt(configValue);
                    if (configKey.contains("max-tokens") && (intValue < 1 || intValue > 8192)) {
                        throw new IllegalArgumentException(
                                "Value for '" + configKey + "' must be between 1 and 8192, got: " + configValue);
                    }
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Value must be a valid integer for key '" + configKey + "', got: " + configValue);
                }
            }
            case "DOUBLE" -> {
                try {
                    final double doubleValue = Double.parseDouble(configValue);
                    if (configKey.contains("temperature") && (doubleValue < 0.0 || doubleValue > 2.0)) {
                        throw new IllegalArgumentException(
                                "Value for '" + configKey + "' must be between 0.0 and 2.0, got: " + configValue);
                    }
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
     * Validates that a URL is safe and does not enable SSRF attacks.
     * Enforces HTTPS for external providers, blocks private IP ranges,
     * and rejects localhost/loopback addresses.
     */
    private void validateUrlSafe(final String configKey, final String configValue) {
        final URI uri;
        try {
            uri = URI.create(configValue);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Value must be a valid URL for key '" + configKey + "'");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host for key '" + configKey + "'");
        }

        final String host = uri.getHost().toLowerCase();

        // Enforce HTTPS for external providers (Gemini, OpenAI)
        if (configKey.contains("gemini") || configKey.contains("openai")) {
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException(
                        "External provider URLs must use HTTPS for key '" + configKey + "'");
            }
        }

        // Block localhost and loopback
        if ("localhost".equals(host) || "127.0.0.1".equals(host) || host.startsWith("127.")
                || "0.0.0.0".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host)) {
            throw new IllegalArgumentException(
                    "Loopback addresses are not allowed for key '" + configKey + "'");
        }

        // Block private IP ranges by resolving the host
        try {
            final InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalArgumentException(
                        "Private IP addresses are not allowed for key '" + configKey + "'");
            }
        } catch (final UnknownHostException e) {
            // Allow unresolved hostnames (they may be internal Docker hosts)
            // but block obvious private patterns without DNS
        }

        // Block common private IPv4 patterns without DNS resolution
        if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) {
            throw new IllegalArgumentException(
                    "Private IP addresses are not allowed for key '" + configKey + "'");
        }
        if (host.startsWith("172.")) {
            final String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                try {
                    final int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        throw new IllegalArgumentException(
                                "Private IP addresses are not allowed for key '" + configKey + "'");
                    }
                } catch (final NumberFormatException e) {
                    // Not a numeric octet, allow
                }
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
     * Resets all known AI configuration values to their factory defaults.
     *
     * @param userId the ID of the user performing the reset
     */
    @Transactional
    public void resetToDefaults(final Long userId) {
        final var updates = DEFAULT_VALUES.entrySet().stream()
                .map(e -> new AiConfigUpdateDto(e.getKey(), e.getValue()))
                .toList();
        this.updateMultipleConfigs(updates, userId);
        LOG.info("All AI configurations reset to defaults by userId='{}'", userId);
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
                entity.category, entity.description, entity.lastEdit, lastUpdatedByName);
    }
}
