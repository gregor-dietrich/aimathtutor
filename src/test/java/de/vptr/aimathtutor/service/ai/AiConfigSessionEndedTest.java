package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;

/**
 * An AI-config save or reset made with a session that has ended (stale credential stamp, banned or deactivated account)
 * is refused with {@link AppConstants#SESSION_ENDED_MESSAGE}, before any value or URL validation, and writes nothing.
 * The real permission and authentication services run.
 */
@QuarkusTest
class AiConfigSessionEndedTest {

    private static final String KEY = AiConfigKeys.GOOGLE_TEMPERATURE;
    private static final String SEEDED_VALUE = "1.7";
    private static final String BLOCKED_URL_KEY = AiConfigKeys.OLLAMA_API_URL;

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    EntityManager entityManager;

    @Inject
    UserRepository userRepository;

    static Stream<Arguments> writes() {
        final BiConsumer<AiConfigSessionEndedTest, SessionCredentials> save = (t,
                credentials) -> t.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(KEY, "0.3"),
                        // Would fail URL validation if the session check did not come first
                        new AiConfigUpdateDto(BLOCKED_URL_KEY, "http://evil.invalid")), credentials);
        final BiConsumer<AiConfigSessionEndedTest, SessionCredentials> reset =
                (t, credentials) -> t.aiConfigService.resetToDefaults(credentials);
        final Consumer<UserEntity> stale = admin -> {
        };
        final Consumer<UserEntity> banned = admin -> admin.banned = true;
        final Consumer<UserEntity> deactivated = admin -> admin.activated = false;
        return Stream.of(Arguments.of("save", save, "stale stamp", stale, true),
                Arguments.of("save", save, "banned", banned, false),
                Arguments.of("save", save, "deactivated", deactivated, false),
                Arguments.of("reset", reset, "stale stamp", stale, true),
                Arguments.of("reset", reset, "banned", banned, false),
                Arguments.of("reset", reset, "deactivated", deactivated, false));
    }

    @SuppressWarnings("NullAway")
    private SessionCredentials credentialsFor(final UserEntity admin, final boolean staleStamp) {
        return new SessionCredentials(admin.publicId,
                staleStamp ? "stale" : AuthService.credentialStamp(admin.password), UUID.randomUUID().toString());
    }

    @SuppressWarnings("NullAway")
    private UserEntity seedValue() {
        final var admin = this.userRepository.findByUsername("admin");
        final var existing = this.aiConfigRepository.findByConfigKey(KEY);
        final var entity = existing.orElseGet(() -> {
            final var created = new AiConfigEntity();
            created.configKey = KEY;
            created.configType = ConfigType.DOUBLE;
            created.category = ConfigCategory.GOOGLE;
            return created;
        });
        entity.configValue = SEEDED_VALUE;
        entity.lastUpdatedBy = null;
        if (existing.isEmpty()) {
            this.aiConfigRepository.persist(entity);
        } else {
            this.aiConfigRepository.update(entity);
        }
        this.entityManager.flush();
        return admin;
    }

    @ParameterizedTest(name = "{0}: {2}")
    @MethodSource("writes")
    @DisplayName("A save or reset with an ended session is refused before validation and writes nothing")
    @TestTransaction
    void endedSessionIsRefused(final String name, final BiConsumer<AiConfigSessionEndedTest, SessionCredentials> write,
            final String reason, final Consumer<UserEntity> change, final boolean staleStamp) {
        final var admin = this.seedValue();
        final var credentials = this.credentialsFor(admin, staleStamp);
        change.accept(admin);

        final var ex = assertThrows(ValidationException.class, () -> write.accept(this, credentials));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());

        this.entityManager.flush();
        this.entityManager.clear();
        final var unchanged = this.aiConfigRepository.findByConfigKey(KEY).orElseThrow();
        assertEquals(SEEDED_VALUE, unchanged.configValue);
        assertNull(unchanged.lastUpdatedBy);
        assertTrue(this.aiConfigRepository.findByConfigKey(BLOCKED_URL_KEY).isEmpty() || !"http://evil.invalid"
                .equals(this.aiConfigRepository.findByConfigKey(BLOCKED_URL_KEY).get().configValue));
    }

    @Test
    @DisplayName("The same save with a valid session writes")
    @TestTransaction
    void validSessionWrites() {
        final var admin = this.seedValue();
        final var credentials = this.credentialsFor(admin, false);

        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(KEY, "0.3")),
                credentials));

        final var updated = this.aiConfigRepository.findByConfigKey(KEY).orElseThrow();
        assertEquals("0.3", updated.configValue);
        assertSame(admin, updated.lastUpdatedBy);
    }
}
