package de.vptr.aimathtutor.service.ai;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.AiConfigDto;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.AiConfigChangedEvent;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.ValidationException;

/**
 * Service for managing AI configuration at runtime. Provides methods to retrieve, validate, and update AI settings from
 * the database. Supports caching to avoid frequent database hits.
 */
@ApplicationScoped
public class AiConfigService {

    private static final Logger LOG = Logger.getLogger(AiConfigService.class);

    // Internal cache for configuration values to reduce database hits.
    private final AiConfigCache cache = new AiConfigCache();

    // The built-in defaults; read one through defaultValue(key).
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

    /**
     * Returns the built-in default of a setting.
     *
     * @param key
     *            the configuration key, one of the {@link AiConfigKeys} constants
     * @return the built-in default value
     * @throws IllegalArgumentException
     *             if the key has no built-in default
     */
    public static String defaultValue(final String key) {
        final var value = DEFAULT_VALUES.get(key);
        if (value == null) {
            throw new IllegalArgumentException("No built-in default for AI setting '" + key + "'");
        }
        return value;
    }

    @Inject
    private AiConfigRepository aiConfigRepository;

    @Inject
    private PermissionService permissionService;

    @Inject
    private HostResolver hostResolver;

    @Inject
    Event<AiConfigChangedEvent> configChangedEvent;

    @ConfigProperty(name = "app.security.allowed-ollama-hosts", defaultValue = "ollama,localhost")
    Set<String> allowedOllamaHosts = Set.of("ollama", "localhost");

    @ConfigProperty(name = "app.security.allowed-google-hosts", defaultValue = "generativelanguage.googleapis.com")
    Set<String> allowedGoogleHosts = Set.of("generativelanguage.googleapis.com");

    @ConfigProperty(name = "app.security.allowed-openai-hosts", defaultValue = "api.openai.com")
    Set<String> allowedOpenAiHosts = Set.of("api.openai.com");

    /**
     * Retrieves a configuration value as a String. Falls back to defaultValue if not found. A miss reads the row and
     * caches it only if no write evicted anything meanwhile (see {@link AiConfigCache}). A miss read while the calling
     * thread has a transaction, whatever its status (rollback-only included), is returned but never cached: it may be
     * an uncommitted value or an entity loaded before a concurrent commit, and a transaction that is not active gets no
     * after-completion notification to evict it. A cache hit is returned in any case.
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

        final String cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Every status but NO_TRANSACTION counts, a rollback-only transaction included
        final boolean inTransaction = QuarkusTransaction.getStatus() != Status.STATUS_NO_TRANSACTION;
        // Read before the row, so an eviction after this point discards the fill
        final long seen = this.cache.generation();
        return this.aiConfigRepository.findByConfigKey(key).map(entity -> {
            if (!inTransaction) {
                this.cache.putIfCurrent(key, entity.configValue, seen);
            }
            return entity.configValue;
        }).orElse(defaultValue);
    }

    /**
     * Returns the stored value of a setting, or its built-in default when the row or its value is missing.
     *
     * @param key
     *            one of the keys of the built-in defaults
     * @return the stored value or the built-in default
     * @throws IllegalArgumentException
     *             if the key has no built-in default
     */
    public String getConfigValue(final String key) {
        final var fallback = defaultValue(key);
        return Objects.requireNonNullElse(this.getConfigValue(key, fallback), fallback);
    }

