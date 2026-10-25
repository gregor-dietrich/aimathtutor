package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.TestSessionCredentials;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

/**
 * Unit tests for AiConfigService. Tests configuration CRUD operations, type conversions, validation, and caching.
 */
@QuarkusTest
@DisplayName("AiConfigService Tests")
class AiConfigServiceTest {

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    private UserEntity adminUser;

    private SessionCredentials credentials;

    @BeforeEach
    void setUp() {
        this.aiConfigService.clearCache();

        // The seeded admin, so the session credentials' stamp matches its stored password hash
        this.adminUser =
                Objects.requireNonNull(this.userRepository.findByUsername("admin"), "Seeded admin should exist");
        this.credentials = TestSessionCredentials.of(this.adminUser);
    }

    @AfterEach
    void tearDown() {
        this.aiConfigService.clearCache();
    }

    @Test
    @DisplayName("Get configuration value as String with default fallback")
    @TestTransaction
    void testGetConfigValueWithDefault() {
        final String value = this.aiConfigService.getConfigValue("nonexistent.key", "defaultValue");
        assertEquals("defaultValue", value);

        // Create a config and retrieve it
        final var entity = new AiConfigEntity("test.key", "test.value", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        final String retrieved = this.aiConfigService.getConfigValue("test.key", "defaultValue");
        assertEquals("test.value", retrieved);
    }

    @Test
    @DisplayName("Get configuration value as Integer with default fallback")
    @TestTransaction
    void testGetConfigValueAsInt() {
        // Non-existent key returns default
        Integer value = this.aiConfigService.getConfigValueAsInt("nonexistent.int", 42);
        assertEquals(42, value);

        // Create a numeric config
        final var entity = new AiConfigEntity("test.int", "123", ConfigType.INTEGER, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        value = this.aiConfigService.getConfigValueAsInt("test.int", 0);
        assertEquals(123, value);

        // Invalid integer format returns default
        final var invalidEntity =
                new AiConfigEntity("test.invalid.int", "not_a_number", ConfigType.INTEGER, ConfigCategory.GENERAL);
        invalidEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(invalidEntity);

        value = this.aiConfigService.getConfigValueAsInt("test.invalid.int", 99);
        assertEquals(99, value);
    }

    @Test
    @DisplayName("Get configuration value as Double with default fallback")
    @TestTransaction
    void testGetConfigValueAsDouble() {
        // Non-existent key returns default
        Double value = this.aiConfigService.getConfigValueAsDouble("nonexistent.double", 3.14);
        assertEquals(3.14, value);

        // Create a numeric config
        final var entity = new AiConfigEntity("test.double", "2.71", ConfigType.DOUBLE, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        value = this.aiConfigService.getConfigValueAsDouble("test.double", 0.0);
        assertEquals(2.71, value);

        // Invalid double format returns default
        final var invalidEntity =
                new AiConfigEntity("test.invalid.double", "not_a_double", ConfigType.DOUBLE, ConfigCategory.GENERAL);
        invalidEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(invalidEntity);

        value = this.aiConfigService.getConfigValueAsDouble("test.invalid.double", 1.23);
        assertEquals(1.23, value);
    }

    @Test
    @DisplayName("Get configuration value as Boolean with various formats")
    @TestTransaction
    void testGetConfigValueAsBoolean() {
        // Non-existent key returns default
        Boolean value = this.aiConfigService.getConfigValueAsBoolean("nonexistent.bool", false);
        assertEquals(false, value);

        // Test "true" string
        final var trueEntity = new AiConfigEntity("test.bool.true", "true", ConfigType.BOOLEAN, ConfigCategory.GENERAL);
        trueEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(trueEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.true", false);
        assertEquals(true, value);

        // Test "false" string
        final var falseEntity =
                new AiConfigEntity("test.bool.false", "false", ConfigType.BOOLEAN, ConfigCategory.GENERAL);
        falseEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(falseEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.false", true);
        assertEquals(false, value);

        // Test "1" for true
        final var oneEntity = new AiConfigEntity("test.bool.one", "1", ConfigType.BOOLEAN, ConfigCategory.GENERAL);
        oneEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(oneEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.one", false);
        assertEquals(true, value);

        // Test "0" for false
        final var zeroEntity = new AiConfigEntity("test.bool.zero", "0", ConfigType.BOOLEAN, ConfigCategory.GENERAL);
        zeroEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(zeroEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.zero", true);
        assertEquals(false, value);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Update single configuration value")
    @TestTransaction
    void testUpdateConfig() {
        // Create initial config
        final var entity =
                new AiConfigEntity("update.test", "initial_value", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Update it
        this.aiConfigService.updateConfig("update.test", "updated_value", this.credentials);

        // Verify update
        final var updated = this.aiConfigService.getConfigValue("update.test", "NOT_FOUND");
        assertEquals("updated_value", updated);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Update multiple configurations at once")
    @TestTransaction
    void testUpdateMultipleConfigs() {
        // Create initial configs
        final var config1 = new AiConfigEntity("batch.config1", "value1", ConfigType.STRING, ConfigCategory.GENERAL);
        config1.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config1);

        final var config2 = new AiConfigEntity("batch.config2", "value2", ConfigType.STRING, ConfigCategory.GENERAL);
        config2.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config2);

        // Update multiple
        final var updates = List.of(new AiConfigUpdateDto("batch.config1", "new_value1"),
                new AiConfigUpdateDto("batch.config2", "new_value2"),
                new AiConfigUpdateDto("batch.config3", "new_value3") // New config
        );

        this.aiConfigService.updateMultipleConfigs(updates, this.credentials);

        // Verify all updates
        assertEquals("new_value1", this.aiConfigService.getConfigValue("batch.config1", "NOT_FOUND"));
        assertEquals("new_value2", this.aiConfigService.getConfigValue("batch.config2", "NOT_FOUND"));
        assertEquals("new_value3", this.aiConfigService.getConfigValue("batch.config3", "NOT_FOUND"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Type validation - INTEGER type")
    @TestTransaction
    void testValidateIntegerType() {
        // Create an INTEGER type config (marked as optional so empty values are
        // allowed)
        final var entity =
                new AiConfigEntity("test.integer", "100", ConfigType.INTEGER, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid integer update
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.integer", "200", this.credentials));

        // Invalid integer (not a number)
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.integer", "not_a_number", this.credentials));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.integer", "", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Type validation - DOUBLE type")
    @TestTransaction
    void testValidateDoubleType() {
        // Create a DOUBLE type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.double", "3.14", ConfigType.DOUBLE, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid double update
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.double", "2.71", this.credentials));

        // Invalid double (not a number)
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.double", "not_a_double", this.credentials));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.double", "", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Type validation - BOOLEAN type")
    @TestTransaction
    void testValidateBooleanType() {
        // Create a BOOLEAN type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.boolean", "true", ConfigType.BOOLEAN, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid boolean values
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "true", this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "false", this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "1", this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "0", this.credentials));

        // Invalid boolean value
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.boolean", "maybe", this.credentials));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Type validation - STRING type accepts anything")
    @TestTransaction
    void testValidateStringType() {
        // Create a STRING type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.string", "value", ConfigType.STRING, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // STRING type accepts any value (no format constraints)
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "anything goes!", this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "123", this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "true", this.credentials));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Cache invalidation on update")
    @TestTransaction
    void testCacheInvalidation() {
        // Create a config
        final var entity = new AiConfigEntity("cache.test", "original", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Get value (should cache it)
        String value = this.aiConfigService.getConfigValue("cache.test", "NOT_FOUND");
        assertEquals("original", value);

        // Update the config
        this.aiConfigService.updateConfig("cache.test", "updated", this.credentials);

        // Get value again (should reflect updated value due to cache invalidation)
        value = this.aiConfigService.getConfigValue("cache.test", "NOT_FOUND");
        assertEquals("updated", value);
    }

    @Test
    @DisplayName("Clear cache method")
    @TestTransaction
    void testClearCache() {
        // Create a config and cache it
        final var entity = new AiConfigEntity("cache.clear.test", "value", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        String value = this.aiConfigService.getConfigValue("cache.clear.test", "NOT_FOUND");
        assertEquals("value", value);

        // Clear cache
        this.aiConfigService.clearCache();

        // Retrieve again (should still work, just hits DB)
        value = this.aiConfigService.getConfigValue("cache.clear.test", "NOT_FOUND");
        assertEquals("value", value);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Null key validation")
    @TestTransaction
    void testNullValidation() {
        // Null key must throw exception
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig(null, "value", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Range validation - temperature out of bounds")
    @TestTransaction
    void testValidateTemperatureRange() {
        final var entity =
                new AiConfigEntity("test.temperature", "0.5", ConfigType.DOUBLE, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid temperature
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.temperature", "1.5", this.credentials));

        // Too high
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.temperature", "3.0", this.credentials));

        // Too low
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.temperature", "-0.5", this.credentials));
    }

    @Test
    @DisplayName("getClampedTemperature clamps values to [0.0, 2.0]")
    @TestTransaction
    void testGetClampedTemperature() {
        double result = this.aiConfigService.getClampedTemperature("nonexistent.key", 0.5);
        assertEquals(0.5, result);

        result = this.aiConfigService.getClampedTemperature("nonexistent.key", 3.0);
        assertEquals(2.0, result, "Should clamp to 2.0");

        result = this.aiConfigService.getClampedTemperature("nonexistent.key", -1.0);
        assertEquals(0.0, result, "Should clamp to 0.0");
    }

    @Test
    @DisplayName("getClampedTokens clamps values to [1, 8192]")
    @TestTransaction
    void testGetClampedTokens() {
        int result = this.aiConfigService.getClampedTokens("nonexistent.key", 1000);
        assertEquals(1000, result);

        result = this.aiConfigService.getClampedTokens("nonexistent.key", 10_000);
        assertEquals(8192, result, "Should clamp to 8192");

        result = this.aiConfigService.getClampedTokens("nonexistent.key", 0);
        assertEquals(1, result, "Should clamp to 1");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("getAllConfigsByCategory with null returns empty map")
    @TestTransaction
    void testGetAllConfigsByCategory_null() {
        final var result = this.aiConfigService.getAllConfigsByCategory(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllConfigs returns non-null list")
    @TestTransaction
    void testGetAllConfigs() {
        final var configs = this.aiConfigService.getAllConfigs();
        assertNotNull(configs);
        assertFalse(configs.isEmpty(), "Should have at least the seeded configs");
    }

    @Test
    @DisplayName("getConfigsByCategory returns configs in that category")
    @TestTransaction
    void testGetConfigsByCategory() {
        final var configs = this.aiConfigService.getConfigsByCategory(ConfigCategory.GENERAL);
        assertNotNull(configs);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateMultipleConfigs with null/empty list does nothing")
    @TestTransaction
    void testUpdateMultipleConfigs_nullOrEmpty() {
        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(null, this.credentials));
        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(List.of(), this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("resetToDefaults restores factory defaults")
    @TestTransaction
    void testResetToDefaults() {
        this.aiConfigService.updateConfig(AiConfigKeys.AI_TUTOR_ENABLED, "false", this.credentials);
        assertEquals("false", this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, ""));

        this.aiConfigService.resetToDefaults(this.credentials);

        assertEquals("true", this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, ""));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("getConfigValue with null key returns default")
    @TestTransaction
    void testGetConfigValue_nullKey() {
        assertEquals("default", this.aiConfigService.getConfigValue(null, "default"));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("validateConfigValue - empty value on non-optional config throws")
    @TestTransaction
    void testValidateConfigValue_nonOptionalEmpty() {
        final var entity =
                new AiConfigEntity("test.nonoptional", "value", ConfigType.STRING, ConfigCategory.GENERAL, null, false);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.nonoptional", "", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("URL validation - unresolvable hostname allowed for Ollama ONLY if in allow-list")
    @TestTransaction
    void testUrlValidationUnresolvableHostOllamaAllowlist() {
        // 'ollama' is in the default allow-list
        assertDoesNotThrow(
                () -> this.aiConfigService.updateConfig("ollama.base-url", "http://ollama:11434/", this.credentials));

        // 'nonexistent-host' is NOT in the allow-list and is rejected without being resolved
        assertThrows(ValidationException.class, () -> this.aiConfigService.updateConfig("ollama.base-url",
                "http://nonexistent-host.invalid/", this.credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("URL validation - host outside the provider allow-list rejected for non-Ollama providers")
    @TestTransaction
    void testUrlValidationUnlistedHostRejectedForNonOllama() {
        assertThrows(ValidationException.class, () -> this.aiConfigService.updateConfig("google.base-url",
                "https://nonexistent-google-host.invalid/", this.credentials));
    }

    @Test
    @DisplayName("validateProviderApiUrl - rejects blank URL")
    void testValidateProviderApiUrlRejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("", AiConfigService.ProviderType.OLLAMA));
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl(null, AiConfigService.ProviderType.GOOGLE));
    }

    @Test
    @DisplayName("validateProviderApiUrl - rejects URL without a host")
    void testValidateProviderApiUrlRejectsHostlessUri() {
        // file:/// has no host; URI.create accepts it
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("file:///etc/passwd", AiConfigService.ProviderType.OLLAMA));
    }

    @Test
    @DisplayName("validateProviderApiUrl - allowlisted Ollama host passes")
    void testValidateProviderApiUrlOllamaAllowed() {
        assertDoesNotThrow(() -> this.aiConfigService.validateProviderApiUrl("http://ollama:11434",
                AiConfigService.ProviderType.OLLAMA));
        assertDoesNotThrow(() -> this.aiConfigService.validateProviderApiUrl("http://localhost:11434",
                AiConfigService.ProviderType.OLLAMA));
    }

    @Test
    @DisplayName("validateProviderApiUrl - non-allowlisted host rejected for every provider")
    void testValidateProviderApiUrlRejectsUnknownHost() {
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("http://evil.example.com", AiConfigService.ProviderType.OLLAMA));
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("https://evil.example.com", AiConfigService.ProviderType.GOOGLE));
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("https://evil.example.com", AiConfigService.ProviderType.OPENAI));
    }

    @Test
    @DisplayName("validateProviderApiUrl - HTTPS required for external providers")
    void testValidateProviderApiUrlExternalRequiresHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("http://generativelanguage.googleapis.com",
                        AiConfigService.ProviderType.GOOGLE));
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("http://api.openai.com", AiConfigService.ProviderType.OPENAI));
    }

    @Test
    @DisplayName("validateProviderApiUrl - malformed URI rejected")
    void testValidateProviderApiUrlMalformedUri() {
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("not a url", AiConfigService.ProviderType.OLLAMA));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateConfig - non-provider URL key falls through to generic loopback check")
    @TestTransaction
    void testUpdateConfigGenericUrlKeyRejectsLoopback() {
        // 'custom.url' does not match any provider keyword, so the generic-URL fallback is exercised.
        final var ex = assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("custom.url", "https://127.0.0.1:8080/", this.credentials));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("Loopback addresses are not allowed"),
                ex.getMessage());
    }

    @Test
    @DisplayName("validateProviderApiUrl - SSRF bypass attempts are rejected")
    void testValidateProviderApiUrlSsrfBypassAttempts() {
        // Mixed-case host (should be normalized and checked against allowlist)
        assertDoesNotThrow(() -> this.aiConfigService.validateProviderApiUrl(
                "https://GENERATIVELANGUAGE.googleapis.com", AiConfigService.ProviderType.GOOGLE));

        // URL-encoded host
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("https://generativelanguage%2Egoogleapis%2Ecom",
                        AiConfigService.ProviderType.GOOGLE));

        // Host with trailing dot (FQDN) - should be normalized/handled
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("https://generativelanguage.googleapis.com.",
                        AiConfigService.ProviderType.GOOGLE));

        // IPv6 address as host for external provider
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService.validateProviderApiUrl("https://[::1]",
                AiConfigService.ProviderType.GOOGLE));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Range validation - max-tokens out of bounds")
    @TestTransaction
    void testValidateMaxTokensRange() {
        final var entity =
                new AiConfigEntity("test.max-tokens", "1000", ConfigType.INTEGER, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid max-tokens
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.max-tokens", "4096", this.credentials));

        // Too high
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.max-tokens", "10000", this.credentials));

        // Too low
        assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("test.max-tokens", "0", this.credentials));
    }
}
