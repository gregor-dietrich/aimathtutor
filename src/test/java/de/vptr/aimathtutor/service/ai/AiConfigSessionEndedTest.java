package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
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
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestSessionCredentials;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
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
    /** A generic URL key: its host is resolved by the validation, so a resolver call shows validation ran. */
    private static final String EXTERNAL_URL_KEY = "custom.url";
    private static final String EXTERNAL_URL = "https://attacker.example/";

    @Inject
    AiConfigService aiConfigService;

    @Inject
    AiConfigRepository aiConfigRepository;

    @Inject
    EntityManager entityManager;

    @Inject
    UserRepository userRepository;

    @InjectMock
    HostResolver hostResolver;

    /** The unique key a test committed; null when it committed nothing. */
    @Nullable
    private String committedKey;

    static Stream<Arguments> writes() {
        final BiConsumer<AiConfigSessionEndedTest, SessionCredentials> save = (t,
                credentials) -> t.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(KEY, "0.3"),
                        // Would resolve its host if the session check did not come first
                        new AiConfigUpdateDto(EXTERNAL_URL_KEY, EXTERNAL_URL)), credentials);
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
        return staleStamp ? new SessionCredentials(admin.publicId, "stale", UUID.randomUUID().toString())
                : TestSessionCredentials.of(admin);
    }

    @SuppressWarnings("NullAway")
    private UserEntity seedValueAndGetAdmin() {
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

    /** Reads the seeded admin's credentials in a short transaction of its own, leaving the test without one. */
    private SessionCredentials committedCredentials(final boolean staleStamp) {
        return QuarkusTransaction.requiringNew().call(() -> this.credentialsFor(
                Objects.requireNonNull(this.userRepository.findByUsername("admin"), "Seeded admin should exist"),
                staleStamp));
    }

    @AfterEach
    void deleteCommittedKey() {
        final var key = this.committedKey;
        this.committedKey = null;
        if (key == null) {
            return;
        }
        AiConfigTestSupport.deleteCommittedKey(this.aiConfigRepository, this.entityManager, key);
    }

    @ParameterizedTest(name = "{0}: {2}")
    @MethodSource("writes")
    @DisplayName("A save or reset with an ended session is refused before validation and writes nothing")
    @TestTransaction
    void endedSessionIsRefused(final String name, final BiConsumer<AiConfigSessionEndedTest, SessionCredentials> write,
            final String reason, final Consumer<UserEntity> change, final boolean staleStamp) {
        final var admin = this.seedValueAndGetAdmin();
        final var credentials = this.credentialsFor(admin, staleStamp);
        change.accept(admin);

        // The session refusal, with no host resolved, proves authorisation runs before validation
        final var ex = assertThrows(ValidationException.class, () -> write.accept(this, credentials));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());

        this.entityManager.flush();
        this.entityManager.clear();
        final var unchanged = this.aiConfigRepository.findByConfigKey(KEY).orElseThrow();
        assertEquals(SEEDED_VALUE, unchanged.configValue);
        assertNull(unchanged.lastUpdatedBy);
        // The save's external URL would be resolved, and so would the reset's default URLs, if validation ran first
        verifyNoInteractions(this.hostResolver);
    }

    @Test
    @DisplayName("The same save with a valid session writes")
    @TestTransaction
    void validSessionWrites() {
        final var admin = this.seedValueAndGetAdmin();
        final var credentials = this.credentialsFor(admin, false);

        assertDoesNotThrow(() -> this.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(KEY, "0.3")),
                credentials));

        final var updated = this.aiConfigRepository.findByConfigKey(KEY).orElseThrow();
        assertEquals("0.3", updated.configValue);
        assertSame(admin, updated.lastUpdatedBy);
    }

    @Test
    @DisplayName("Without a caller transaction, an ended session is refused with the session-ended message")
    void endedSessionIsRefusedWithoutCallerTransaction() {
        final var credentials = this.committedCredentials(true);
        final var updates = List.of(new AiConfigUpdateDto(EXTERNAL_URL_KEY, EXTERNAL_URL));

        // A broken transaction interceptor would surface as a TransactionalException instead
        for (final Runnable write : List.<Runnable>of(
                () -> this.aiConfigService.updateMultipleConfigs(updates, credentials),
                () -> this.aiConfigService.updateConfig(EXTERNAL_URL_KEY, EXTERNAL_URL, credentials),
                () -> this.aiConfigService.resetToDefaults(credentials))) {
            final var ex = assertThrows(ValidationException.class, write::run);
            assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        }
        verifyNoInteractions(this.hostResolver);
    }

    @Test
    @SuppressWarnings("NullAway")
    @DisplayName("Without a caller transaction, a valid session's save commits and records the admin")
    void validSessionCommitsWithoutCallerTransaction() {
        final var credentials = this.committedCredentials(false);
        // The write commits, so it goes to a key of its own, which deleteCommittedKey removes
        final var key = "test.commit-" + UUID.randomUUID();
        this.committedKey = key;

        this.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(key, "0.3")), credentials);

        final var stored = QuarkusTransaction.requiringNew().call(() -> {
            final var entity = this.aiConfigRepository.findByConfigKey(key).orElseThrow();
            return List.of(entity.configValue, entity.lastUpdatedBy.publicId);
        });
        assertEquals(List.of("0.3", credentials.userPublicId()), stored);
    }

    @Test
    @DisplayName("The write's own transaction re-checks the session and writes nothing")
    @TestTransaction
    void writeTransactionRechecksSession() {
        final var admin = this.seedValueAndGetAdmin();
        final var credentials = this.credentialsFor(admin, true);

        final var multiple = assertThrows(ValidationException.class, () -> this.aiConfigService
                .persistMultipleConfigUpdates(List.of(new AiConfigUpdateDto(KEY, "0.3")), credentials));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, multiple.getMessage());
        final var single = assertThrows(ValidationException.class,
                () -> this.aiConfigService.persistConfigUpdate(KEY, "0.3", credentials));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, single.getMessage());

        this.entityManager.flush();
        this.entityManager.clear();
        final var unchanged = this.aiConfigRepository.findByConfigKey(KEY).orElseThrow();
        assertEquals(SEEDED_VALUE, unchanged.configValue);
        assertNull(unchanged.lastUpdatedBy);
    }
}
