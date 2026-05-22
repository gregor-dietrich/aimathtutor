package de.vptr.aimathtutor.service.ai;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiConfigDto;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing AI configuration at runtime. Provides methods to retrieve, validate, and update AI settings from
 * the database. Supports caching to avoid frequent database hits.
 */
@ApplicationScoped
public class AiConfigService {

    private static final Logger LOG = Logger.getLogger(AiConfigService.class);

    // Internal cache for configuration values to reduce database hits.
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    // Default values for runtime reset to factory defaults.
    private static final Map<String, String> DEFAULT_VALUES = Map.ofEntries(
            Map.entry(AiConfigKeys.AI_TUTOR_ENABLED, "true"), Map.entry(AiConfigKeys.AI_TUTOR_PROVIDER, "mock"),
            Map.entry(AiConfigKeys.GOOGLE_MODEL, "gemini-3.1-flash-lite"),
            Map.entry(AiConfigKeys.GOOGLE_API_BASE_URL, "https://generativelanguage.googleapis.com"),
            Map.entry(AiConfigKeys.GOOGLE_TEMPERATURE, "0.7"), Map.entry(AiConfigKeys.GOOGLE_MAX_TOKENS, "2000"),
            Map.entry(AiConfigKeys.OPENAI_MODEL, "gpt-5-nano"), Map.entry(AiConfigKeys.OPENAI_ORGANIZATION_ID, ""),
            Map.entry(AiConfigKeys.OPENAI_API_BASE_URL, "https://api.openai.com/v1"),
            Map.entry(AiConfigKeys.OPENAI_TEMPERATURE, "0.7"), Map.entry(AiConfigKeys.OPENAI_MAX_TOKENS, "2000"),
            Map.entry(AiConfigKeys.OLLAMA_API_URL, "http://ollama:11434"),
            Map.entry(AiConfigKeys.OLLAMA_MODEL, "llama3.2:3b"), Map.entry(AiConfigKeys.OLLAMA_TEMPERATURE, "0.7"),
            Map.entry(AiConfigKeys.OLLAMA_MAX_TOKENS, "2000"),
            Map.entry(AiConfigKeys.PROMPT_QUESTION_PREFIX, AppConstants.PROMPT_QUESTION_ANSWERING_PREFIX),
            Map.entry(AiConfigKeys.PROMPT_QUESTION_POSTFIX, AppConstants.PROMPT_QUESTION_ANSWERING_POSTFIX),
            Map.entry(AiConfigKeys.PROMPT_TUTORING_PREFIX, AppConstants.PROMPT_MATH_TUTORING_PREFIX),
            Map.entry(AiConfigKeys.PROMPT_TUTORING_POSTFIX, AppConstants.PROMPT_MATH_TUTORING_POSTFIX));

    @Inject
    private AiConfigRepository aiConfigRepository;

    @Inject
    private PermissionService permissionService;

    @Inject
    @ConfigProperty(name = "app.security.allowed-ollama-hosts", defaultValue = "ollama,localhost")
    Set<String> allowedOllamaHosts = Set.of("ollama", "localhost");

    @Inject
    @ConfigProperty(name = "app.security.allowed-google-hosts", defaultValue = "generativelanguage.googleapis.com")
    Set<String> allowedGoogleHosts = Set.of("generativelanguage.googleapis.com");

    @Inject
    @ConfigProperty(name = "app.security.allowed-openai-hosts", defaultValue = "api.openai.com")
    Set<String> allowedOpenAiHosts = Set.of("api.openai.com");

    /**
     * Retrieves a configuration value as a String. Falls back to defaultValue if not found.
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found
     * @return the configuration value or default
     */
    @Nullable
    public String getConfigValue(final String key, @Nullable final String defaultValue) {
        if (key == null) {
            return defaultValue;
        }

        final String cached = this.configCache.get(key);
        if (cached != null) {
            return cached;
        }

        return this.aiConfigRepository.findByConfigKey(key).map(entity -> {
            if (entity.configValue != null) {
                this.configCache.put(entity.configKey, entity.configValue);
            } else {
                this.configCache.remove(entity.configKey);
            }
            return entity.configValue;
        }).orElse(defaultValue);
    }

