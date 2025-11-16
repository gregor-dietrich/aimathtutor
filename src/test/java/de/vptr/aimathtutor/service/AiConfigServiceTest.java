package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * Unit tests for AiConfigService.
 * Tests configuration CRUD operations, type conversions, validation, and
 * caching.
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

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any test configuration records from previous test runs
        // This ensures tests are isolated and don't fail on subsequent runs
        final var testKeys = List.of(
                "nonexistent.key", "test.key", "nonexistent.int", "test.int", "test.invalid.int",
                "nonexistent.double", "test.double", "test.invalid.double", "nonexistent.bool",
                "test.bool.true", "test.bool.false", "test.bool.one", "test.bool.zero",
                "test.config1", "test.config2", "test.config3", "update.test",
                "batch.config1", "batch.config2", "batch.config3",
                "cache.test", "cache.clear.test", "test.integer", "test.boolean", "test.string");
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

        this.adminUser = new UserEntity();
        this.adminUser.id = 1L;
        this.adminUser.username = "testadmin";
        this.adminUser.rank = adminRank;

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
        final var entity = new AiConfigEntity("test.key", "test.value", "STRING", "TEST");
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
        final var entity = new AiConfigEntity("test.int", "123", "INTEGER", "TEST");
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        value = this.aiConfigService.getConfigValueAsInt("test.int", 0);
        assertEquals(123, value);

        // Invalid integer format returns default
        final var invalidEntity = new AiConfigEntity("test.invalid.int", "not_a_number", "INTEGER", "TEST");
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
        final var entity = new AiConfigEntity("test.double", "2.71", "DOUBLE", "TEST");
        entity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(entity);

        value = this.aiConfigService.getConfigValueAsDouble("test.double", 0.0);
        assertEquals(2.71, value);

        // Invalid double format returns default
        final var invalidEntity = new AiConfigEntity("test.invalid.double", "not_a_double", "DOUBLE", "TEST");
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
        final var trueEntity = new AiConfigEntity("test.bool.true", "true", "BOOLEAN", "TEST");
        trueEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(trueEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.true", false);
        assertEquals(true, value);

        // Test "false" string
        final var falseEntity = new AiConfigEntity("test.bool.false", "false", "BOOLEAN", "TEST");
        falseEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(falseEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.false", true);
        assertEquals(false, value);

        // Test "1" for true
        final var oneEntity = new AiConfigEntity("test.bool.one", "1", "BOOLEAN", "TEST");
        oneEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(oneEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.one", false);
        assertEquals(true, value);

        // Test "0" for false
        final var zeroEntity = new AiConfigEntity("test.bool.zero", "0", "BOOLEAN", "TEST");
        zeroEntity.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(zeroEntity);
        value = this.aiConfigService.getConfigValueAsBoolean("test.bool.zero", true);
        assertEquals(false, value);
    }

    @Test
    @DisplayName("Get all configurations by category")
    @Transactional
    void testGetAllConfigsByCategory() {
        // Create multiple configs in same category
        final var config1 = new AiConfigEntity("test.config1", "value1", "STRING", "TESTCAT");
        config1.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config1);

        final var config2 = new AiConfigEntity("test.config2", "value2", "STRING", "TESTCAT");
        config2.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config2);

        final var config3 = new AiConfigEntity("test.config3", "value3", "STRING", "OTHERCAT");
        config3.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config3);

        // Retrieve by category
        final var testcatConfigs = this.aiConfigService.getAllConfigsByCategory("TESTCAT");
        assertEquals(2, testcatConfigs.size());
        assertTrue(testcatConfigs.containsKey("test.config1"));
        assertTrue(testcatConfigs.containsKey("test.config2"));
        assertEquals("value1", testcatConfigs.get("test.config1"));
        assertEquals("value2", testcatConfigs.get("test.config2"));

        final var othercatConfigs = this.aiConfigService.getAllConfigsByCategory("OTHERCAT");
        assertEquals(1, othercatConfigs.size());
        assertTrue(othercatConfigs.containsKey("test.config3"));
    }

    @Test
    @DisplayName("Update single configuration value")
    @Transactional
    void testUpdateConfig() {
        // Create initial config
        final var entity = new AiConfigEntity("update.test", "initial_value", "STRING", "TEST");
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
        final var config1 = new AiConfigEntity("batch.config1", "value1", "STRING", "BATCH");
        config1.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config1);

        final var config2 = new AiConfigEntity("batch.config2", "value2", "STRING", "BATCH");
        config2.lastUpdatedBy = this.adminUser;
        this.aiConfigRepository.persist(config2);

        // Update multiple
        final var updates = List.of(
                new AiConfigUpdateDto("batch.config1", "new_value1"),
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
        final var entity = new AiConfigEntity("test.integer", "100", "INTEGER", "TEST", null, true);
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
        final var entity = new AiConfigEntity("test.double", "3.14", "DOUBLE", "TEST", null, true);
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
        final var entity = new AiConfigEntity("test.boolean", "true", "BOOLEAN", "TEST", null, true);
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
        final var entity = new AiConfigEntity("test.string", "value", "STRING", "TEST", null, true);
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
        final var entity = new AiConfigEntity("cache.test", "original", "STRING", "TEST");
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
        final var entity = new AiConfigEntity("cache.clear.test", "value", "STRING", "TEST");
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
}
