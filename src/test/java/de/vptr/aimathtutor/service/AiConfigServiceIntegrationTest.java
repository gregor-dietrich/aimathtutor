package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for AiConfigService using real database and seeded data.
 */
@QuarkusTest
@DisplayName("AiConfigService Integration Tests")
class AiConfigServiceIntegrationTest {

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    private static final Long ADMIN_USER_ID = 1L;
    private static final String TEST_KEY = "it.test.config";
    private String originalTemperature;

    @BeforeEach
    @Transactional
    void setUp() {
        // Ensure admin user exists (seeded in init.sql)
        final UserEntity admin = this.userRepository.findById(ADMIN_USER_ID);
        assertNotNull(admin, "Admin user should exist in test database");

        // Save original value so we can restore it after tests
        this.originalTemperature = this.aiConfigService.getConfigValue("gemini.temperature", "0.7");

        // Clean up any leftover test key from previous interrupted runs
        final var existing = this.aiConfigRepository.findByConfigKey(TEST_KEY);
        existing.ifPresent(e -> this.aiConfigRepository.deleteById(e.id));
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Restore gemini.temperature to its original value
        if (this.originalTemperature != null) {
            this.aiConfigService.updateConfig("gemini.temperature", this.originalTemperature, ADMIN_USER_ID);
        }

        // Remove test key if it still exists
        final var existing = this.aiConfigRepository.findByConfigKey(TEST_KEY);
        existing.ifPresent(e -> this.aiConfigRepository.deleteById(e.id));
    }

    @Test
    @DisplayName("Read seeded string config value")
    @Transactional
    void testGetConfigValueFromSeed() {
        final String value = this.aiConfigService.getConfigValue("ai.tutor.enabled", "false");
        assertEquals("true", value);
    }

    @Test
    @DisplayName("Read seeded integer config value")
    @Transactional
    void testGetConfigValueAsIntFromSeed() {
        final Integer value = this.aiConfigService.getConfigValueAsInt("gemini.max-tokens", 0);
        assertEquals(2000, value);
    }

    @Test
    @DisplayName("Read seeded double config value")
    @Transactional
    void testGetConfigValueAsDoubleFromSeed() {
        final Double value = this.aiConfigService.getConfigValueAsDouble("gemini.temperature", 0.0);
        assertEquals(0.7, value);
    }

    @Test
    @DisplayName("Read seeded boolean config value")
    @Transactional
    void testGetConfigValueAsBooleanFromSeed() {
        final Boolean value = this.aiConfigService.getConfigValueAsBoolean("ai.tutor.enabled", false);
        assertTrue(value);
    }

    @Test
    @DisplayName("Update config and read back updated value")
    @Transactional
    void testUpdateConfigAndReadBack() {
        // Create a test config
        final var entity = new AiConfigEntity(TEST_KEY, "initial", "STRING", "TEST");
        entity.lastUpdatedBy = this.userRepository.findById(ADMIN_USER_ID);
        this.aiConfigRepository.persist(entity);

        // Update via service
        this.aiConfigService.updateConfig(TEST_KEY, "updated", ADMIN_USER_ID);

        // Read back
        final String value = this.aiConfigService.getConfigValue(TEST_KEY, "NOT_FOUND");
        assertEquals("updated", value);
    }

    @Test
    @DisplayName("Get all configs by category using seeded data")
    @Transactional
    void testGetAllConfigsByCategory() {
        final Map<String, String> geminiConfigs = this.aiConfigService.getAllConfigsByCategory("GEMINI");
        assertFalse(geminiConfigs.isEmpty());
        assertTrue(geminiConfigs.containsKey("gemini.model"));
        assertEquals("gemma-3-27b-it", geminiConfigs.get("gemini.model"));
    }

    @Test
    @DisplayName("Reset to defaults restores seeded values")
    @Transactional
    void testResetToDefaults() {
        // Change a seeded value away from default
        this.aiConfigService.updateConfig("gemini.temperature", "1.5", ADMIN_USER_ID);
        assertEquals("1.5", this.aiConfigService.getConfigValue("gemini.temperature", ""));

        // Reset all to defaults
        this.aiConfigService.resetToDefaults(ADMIN_USER_ID);

        // Verify restored
        final String restored = this.aiConfigService.getConfigValue("gemini.temperature", "");
        assertEquals("0.7", restored);
    }

    @Test
    @DisplayName("Cache invalidation after update reflects new value immediately")
    @Transactional
    void testCacheInvalidation() {
        // Create test config
        final var entity = new AiConfigEntity(TEST_KEY, "v1", "STRING", "TEST");
        entity.lastUpdatedBy = this.userRepository.findById(ADMIN_USER_ID);
        this.aiConfigRepository.persist(entity);

        // Read to populate cache
        assertEquals("v1", this.aiConfigService.getConfigValue(TEST_KEY, ""));

        // Update via service (should invalidate cache)
        this.aiConfigService.updateConfig(TEST_KEY, "v2", ADMIN_USER_ID);

        // Read again — should see updated value without stale cache
        assertEquals("v2", this.aiConfigService.getConfigValue(TEST_KEY, ""));
    }

    @Test
    @DisplayName("Validation rejects out-of-range max-tokens")
    @Transactional
    void testValidationRejectsOutOfRangeMaxTokens() {
        assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.updateConfig("gemini.max-tokens", "99999", ADMIN_USER_ID));
    }

    @Test
    @DisplayName("Validation accepts in-range temperature")
    @Transactional
    void testValidationAcceptsInRangeTemperature() {
        assertDoesNotThrow(
                () -> this.aiConfigService.updateConfig("gemini.temperature", "1.2", ADMIN_USER_ID));

        // Restore
        this.aiConfigService.updateConfig("gemini.temperature", this.originalTemperature, ADMIN_USER_ID);
    }

    @Test
    @DisplayName("Permission check rejects non-admin user")
    @Transactional
    void testPermissionCheckRejectsNonAdmin() {
        // student1 has id=3 and rank=Student (no exercise/lesson permissions)
        final Long studentId = 3L;
        final var student = this.userRepository.findById(studentId);
        assertNotNull(student, "Student user should exist");

        assertThrows(IllegalStateException.class,
                () -> this.aiConfigService.updateConfig("gemini.temperature", "0.5", studentId));
    }
}
