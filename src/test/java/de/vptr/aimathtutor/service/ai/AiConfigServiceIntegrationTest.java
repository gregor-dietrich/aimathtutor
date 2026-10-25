package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

@QuarkusTest
class AiConfigServiceIntegrationTest {

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    @InjectMock
    HostResolver hostResolver;

    private static final String ADMIN_USERNAME = "admin";
    private static final String TEST_KEY = "it.test.config";

    @SuppressWarnings("NullAway")
    private SessionCredentials credentialsFor(final String username) {
        final var user = this.userRepository.findByUsername(username);
        return new SessionCredentials(user.publicId, AuthService.credentialStamp(user.password),
                UUID.randomUUID().toString());
    }

    @BeforeEach
    void setUp() throws UnknownHostException {
        this.aiConfigService.clearCache();
        HostResolverStubs.resolveEverythingPublic(this.hostResolver);

        // Ensure admin user exists (seeded by V1__baseline.sql)
        final UserEntity admin = this.userRepository.findByUsername(ADMIN_USERNAME);
        assertNotNull(admin, "Admin user should exist in test database");
    }

    @AfterEach
    void tearDown() {
        this.aiConfigService.clearCache();
    }

    @Test
    @DisplayName("Read seeded integer config value")
    @TestTransaction
    void testGetConfigValueAsIntFromSeed() {
        final Integer value = this.aiConfigService.getConfigValueAsInt("google.max-tokens", 0);
        assertEquals(2000, value);
    }

    @Test
    @DisplayName("Read seeded double config value")
    @TestTransaction
    void testGetConfigValueAsDoubleFromSeed() {
        final Double value = this.aiConfigService.getConfigValueAsDouble("google.temperature", 0.0);
        assertEquals(0.7, value);
    }

    @Test
    @DisplayName("Read seeded boolean config value")
    @TestTransaction
    void testGetConfigValueAsBooleanFromSeed() {
        final Boolean value = this.aiConfigService.getConfigValueAsBoolean("ai.tutor.enabled", false);
        assertTrue(value);
    }

    @Test
    @DisplayName("Update config and read back updated value")
    @TestTransaction
    void testUpdateConfigAndReadBack() {
        // Create a test config
        final var entity = new AiConfigEntity(TEST_KEY, "initial", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.userRepository.findByUsername(ADMIN_USERNAME);
        this.aiConfigRepository.persist(entity);

        // Update via service
        this.aiConfigService.updateConfig(TEST_KEY, "updated", this.credentialsFor(ADMIN_USERNAME));

        // Read back
        final String value = this.aiConfigService.getConfigValue(TEST_KEY, "NOT_FOUND");
        assertEquals("updated", value);
    }

    @Test
    @DisplayName("Get all configs by category using seeded data")
    @TestTransaction
    void testGetAllConfigsByCategory() {
        final Map<String, String> googleConfigs = this.aiConfigService.getAllConfigsByCategory(ConfigCategory.GOOGLE);
        assertFalse(googleConfigs.isEmpty());
        assertTrue(googleConfigs.containsKey("google.model"));
        assertEquals("gemini-3.1-flash-lite", googleConfigs.get("google.model"));
    }

    @Test
    @DisplayName("Reset to defaults restores seeded values")
    @TestTransaction
    void testResetToDefaults() {
        // Change a seeded value away from default
        this.aiConfigService.updateConfig("google.temperature", "1.5", this.credentialsFor(ADMIN_USERNAME));
        assertEquals("1.5", this.aiConfigService.getConfigValue("google.temperature", ""));

        // Reset all to defaults
        this.aiConfigService.resetToDefaults(this.credentialsFor(ADMIN_USERNAME));

        // Verify restored
        final String restored = this.aiConfigService.getConfigValue("google.temperature", "");
        assertEquals("0.7", restored);
    }

    @Test
    @DisplayName("Cache invalidation after update reflects new value immediately")
    @TestTransaction
    void testCacheInvalidation() {
        // Create test config
        final var entity = new AiConfigEntity(TEST_KEY, "v1", ConfigType.STRING, ConfigCategory.GENERAL);
        entity.lastUpdatedBy = this.userRepository.findByUsername(ADMIN_USERNAME);
        this.aiConfigRepository.persist(entity);

        // Read to populate cache
        assertEquals("v1", this.aiConfigService.getConfigValue(TEST_KEY, ""));

        // Update via service (should invalidate cache)
        this.aiConfigService.updateConfig(TEST_KEY, "v2", this.credentialsFor(ADMIN_USERNAME));

        // Read again — should see updated value without stale cache
        assertEquals("v2", this.aiConfigService.getConfigValue(TEST_KEY, ""));
    }

    @Test
    @DisplayName("Validation rejects out-of-range max-tokens")
    @TestTransaction
    void testValidationRejectsOutOfRangeMaxTokens() {
        assertThrows(ValidationException.class, () -> this.aiConfigService.updateConfig("google.max-tokens", "99999",
                this.credentialsFor(ADMIN_USERNAME)));
    }

    @Test
    @DisplayName("Validation failure is a ValidationException that keeps the validation message")
    @TestTransaction
    void testValidationFailureKeepsMessage() {
        final var ex = assertThrows(ValidationException.class, () -> this.aiConfigService
                .updateConfig("google.temperature", "5.0", this.credentialsFor(ADMIN_USERNAME)));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals(ex.getCause().getMessage(), ex.getMessage());
    }

    @Test
    @DisplayName("Validation accepts in-range temperature")
    @TestTransaction
    void testValidationAcceptsInRangeTemperature() {
        assertDoesNotThrow(() -> this.aiConfigService.updateConfig("google.temperature", "1.2",
                this.credentialsFor(ADMIN_USERNAME)));
    }

    @Test
    @DisplayName("Permission check rejects non-admin user")
    @TestTransaction
    void testPermissionCheckRejectsNonAdmin() {
        // student1 has rank Student (no exercise/lesson permissions)
        final var studentUsername = "student1";
        final var student = this.userRepository.findByUsername(studentUsername);
        assertNotNull(student, "Student user should exist");

        assertThrows(PermissionDeniedException.class, () -> this.aiConfigService.updateConfig("google.temperature",
                "0.5", this.credentialsFor(studentUsername)));
    }
}
