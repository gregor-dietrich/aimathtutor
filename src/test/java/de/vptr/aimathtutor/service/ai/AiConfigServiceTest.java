package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Unit tests for AiConfigService. Tests configuration CRUD operations, type conversions, validation, and caching.
 */
@QuarkusTest
@DisplayName("AiConfigService Tests")
@SuppressWarnings("NullAway")
class AiConfigServiceTest {

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    private UserEntity adminUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any test configuration records from previous test runs
        // This ensures tests are isolated and don't fail on subsequent runs
        final var testKeys = List.of("nonexistent.key", "test.key", "nonexistent.int", "test.int", "test.invalid.int",
                "nonexistent.double", "test.double", "test.invalid.double", "nonexistent.bool", "test.bool.true",
                "test.bool.false", "test.bool.one", "test.bool.zero", "test.config1", "test.config2", "test.config3",
                "update.test", "batch.config1", "batch.config2", "batch.config3", "cache.test", "cache.clear.test",
                "test.integer", "test.boolean", "test.string", "test.temperature", "test.max-tokens", "test.config3",
                "ollama.base-url", "google.base-url");
        for (final var key : testKeys) {
            final var entity = this.aiConfigRepository.findByConfigKey(key);
            if (entity.isPresent()) {
                this.aiConfigRepository.deleteById(entity.get().id);
            }
        }

        // Setup: Get or create admin user for testing
        final var adminRank = new UserRankEntity();
        adminRank.id = 1L;
        adminRank.name = "Admin";
        adminRank.version = 0L;

        this.adminUser = new UserEntity();
        this.adminUser.id = 1L;
        this.adminUser.username = "testadmin";
        this.adminUser.rank = adminRank;
        this.adminUser.version = 0L;