    /**
     * Retrieves a configuration value as an Integer. Falls back to defaultValue if not found or if parsing fails.
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found or parsing fails
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
            LOG.warnf(e, "Failed to parse integer config '%s' with value '%s'", key, value);
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration value as a Double. Falls back to defaultValue if not found or if parsing fails.
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found or parsing fails
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
            LOG.warnf(e, "Failed to parse double config '%s' with value '%s'", key, value);
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration value as a Boolean. Falls back to defaultValue if not found. Accepts "true", "false"
     * (case-insensitive) and "1", "0".
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found
     * @return the configuration value or default
     */
    public Boolean getConfigValueAsBoolean(final String key, final Boolean defaultValue) {
        final String value = this.getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        final String lower = value.toLowerCase(Locale.ROOT).trim();
        if ("true".equals(lower) || "1".equals(lower)) {
            return true;
        }
        if ("false".equals(lower) || "0".equals(lower)) {
            return false;
        }
        LOG.warnf("Failed to parse boolean config '%s' with value '%s', using default", key, value);
        return defaultValue;
    }

    /**
     * Retrieves a temperature configuration value clamped to the valid range [0.0, 2.0].
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found or parsing fails
     * @return the clamped temperature value
     */
    public double getClampedTemperature(final String key, final double defaultValue) {
        final Double value = this.getConfigValueAsDouble(key, defaultValue);
        return (value != null) ? Math.max(0.0, Math.min(2.0, value)) : defaultValue;
    }

    /**
     * Retrieves a max-tokens configuration value clamped to the valid range [1, 8192].
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found or parsing fails
     * @return the clamped token limit
     */
    public int getClampedTokens(final String key, final int defaultValue) {
        final Integer value = this.getConfigValueAsInt(key, defaultValue);
        return (value != null) ? Math.max(1, Math.min(8192, value)) : defaultValue;
    }

    /**
     * Retrieves all configuration entries in a specific category as a key-value map. Useful for populating UI forms.
     *
     * @param category
     *            the category to retrieve
     * @return a map of all config keys and values in the category
     */
    public Map<String, String> getAllConfigsByCategory(final ConfigCategory category) {
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
        return this.aiConfigRepository.findAll().stream().map(this::entityToDto).toList();
    }

    /**
     * Retrieves all configuration entries in a category as DTOs.
     *
     * @param category
     *            the category to retrieve
     * @return a list of {@link AiConfigDto} objects in the category
     */
    public List<AiConfigDto> getConfigsByCategory(final ConfigCategory category) {
        return this.aiConfigRepository.findByCategory(category).stream().map(this::entityToDto).toList();
    }

