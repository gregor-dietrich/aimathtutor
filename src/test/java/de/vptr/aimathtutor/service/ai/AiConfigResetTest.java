package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestSessionCredentials;
import io.quarkus.arc.ClientProxy;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;

/**
 * "Reset to Defaults" writes the built-in defaults without validating them, so it needs no DNS lookup, and a test
 * proves that the constants pass a normal save's validation. The tests that commit restore the stored values.
 */
@QuarkusTest
@DisplayName("AiConfigService reset to defaults")
class AiConfigResetTest {

    private static final String TEMPERATURE_KEY = AiConfigKeys.GOOGLE_TEMPERATURE;
    private static final String SEEDED_VALUE = "1.7";

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager entityManager;

    @InjectMock
    HostResolver hostResolver;

    /** The committed values of the default keys before the test; a key without a row is absent. */
    private Map<String, String> original = Map.of();

    private static Map<String, String> defaults() {
        return AiConfigService.defaultUpdates().stream()
                .collect(Collectors.toMap(update -> update.configKey, update -> update.configValue));
    }

    @BeforeEach
    void snapshot() {
        final var defaultKeys = defaults().keySet();
        this.original = QuarkusTransaction.requiringNew().call(() -> {
            final var values = new HashMap<String, String>();
            this.aiConfigRepository.findAll().stream().filter(entity -> defaultKeys.contains(entity.configKey))
                    .forEach(entity -> values.put(Objects.requireNonNull(entity.configKey), entity.configValue));
            return Map.copyOf(values);
        });
        this.aiConfigService.clearCache();
    }

    /** Puts the committed rows of the default keys back as {@link #snapshot()} found them. */
    @AfterEach
    void restore() {
        QuarkusTransaction.requiringNew().run(() -> defaults().keySet().forEach(key -> {
            final var row = this.aiConfigRepository.findByConfigKey(key);
            final var value = this.original.get(key);
            if (value == null) {
                row.ifPresent(this.entityManager::remove);
            } else {
                row.ifPresent(entity -> entity.configValue = value);
            }
        }));
        this.aiConfigService.clearCache();
    }

    private SessionCredentials credentialsOf(final String username) {
        return TestSessionCredentials
                .of(Objects.requireNonNull(this.userRepository.findByUsername(username), "Seeded user"));
    }

    private void setStoredValue(final String key, final String value) {
        final var entity = this.aiConfigRepository.findByConfigKey(key).orElseThrow();
        entity.configValue = value;
        this.aiConfigRepository.update(entity);
    }

    private void assertTemperatureUnchanged() {
        this.entityManager.flush();
        this.entityManager.clear();
        final var stored = this.aiConfigRepository.findByConfigKey(TEMPERATURE_KEY).map(entity -> entity.configValue);
        assertEquals(SEEDED_VALUE, stored.orElseThrow());
    }

    @Test
    @DisplayName("Reset commits every default where no host resolves, and resolves nothing")
    void resetWithoutDnsRestoresAllDefaults() throws UnknownHostException {
        when(this.hostResolver.resolveAll(anyString())).thenThrow(new UnknownHostException("no DNS"));
        final var credentials = QuarkusTransaction.requiringNew().call(() -> {
            this.setStoredValue(TEMPERATURE_KEY, SEEDED_VALUE);
            this.setStoredValue(AiConfigKeys.OPENAI_API_BASE_URL, "https://api.openai.com/v1/beta");
            this.setStoredValue(AiConfigKeys.PROMPT_TUTORING_PREFIX, "changed " + UUID.randomUUID());
            return this.credentialsOf("admin");
        });
        this.aiConfigService.clearCache();

        this.aiConfigService.resetToDefaults(credentials);

        this.aiConfigService.clearCache();
        assertEquals(defaults(), this.aiConfigService.getEffectiveConfig());
        verifyNoInteractions(this.hostResolver);
    }

    @Test
    @DisplayName("Reset writes the default URL although the Google allow-list no longer holds its host")
    void resetIgnoresNarrowedAllowList() throws UnknownHostException {
        when(this.hostResolver.resolveAll(anyString()))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(TestHostResolver.PUBLIC_ADDRESS) });
        final var credentials = QuarkusTransaction.requiringNew().call(() -> {
            this.setStoredValue(AiConfigKeys.GOOGLE_API_BASE_URL, "https://gateway.example");
            return this.credentialsOf("admin");
        });
        final var bean = ClientProxy.unwrap(this.aiConfigService);
        final var previous = bean.allowedGoogleHosts;
        bean.allowedGoogleHosts = Set.of("gateway.example");
        try {
            // The narrowing takes effect: a normal save of the default URL is refused
            assertThrows(IllegalArgumentException.class,
                    () -> this.aiConfigService.validateProviderApiUrl(defaults().get(AiConfigKeys.GOOGLE_API_BASE_URL),
                            AiConfigService.ProviderType.GOOGLE));

            this.aiConfigService.resetToDefaults(credentials);
        } finally {
            bean.allowedGoogleHosts = previous;
        }

        this.aiConfigService.clearCache();
        assertEquals(defaults().get(AiConfigKeys.GOOGLE_API_BASE_URL),
                this.aiConfigService.getConfigValue(AiConfigKeys.GOOGLE_API_BASE_URL, null));
    }

    @Test
    @DisplayName("Reset with an ended session is refused and changes nothing")
    @TestTransaction
    void resetWithEndedSessionIsRefused() {
        this.setStoredValue(TEMPERATURE_KEY, SEEDED_VALUE);
        final var admin = Objects.requireNonNull(this.userRepository.findByUsername("admin"), "Seeded admin");
        final var stale =
                new SessionCredentials(Objects.requireNonNull(admin.publicId), "stale", UUID.randomUUID().toString());

        final var ex = assertThrows(ValidationException.class, () -> this.aiConfigService.resetToDefaults(stale));

        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        this.assertTemperatureUnchanged();
    }

    @Test
    @DisplayName("Reset by a user without the AI-config permission is refused and changes nothing")
    @TestTransaction
    void resetWithoutPermissionIsRefused() {
        this.setStoredValue(TEMPERATURE_KEY, SEEDED_VALUE);
        final var student = this.credentialsOf("student1");

        assertThrows(PermissionDeniedException.class, () -> this.aiConfigService.resetToDefaults(student));

        this.assertTemperatureUnchanged();
    }

    @Test
    @DisplayName("Every default passes the full validation of a normal save")
    @TestTransaction
    void defaultsPassSaveValidation() throws UnknownHostException {
        when(this.hostResolver.resolveAll(anyString()))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(TestHostResolver.PUBLIC_ADDRESS) });
        final var credentials = this.credentialsOf("admin");

        assertDoesNotThrow(
                () -> this.aiConfigService.updateMultipleConfigs(AiConfigService.defaultUpdates(), credentials));
    }
}