        // Ensure admin user exists
        final var existing = this.userRepository.findById(1L);
        if (existing == null) {
            this.userRepository.persist(this.adminUser);
        } else {
            this.adminUser = existing;
        }
    }

    @Test
    @DisplayName("Get configuration value as String with default fallback")
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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

    @Test
    @DisplayName("Update single configuration value")
    @Transactional
    void testUpdateConfig() {
        // Create initial config
        final var entity =
                new AiConfigEntity("update.test", "initial_value", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Update it
        this.aiConfigService.updateConfig("update.test", "updated_value", this.adminUser.id);

        // Verify update
        final var updated = this.aiConfigService.getConfigValue("update.test", "NOT_FOUND");
        assertEquals("updated_value", updated);
    }

    @Test
    @DisplayName("Update multiple configurations at once")
    @Transactional
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

        this.aiConfigService.updateMultipleConfigs(updates, this.adminUser.id);

        // Verify all updates
        assertEquals("new_value1", this.aiConfigService.getConfigValue("batch.config1", "NOT_FOUND"));
        assertEquals("new_value2", this.aiConfigService.getConfigValue("batch.config2", "NOT_FOUND"));
        assertEquals("new_value3", this.aiConfigService.getConfigValue("batch.config3", "NOT_FOUND"));
    }

    @Test
    @DisplayName("Type validation - INTEGER type")
    @Transactional
    void testValidateIntegerType() {
        // Create an INTEGER type config (marked as optional so empty values are
        // allowed)
        final var entity =
                new AiConfigEntity("test.integer", "100", ConfigType.INTEGER, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid integer update
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.integer", "200", this.adminUser.id));

        // Invalid integer (not a number)
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.integer", "not_a_number", this.adminUser.id));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.integer", "", this.adminUser.id));
    }

    @Test
    @DisplayName("Type validation - DOUBLE type")
    @Transactional
    void testValidateDoubleType() {
        // Create a DOUBLE type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.double", "3.14", ConfigType.DOUBLE, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid double update
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.double", "2.71", this.adminUser.id));

        // Invalid double (not a number)
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.double", "not_a_double", this.adminUser.id));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.double", "", this.adminUser.id));
    }

    @Test
    @DisplayName("Type validation - BOOLEAN type")
    @Transactional
    void testValidateBooleanType() {
        // Create a BOOLEAN type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.boolean", "true", ConfigType.BOOLEAN, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid boolean values
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "true", this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "false", this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "1", this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "0", this.adminUser.id));

        // Invalid boolean value
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.boolean", "maybe", this.adminUser.id));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.boolean", "", this.adminUser.id));
    }

    @Test
    @DisplayName("Type validation - STRING type accepts anything")
    @Transactional
    void testValidateStringType() {
        // Create a STRING type config (marked as optional so empty values are allowed)
        final var entity =
                new AiConfigEntity("test.string", "value", ConfigType.STRING, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // STRING type accepts any value (no format constraints)
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "anything goes!", this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "123", this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "true", this.adminUser.id));

        // Empty value is allowed
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.string", "", this.adminUser.id));
    }

    @Test
    @DisplayName("Cache invalidation on update")
    @Transactional
    void testCacheInvalidation() {
        // Create a config
        final var entity = new AiConfigEntity("cache.test", "original", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Get value (should cache it)
        String value = this.aiConfigService.getConfigValue("cache.test", "NOT_FOUND");
        assertEquals("original", value);

        // Update the config
        this.aiConfigService.updateConfig("cache.test", "updated", this.adminUser.id);

        // Get value again (should reflect updated value due to cache invalidation)
        value = this.aiConfigService.getConfigValue("cache.test", "NOT_FOUND");
        assertEquals("updated", value);
    }

    @Test
    @DisplayName("Clear cache method")
    @Transactional
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

    @Test
    @DisplayName("Null key validation")
    @Transactional
    void testNullValidation() {
        // Null key must throw exception
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig(null, "value", this.adminUser.id));
    }

    @Test
    @DisplayName("Range validation - temperature out of bounds")
    @Transactional
    void testValidateTemperatureRange() {
        final var entity =
                new AiConfigEntity("test.temperature", "0.5", ConfigType.DOUBLE, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid temperature
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.temperature", "1.5", this.adminUser.id));

        // Too high
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.temperature", "3.0", this.adminUser.id));

        // Too low
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.temperature", "-0.5", this.adminUser.id));
    }

    @Test
    @DisplayName("getClampedTemperature clamps values to [0.0, 2.0]")
    @Transactional
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
    @Transactional
    void testGetClampedTokens() {
        int result = this.aiConfigService.getClampedTokens("nonexistent.key", 1000);
        assertEquals(1000, result);

        result = this.aiConfigService.getClampedTokens("nonexistent.key", 10_000);
        assertEquals(8192, result, "Should clamp to 8192");

        result = this.aiConfigService.getClampedTokens("nonexistent.key", 0);
        assertEquals(1, result, "Should clamp to 1");
    }

    @Test
    @DisplayName("getAllConfigsByCategory with null returns empty map")
    @Transactional
    void testGetAllConfigsByCategory_null() {
        final var result = this.aiConfigService.getAllConfigsByCategory(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllConfigs returns non-null list")
    @Transactional
    void testGetAllConfigs() {
        final var configs = this.aiConfigService.getAllConfigs();
        assertNotNull(configs);
        assertFalse(configs.isEmpty(), "Should have at least the seeded configs");
    }

    @Test
    @DisplayName("getConfigsByCategory returns configs in that category")
    @Transactional
    void testGetConfigsByCategory() {
        final var configs = this.aiConfigService.getConfigsByCategory(ConfigCategory.GENERAL);
        assertNotNull(configs);
    }

    @Test
    @DisplayName("updateMultipleConfigs with null/empty list does nothing")
    @Transactional
    void testUpdateMultipleConfigs_nullOrEmpty() {
        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(null, this.adminUser.id));
        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(List.of(), this.adminUser.id));
    }

    @Test
    @DisplayName("resetToDefaults restores factory defaults")
    @Transactional
    void testResetToDefaults() {
        this.aiConfigService.updateConfig(AiConfigKeys.AI_TUTOR_ENABLED, "false", this.adminUser.id);
        assertEquals("false", this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, ""));

        this.aiConfigService.resetToDefaults(this.adminUser.id);

        assertEquals("true", this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, ""));
    }

    @Test
    @DisplayName("getConfigValue with null key returns default")
    @Transactional
    void testGetConfigValue_nullKey() {
        assertEquals("default", this.aiConfigService.getConfigValue(null, "default"));
    }

    @Test
    @DisplayName("validateConfigValue - empty value on non-optional config throws")
    @Transactional
    void testValidateConfigValue_nonOptionalEmpty() {
        final var entity =
                new AiConfigEntity("test.nonoptional", "value", ConfigType.STRING, ConfigCategory.GENERAL, null, false);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.nonoptional", "", this.adminUser.id));
    }

    @Test
    @DisplayName("validateConfigValue - configKey with null key throws")
    @Transactional
    void testUpdateConfig_nullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig(null, "value", this.adminUser.id));
    }

    @Test
    @DisplayName("URL validation - unresolvable hostname allowed for Ollama ONLY if in allow-list")
    @Transactional
    void testUrlValidationUnresolvableHostOllamaAllowlist() {
        // 'ollama' is in the default allow-list
        assertDoesNotThrow(
                () -> this.aiConfigService.updateConfig("ollama.base-url", "http://ollama:11434/", this.adminUser.id));

        // 'nonexistent-host' is NOT in the allow-list and won't resolve
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService.updateConfig("ollama.base-url",
                "http://nonexistent-host.invalid/", this.adminUser.id));
    }

    @Test
    @DisplayName("URL validation - unresolvable hostname rejected for non-Ollama providers")
    @Transactional
    void testUrlValidationUnresolvableHostRejectedForNonOllama() {
        assertThrows(IllegalArgumentException.class, () -> this.aiConfigService.updateConfig("google.base-url",
                "https://nonexistent-google-host.invalid/", this.adminUser.id));
    }

    @Test
    @DisplayName("Range validation - max-tokens out of bounds")
    @Transactional
    void testValidateMaxTokensRange() {
        final var entity =
                new AiConfigEntity("test.max-tokens", "1000", ConfigType.INTEGER, ConfigCategory.GENERAL, null, true);
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        // Valid max-tokens
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("test.max-tokens", "4096", this.adminUser.id));

        // Too high
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.max-tokens", "10000", this.adminUser.id));

        // Too low
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("test.max-tokens", "0", this.adminUser.id));
    }
}