    /**
     * Returns every known AI setting: the stored value, or the built-in default for a missing row. A stored temperature
     * that does not parse or is not finite, or a token limit that does not parse, is replaced by its default, so a
     * caller can parse those values without a fallback of its own.
     * <p>
     * Deliberately not {@code @Transactional}: the reads go through {@link #getConfigValue}, which fills the cache only
     * outside a transaction, so a transactional snapshot would never fill it. There is no permission check: the values
     * hold no API keys and no user data, and the settings page is readable without the edit permission. The map is a
     * set of per-key reads, not one atomic read, so a concurrent save can leave it mixing old and new values.
     *
     * @return an unmodifiable map holding a value for every key of the built-in defaults
     */
    public Map<String, String> getEffectiveConfig() {
        final var values = new HashMap<String, String>();
        DEFAULT_VALUES.forEach((key, defaultValue) -> {
            if (key.endsWith(AiConfigKeys.SUFFIX_TEMPERATURE)) {
                values.put(key, String.valueOf(this.getConfigValueAsDouble(key, Double.valueOf(defaultValue))));
            } else if (key.endsWith(AiConfigKeys.SUFFIX_MAX_TOKENS)) {
                values.put(key, String.valueOf(this.getConfigValueAsInt(key, Integer.valueOf(defaultValue))));
            } else {
                values.put(key, this.getConfigValue(key));
            }
        });
        return Map.copyOf(values);
    }

    /** The built-in defaults as updates, in no particular order. */
    static List<AiConfigUpdateDto> defaultUpdates() {
        return DEFAULT_VALUES.entrySet().stream().map(e -> new AiConfigUpdateDto(e.getKey(), e.getValue())).toList();
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
     * Retrieves a configuration value as a Double. Falls back to defaultValue if not found, if parsing fails or if the
     * value is not finite.
     *
     * @param key
     *            the configuration key
     * @param defaultValue
     *            the default value if not found, unparseable or not finite
     * @return the configuration value or default
     */
    public Double getConfigValueAsDouble(final String key, final Double defaultValue) {
        final String value = this.getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            final double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                LOG.warnf("Ignoring non-finite double config '%s'", key);
                return defaultValue;
            }
            return parsed;
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
     * Retrieves a temperature configuration value clamped to the valid range [0.0, 2.0]. A missing, unparseable or
     * non-finite value yields the built-in default of the key.
     *
     * @param key
     *            the key of a temperature setting (an {@link AiConfigKeys} {@code *_TEMPERATURE} constant)
     * @return the clamped temperature value
     * @throws IllegalArgumentException
     *             if the key has no numeric built-in default
     */
    public double getClampedTemperature(final String key) {
        final double value = this.getConfigValueAsDouble(key, Double.valueOf(defaultValue(key)));
        return Math.max(0.0, Math.min(2.0, value));
    }

    /**
     * Retrieves a max-tokens configuration value clamped to the valid range [1, 8192]. A missing or unparseable value
     * yields the built-in default of the key.
     *
     * @param key
     *            the key of a max-tokens setting (an {@link AiConfigKeys} {@code *_MAX_TOKENS} constant)
     * @return the clamped token limit
     * @throws IllegalArgumentException
     *             if the key has no numeric built-in default
     */
    public int getClampedTokens(final String key) {
        final int value = this.getConfigValueAsInt(key, Integer.valueOf(defaultValue(key)));
        return Math.max(1, Math.min(8192, value));
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
     * Updates a single configuration value. Validates the input before persisting. Starts no transaction of its own
     * ({@code SUPPORTS}): the URL validation's DNS resolution must not hold a connection. Inside a caller's transaction
     * it runs in that one, and any exception it throws marks it for rollback.
     *
     * @param configKey
     *            the configuration key to update
     * @param configValue
     *            the new value
     * @param credentials
     *            the credentials captured by {@code AuthService.currentSessionCredentials()} when the user acted, or
     *            null
     * @throws ValidationException
     *             if the session has ended or validation fails
     * @throws PermissionDeniedException
     *             if the user's rank lacks the permission
     */
    @Transactional(TxType.SUPPORTS)
    public void updateConfig(final String configKey, final String configValue,
            @Nullable final SessionCredentials credentials) {
        // Authorize before validating values or resolving any host names, so an unauthorized caller
        // can never trigger DNS resolution of an attacker-supplied host via a generic URL key. Without a caller's
        // transaction this runs in a short one of its own, closed before any validation or DNS lookup.
        this.requireConfigEditPermission(credentials);

        this.validateUpdate(configKey, configValue);

        this.persistConfigUpdate(configKey, configValue, credentials);
    }

    @Transactional
    void persistConfigUpdate(final String configKey, final String configValue,
            @Nullable final SessionCredentials credentials) {
        // Re-authorize inside the transaction (not redundant with the caller's pre-DNS check): this guards
        // against permission revocation between that check and the write (TOCTOU) and yields the managed
        // user entity recorded as lastUpdatedBy.
        final UserEntity user = this.requireConfigEditPermission(credentials);

        // Defensive: fills inside a transaction are never cached, but firing before the first write also keeps the
        // eviction after completion when a later step fails
        this.configChangedEvent.fire(new AiConfigChangedEvent(List.of(configKey)));

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
        this.cache.evict(List.of(configKey));

        LOG.infof("Configuration updated: key='%s', updatedBy='%s'", configKey, user.username);
    }

    /**
     * Updates multiple configuration values at once. All updates are validated before any are persisted. Starts no
     * transaction of its own ({@code SUPPORTS}): the URL validation's DNS resolution must not hold a connection. Inside
     * a caller's transaction it runs in that one, and any exception it throws marks it for rollback.
     *
     * @param updates
     *            a list of {@link AiConfigUpdateDto} objects containing key-value pairs
     * @param credentials
     *            the credentials captured by {@code AuthService.currentSessionCredentials()} when the user acted, or
     *            null
     * @throws ValidationException
     *             if the session has ended or any validation fails
     * @throws PermissionDeniedException
     *             if the user's rank lacks the permission
     */
    @Transactional(TxType.SUPPORTS)
    public void updateMultipleConfigs(final List<AiConfigUpdateDto> updates,
            @Nullable final SessionCredentials credentials) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        // Authorize before validating values or resolving any host names, so an unauthorized caller
        // can never trigger DNS resolution of an attacker-supplied host via a generic URL key. Without a caller's
        // transaction this runs in a short one of its own, closed before any validation or DNS lookup.
        this.requireConfigEditPermission(credentials);

        // Validate all updates first (outside any DB transaction)
        for (final AiConfigUpdateDto update : updates) {
            this.validateUpdate(update.configKey, update.configValue);
        }

        this.persistMultipleConfigUpdates(updates, credentials);
    }

    @Transactional
    void persistMultipleConfigUpdates(final List<AiConfigUpdateDto> updates,
            @Nullable final SessionCredentials credentials) {
        // Re-authorize inside the transaction: after a save's pre-DNS check this guards against permission
        // revocation between that check and the write (TOCTOU); for a reset it is the only check. It also yields the
        // managed user entity recorded as lastUpdatedBy.
        final UserEntity user = this.requireConfigEditPermission(credentials);

        // Defensive: fills inside a transaction are never cached, but firing before the first write also keeps the
        // eviction after completion when a later step fails
        this.configChangedEvent
                .fire(new AiConfigChangedEvent(updates.stream().map(update -> update.configKey).toList()));

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
            this.cache.evict(List.of(update.configKey));
        }

        LOG.infof("Multiple configurations updated: count=%s, updatedBy='%s'", updates.size(), user.username);
    }