    /**
     * Updates a single configuration value. Validates the input before persisting.
     *
     * @param configKey
     *            the configuration key to update
     * @param configValue
     *            the new value
     * @param userId
     *            the ID of the user making the update (for audit trail)
     * @throws IllegalArgumentException
     *             if validation fails
     * @throws IllegalStateException
     *             if user is not an admin
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
        final UserEntity user = this.requireConfigEditPermission(userId);

        // Find existing or create new
        final var existing = this.aiConfigRepository.findByConfigKey(configKey);
        final var entity = existing.orElseGet(() -> {
            final var newEntity = new AiConfigEntity();
            newEntity.configKey = configKey;
            newEntity.configType = ConfigType.STRING;
            newEntity.category = ConfigCategory.GENERAL;
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

        LOG.infof("Configuration updated: key='%s', updatedBy='%s'", configKey, user.username);
    }

    /**
     * Updates multiple configuration values at once. All updates are validated before any are persisted.
     *
     * @param updates
     *            a list of {@link AiConfigUpdateDto} objects containing key-value pairs
     * @param userId
     *            the ID of the user making the updates
     * @throws IllegalArgumentException
     *             if any validation fails
     * @throws IllegalStateException
     *             if user is not an admin
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
        final UserEntity user = this.requireConfigEditPermission(userId);

        // Persist all updates
        for (final AiConfigUpdateDto update : updates) {
            final Optional<AiConfigEntity> existing = this.aiConfigRepository.findByConfigKey(update.configKey);
            final AiConfigEntity entity = existing.orElseGet(() -> {
                final AiConfigEntity newEntity = new AiConfigEntity();
                newEntity.configKey = update.configKey;
                newEntity.configType = ConfigType.STRING;
                newEntity.category = ConfigCategory.GENERAL;
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

        LOG.infof("Multiple configurations updated: count=%s, updatedBy='%s'", updates.size(), user.username);
    }

    /**
     * Validates a configuration value based on its type and optionality. Type validation is determined by the entity's
     * configType field. Optionality is determined by the entity's isOptional flag.
     *
     * @param configKey
     *            the configuration key
     * @param configValue
     *            the value to validate
     * @throws IllegalArgumentException
     *             if validation fails
     */
    private void validateConfigValue(final String configKey, @Nullable final String configValue) {
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

        Objects.requireNonNull(configValue);
        final var configType = existingEntity.get().configType;
        if (configType == null) {
            // No type constraint defined
            return;
        }

        // Type-based validation
        switch (configType) {
            case INTEGER -> {
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
            case DOUBLE -> {
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
            case BOOLEAN -> {
                final var lower = configValue.toLowerCase(Locale.ROOT).trim();
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
     * Identifies which AI provider a URL belongs to so the correct host allow-list is applied.
     */
    public enum ProviderType {
        OLLAMA, GOOGLE, OPENAI
    }

    /**
     * Checks if the provided {@link InetAddress} is a non-public address (loopback, site-local, link-local, multicast,
     * or IPv6 unique-local).
     *
     * @param address
     *            the address to check
     * @return {@code true} if the address is non-public, {@code false} otherwise
     */
    private boolean isNonPublicAddress(final InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isMulticastAddress() || address.isAnyLocalAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            final byte[] bytes = address.getAddress();
            // Block IPv6 unique-local fc00::/7 (first byte 0xFC or 0xFD)
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    /**
     * Validates that a provider API URL is safe to call. Used by {@link #validateUrlSafe} and immediately before HTTP
     * calls. This method re-runs SSRF checks immediately before each HTTP request to close the TOCTOU window between
     * admin save and use. Ollama is allow-list-only; private addresses are permitted because Docker service names like
     * {@code ollama} intentionally resolve into the container network. The allow-list itself is the trust boundary.
     *
     * @param url
     *            the URL to validate
     * @param providerType
     *            the provider type
     * @throws IllegalArgumentException
     *             when the URL fails any check
     */
    public void validateProviderApiUrl(@Nullable final String url, final ProviderType providerType) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(providerType + " API URL is blank");
        }
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(providerType + " API URL is not a valid URI", e);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(providerType + " API URL must have a host");
        }

        final String host = uri.getHost().toLowerCase(Locale.ROOT);
        final Set<String> allowed = switch (providerType) {
            case OLLAMA -> this.allowedOllamaHosts;
            case GOOGLE -> this.allowedGoogleHosts;
            case OPENAI -> this.allowedOpenAiHosts;
        };
        if (!allowed.contains(host)) {
            throw new IllegalArgumentException(providerType + " API host '" + host + "' is not in the allow-list. "
                    + "Update app.security.allowed-" + providerType.name().toLowerCase(Locale.ROOT)
                    + "-hosts to permit it.");
        }

        if (providerType == ProviderType.OLLAMA) {
            return;
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(providerType + " API URL must use HTTPS");
        }
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress address : addresses) {
                if (this.isNonPublicAddress(address)) {
                    throw new IllegalArgumentException(providerType + " API host '" + host
                            + "' resolved to a non-public address (possible DNS rebinding)");
                }
            }
        } catch (final UnknownHostException e) {
            LOG.debugf(e, "Hostname resolution failed for %s", host);
            throw new IllegalArgumentException(providerType + " API host '" + host + "' could not be resolved", e);
        }
    }

    /**
     * Validates that a URL is safe and does not enable SSRF attacks. Called at config-update time only; the
     * provider-side counterpart {@link #validateProviderApiUrl} re-runs the same checks immediately before each HTTP
     * request to close the TOCTOU window between save and use.
     */
    private void validateUrlSafe(final String configKey, final String configValue) {
        final ProviderType providerType;
        if (configKey.contains("ollama")) {
            providerType = ProviderType.OLLAMA;
        } else if (configKey.contains("google")) {
            providerType = ProviderType.GOOGLE;
        } else if (configKey.contains("openai")) {
            providerType = ProviderType.OPENAI;
        } else {
            // Unknown provider key — fall back to a strict check: require HTTPS and reject private/loopback.
            this.validateGenericUrlSafe(configKey, configValue);
            return;
        }
        try {
            this.validateProviderApiUrl(configValue, providerType);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Validation failed for key '" + configKey + "': " + e.getMessage(), e);
        }
    }

    /**
     * Strict URL validation for keys that do not correspond to a known provider (e.g. future custom endpoints). Blocks
     * loopback and private IP ranges.
     */
    private void validateGenericUrlSafe(final String configKey, final String configValue) {
        final URI uri;
        try {
            uri = URI.create(configValue);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Value must be a valid URL for key '" + configKey + "'", e);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host for key '" + configKey + "'");
        }
        final String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Generic API URL must use HTTPS for key '" + configKey + "'");
        }
        if (AppConstants.BLOCKED_HOST_LOCALHOST.equals(host) || AppConstants.BLOCKED_HOST_LOOPBACK_IPV4.equals(host)
                || host.startsWith("127.") || AppConstants.BLOCKED_HOST_ANY.equals(host)
                || AppConstants.BLOCKED_HOST_LOOPBACK_IPV6.equals(host)
                || AppConstants.BLOCKED_HOST_LOOPBACK_IPV6_EXPANDED.equals(host)) {
            throw new IllegalArgumentException("Loopback addresses are not allowed for key '" + configKey + "'");
        }
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress address : addresses) {
                if (this.isNonPublicAddress(address)) {
                    throw new IllegalArgumentException(
                            "Private IP addresses are not allowed for key '" + configKey + "'");
                }
            }
        } catch (final UnknownHostException e) {
            LOG.debugf(e, "Hostname resolution failed for %s", host);
            throw new IllegalArgumentException("URL host must resolve to a public address for key '" + configKey + "'");
        }
    }

    /**
     * Clears the configuration cache. Called when configurations are updated or when cache needs to be refreshed.
     */
    public void clearCache() {
        this.configCache.clear();
        LOG.info("Configuration cache cleared");
    }

    /**
     * Resets all known AI configuration values to their factory defaults.
     *
     * @param userId
     *            the ID of the user performing the reset
     */
    @Transactional
    public void resetToDefaults(final Long userId) {
        final var updates =
                DEFAULT_VALUES.entrySet().stream().map(e -> new AiConfigUpdateDto(e.getKey(), e.getValue())).toList();
        this.updateMultipleConfigs(updates, userId);
        LOG.infof("All AI configurations reset to defaults by userId='%s'", userId);
    }

    /**
     * Converts an AiConfigEntity to an AiConfigDto.
     *
     * @param entity
     *            the entity to convert
     * @return the corresponding DTO
     */
    private AiConfigDto entityToDto(final AiConfigEntity entity) {
        final String lastUpdatedByName = entity.lastUpdatedBy != null ? entity.lastUpdatedBy.username : "system";
        return new AiConfigDto(entity.publicId, entity.configKey, entity.configValue, entity.configType,
                entity.category, entity.description, entity.lastEdit, lastUpdatedByName);
    }

    private UserEntity requireConfigEditPermission(final Long userId) {
        return this.permissionService.requireAiConfigEdit(userId);
    }
}
