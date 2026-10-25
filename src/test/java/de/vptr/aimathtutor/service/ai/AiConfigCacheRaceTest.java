package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.repository.AiConfigRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * A saved AI setting never stays cached at its old value, whichever way a concurrent reader's fill and the write's
 * commit interleave. Concurrent transactions can't share a test transaction, so every test commits a key of its own and
 * deletes it afterwards.
 */
@QuarkusTest
class AiConfigCacheRaceTest {

    private static final String V0 = "v0";
    private static final String V1 = "v1";
    private static final long TIMEOUT_SECONDS = 30;

    @Inject
    AiConfigService aiConfigService;

    @InjectSpy
    AiConfigRepository aiConfigRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager entityManager;

    private final String key = "test.cache-race-" + UUID.randomUUID();

    @Nullable
    private SessionCredentials credentials;

    static Stream<BiConsumer<AiConfigCacheRaceTest, String>> writes() {
        return Stream.<BiConsumer<AiConfigCacheRaceTest, String>>of(
                (t, value) -> t.aiConfigService.updateConfig(t.key, value, t.credentials()),
                (t, value) -> t.aiConfigService.updateMultipleConfigs(List.of(new AiConfigUpdateDto(t.key, value)),
                        t.credentials()));
    }

    private SessionCredentials credentials() {
        return Objects.requireNonNull(this.credentials, "seed() not called");
    }

    /** Commits the key with V0 and the admin's credentials, each in a short transaction of its own. */
    private void seed() {
        this.credentials = QuarkusTransaction.requiringNew().call(() -> {
            final var entity = new AiConfigEntity();
            entity.configKey = this.key;
            entity.configValue = V0;
            entity.configType = ConfigType.STRING;
            entity.category = ConfigCategory.GENERAL;
            this.aiConfigRepository.persist(entity);
            final var admin = Objects.requireNonNull(this.userRepository.findByUsername("admin"), "Seeded admin");
            return new SessionCredentials(Objects.requireNonNull(admin.publicId),
                    Objects.requireNonNull(AuthService.credentialStamp(admin.password)), UUID.randomUUID().toString());
        });
        this.aiConfigService.clearCache();
    }

    @AfterEach
    void deleteKey() {
        QuarkusTransaction.requiringNew()
                .run(() -> this.aiConfigRepository.findByConfigKey(this.key).ifPresent(this.entityManager::remove));
        this.aiConfigService.clearCache();
    }

    @ParameterizedTest
    @MethodSource("writes")
    @DisplayName("A fill that read the old row before a commit and caches after it does not keep the old value")
    void fillCachingAfterCommitIsDiscarded(final BiConsumer<AiConfigCacheRaceTest, String> write) throws Exception {
        this.seed();
        final var readDone = new CountDownLatch(1);
        final var committed = new CountDownLatch(1);
        final var reader = new Thread[1];
        doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            if (Thread.currentThread() == reader[0]) {
                readDone.countDown();
                assertTrue(committed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The write never committed");
            }
            return result;
        }).when(this.aiConfigRepository).findByConfigKey(this.key);

        try (var executor = Executors.newSingleThreadExecutor(runnable -> {
            reader[0] = new Thread(runnable, "cache-race-reader");
            return reader[0];
        })) {
            final Future<String> read = executor.submit(() -> this.aiConfigService.getConfigValue(this.key, null));
            assertTrue(readDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The reader never read the row");
            write.accept(this, V1);
            committed.countDown();
            assertEquals(V0, read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        assertEquals(V1, this.aiConfigService.getConfigValue(this.key, null));
    }

    @Test
    @DisplayName("A value read while the write is uncommitted is not kept after the commit")
    void readBeforeCommitIsEvictedAfterIt() throws Exception {
        this.seed();
        final var written = new CountDownLatch(1);
        final var release = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            final Future<?> writer = executor.submit(() -> QuarkusTransaction.requiringNew().run(() -> {
                this.aiConfigService.updateConfig(this.key, V1, this.credentials());
                written.countDown();
                try {
                    assertTrue(release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The writer was never released");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            assertTrue(written.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The writer never wrote");
            assertEquals(V0, this.aiConfigService.getConfigValue(this.key, null));
            release.countDown();
            writer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        assertEquals(V1, this.aiConfigService.getConfigValue(this.key, null));
    }

    @Test
    @DisplayName("A value cached inside a transaction that rolls back is evicted")
    void rollbackEvictsTheUncommittedValue() {
        this.seed();

        // Running to the end of a transaction marked for rollback reports the rollback as an exception
        assertThrows(QuarkusTransactionException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            this.aiConfigService.updateConfig(this.key, V1, this.credentials());
            assertEquals(V1, this.aiConfigService.getConfigValue(this.key, null));
            QuarkusTransaction.setRollbackOnly();
        }));

        assertEquals(V0, this.aiConfigService.getConfigValue(this.key, null));
    }
}