    /**
     * Validates one update, outside any DB transaction so a slow DNS resolution holds no connection.
     *
     * @throws ValidationException
     *             if the key is null or the value is invalid
     */
    private void validateUpdate(@Nullable final String configKey, @Nullable final String configValue) {
        try {
            if (configKey == null) {
                throw new IllegalArgumentException("Configuration key cannot be null");
            }
            this.validateConfigValue(configKey, configValue);
            if (configValue != null && !configValue.isBlank()
                    && (configKey.endsWith(".base-url") || configKey.endsWith(".url"))) {
                this.validateUrlSafe(configKey, configValue);
            }
        } catch (final IllegalArgumentException e) {
            throw new ValidationException(e.getMessage(), e);
        }
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
                    if (configKey.contains("temperature") && !(doubleValue >= 0.0 && doubleValue <= 2.0)) {
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
     * An address prefix of the IANA special-purpose registries: the network address bytes (4 for IPv4, 16 for IPv6) and
     * the prefix length in bits. A class, not a record, because Error Prone refuses array record components.
     */
    private static final class Cidr {

        private final byte[] prefix;
        private final int length;

        private Cidr(final byte[] prefix, final int length) {
            this.prefix = prefix;
            this.length = length;
        }

        /** Parses CIDR notation such as {@code 10.0.0.0/8}, without any name lookup. */
        static Cidr parse(final String cidr) {
            final int slash = cidr.indexOf('/');
            final byte[] bytes = InetAddress.ofLiteral(cidr.substring(0, slash)).getAddress();
            final int length = Integer.parseInt(cidr.substring(slash + 1));
            if (length < 0 || length > bytes.length * Byte.SIZE) {
                throw new IllegalArgumentException("Prefix length out of range: " + cidr);
            }
            return new Cidr(bytes, length);
        }

        /** The four bytes right after the prefix: the IPv4 address a translation prefix embeds. */
        byte[] embeddedBytes(final byte[] candidate) {
            final int offset = this.length / Byte.SIZE;
            return Arrays.copyOfRange(candidate, offset, offset + Integer.BYTES);
        }

        /** Whether the first {@code length} bits of the candidate equal the prefix's; false for the other family. */
        boolean matches(final byte[] candidate) {
            if (candidate.length != this.prefix.length) {
                return false;
            }
            final int fullBytes = this.length / Byte.SIZE;
            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != this.prefix[i]) {
                    return false;
                }
            }
            final int rest = this.length % Byte.SIZE;
            if (rest == 0) {
                return true;
            }
            final int mask = (0xFF << (Byte.SIZE - rest)) & 0xFF;
            return (candidate[fullBytes] & mask) == (this.prefix[fullBytes] & mask);
        }
    }

    /**
     * The ranges the IANA IPv4 and IPv6 special-purpose address registries mark "Globally Reachable: False", as of
     * 2026-10 (https://www.iana.org/assignments/iana-ipv4-special-registry/ and
     * https://www.iana.org/assignments/iana-ipv6-special-registry/). {@code 192.0.0.0/24} and {@code 2001::/23} are
     * refused whole, including the sub-ranges inside them marked True, N/A or nothing, Teredo among them; none holds a
     * provider. Ranges marked True or N/A elsewhere are not refused for that reason alone. The IPv4-mapped
     * {@code ::ffff:0:0/96}, also marked False, is not listed: {@link #TRANSLATION_PREFIXES} judges it by its embedded
     * IPv4.
     */
    private static final List<Cidr> NON_PUBLIC_RANGES = Stream.of("0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10",
            "127.0.0.0/8", "169.254.0.0/16", "172.16.0.0/12", "192.0.0.0/24", "192.0.2.0/24", "192.88.99.2/32",
            "192.168.0.0/16", "198.18.0.0/15", "198.51.100.0/24", "203.0.113.0/24", "240.0.0.0/4", "255.255.255.255/32",
            "::/128", "::1/128", "64:ff9b:1::/48", "100::/64", "100:0:0:1::/64", "2001::/23", "2001:db8::/32",
            "3fff::/20", "5f00::/16", "fc00::/7", "fe80::/10").map(Cidr::parse).toList();

    /**
     * The IPv6 prefixes that embed an IPv4 address, which then decides: IPv4-mapped, NAT64 and 6to4. The mapped prefix
     * is built from 16 explicit bytes because {@code ofLiteral} collapses {@code ::ffff:0:0} to a 4-byte IPv4 address.
     */
    private static final List<Cidr> TRANSLATION_PREFIXES =
            List.of(new Cidr(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0, 0, 0 }, 96),
                    Cidr.parse("64:ff9b::/96"), Cidr.parse("2002::/16"));

    /**
     * Checks if the provided {@link InetAddress} is a non-public address: loopback, site-local, link-local, multicast
     * or unspecified per the JDK, or inside a range of {@link #NON_PUBLIC_RANGES}. An IPv6 address in one of the
     * {@link #TRANSLATION_PREFIXES} (IPv4-mapped, NAT64, 6to4) is judged by the IPv4 address it embeds.
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
        final byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            for (final Cidr prefix : TRANSLATION_PREFIXES) {
                if (prefix.matches(bytes)) {
                    return this.isNonPublicAddress(embeddedIpv4(prefix.embeddedBytes(bytes)));
                }
            }
        }
        return NON_PUBLIC_RANGES.stream().anyMatch(range -> range.matches(bytes));
    }

    /** Wraps four bytes as an {@link InetAddress}. */
    private static InetAddress embeddedIpv4(final byte[] ipv4) {
        try {
            return InetAddress.getByAddress(ipv4);
        } catch (final UnknownHostException e) {
            // Only a length other than 4 or 16 throws, and four bytes are passed
            throw new IllegalStateException("Embedded IPv4 address is malformed", e);
        }
    }

    /**
     * Validates that a provider API URL is safe to call. Used by {@link #validateUrlSafe} and immediately before HTTP
     * calls. This method re-runs SSRF checks immediately before each HTTP request to close the TOCTOU window between
     * admin save and use. The HTTP client resolves the host again when it connects; that DNS-rebinding window is an
     * accepted risk (AGENTS.md, "AI Configuration"). Ollama is allow-list-only; private addresses are permitted because
     * Docker service names like {@code ollama} intentionally resolve into the container network. The allow-list itself
     * is the trust boundary.
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
            final InetAddress[] addresses = this.hostResolver.resolveAll(host);
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
            // Unknown provider key — fall back to a strict check: require HTTPS and reject non-public addresses.
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
     * non-public IP ranges.
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
            final InetAddress[] addresses = this.hostResolver.resolveAll(host);
            for (final InetAddress address : addresses) {
                if (this.isNonPublicAddress(address)) {
                    throw new IllegalArgumentException(
                            "Non-public IP addresses are not allowed for key '" + configKey + "'");
                }
            }
        } catch (final UnknownHostException e) {
            LOG.debugf(e, "Hostname resolution failed for %s", host);
            throw new IllegalArgumentException("URL host must resolve to a public address for key '" + configKey + "'");
        }
    }

    /**
     * Evicts the written keys once the writing transaction has completed, on commit and on rollback. It serves readers
     * outside that transaction, whose fill may have read the old row before the commit.
     *
     * @param event
     *            the keys the completed transaction wrote
     */
    void onConfigChanged(@Observes(during = TransactionPhase.AFTER_COMPLETION) final AiConfigChangedEvent event) {
        this.cache.evict(event.configKeys());
    }

    /**
     * Clears the configuration cache; for tests and manual refreshes, no production code calls it.
     */
    public void clearCache() {
        this.cache.clear();
        LOG.info("Configuration cache cleared");
    }

    /**
     * Resets all known AI configuration values to their built-in defaults. The defaults are written without validation:
     * they are constants, and every provider call and connection test validates the stored URL again, so a reset needs
     * no DNS lookup and succeeds where the built-in hosts do not resolve. A default whose host the operator removed
     * from an allow-list is written anyway and refused when it is used. The session is checked in the write's own
     * transaction, which joins a caller's; the reset is all-or-nothing. A URL key added to the defaults needs a check
     * where it is used before a reset may write it unvalidated.
     *
     * @param credentials
     *            the credentials captured by {@code AuthService.currentSessionCredentials()} when the user acted, or
     *            null
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the user's rank lacks the permission
     */
    @Transactional(TxType.SUPPORTS)
    public void resetToDefaults(@Nullable final SessionCredentials credentials) {
        this.persistMultipleConfigUpdates(defaultUpdates(), credentials);
        LOG.info("All AI configurations reset to defaults");
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

    /**
     * Authorizes the captured session for AI-config edits. Transactional because resolving the session user needs a
     * transaction: called from the public methods with no caller transaction it runs in its own short one, before any
     * validation or DNS lookup; from the persist methods it joins theirs. {@link ProviderTestService}'s connection
     * tests ({@code SUPPORTS}) rely on that short transaction too.
     */
    @Transactional
    UserEntity requireConfigEditPermission(@Nullable final SessionCredentials credentials) {
        return this.permissionService.requireAiConfigEdit(credentials);
    }
}
