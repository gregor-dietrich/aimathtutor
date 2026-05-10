package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link AiConfigRepository}.
 */
@QuarkusTest
public class AiConfigRepositoryIT {

    @Inject
    AiConfigRepository aiConfigRepository;

    private AiConfigEntity createConfig(final String suffix) {
        final AiConfigEntity config = new AiConfigEntity("test.key." + suffix, "test_value_" + suffix,
                ConfigType.STRING, ConfigCategory.GENERAL, "Test config " + suffix);
        this.aiConfigRepository.persist(config);
        return config;
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsConfigs() {
        final AiConfigEntity config = this.createConfig("fall");
        final List<AiConfigEntity> all = this.aiConfigRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
        Assertions.assertTrue(all.stream().anyMatch(c -> Objects.equals(c.id, config.id)));
    }

    @Test
    @TestTransaction
    public void testFindByConfigKey_found() {
        final AiConfigEntity config = this.createConfig("fkey");
        final Optional<AiConfigEntity> found =
                this.aiConfigRepository.findByConfigKey(Objects.requireNonNull(config.configKey));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(config.configKey, found.get().configKey);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByConfigKey_notFound() {
        Assertions.assertTrue(this.aiConfigRepository.findByConfigKey("nonexistent.key").isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByConfigKey_null() {
        Assertions.assertTrue(this.aiConfigRepository.findByConfigKey(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final AiConfigEntity config = this.createConfig("fpub");
        final Optional<AiConfigEntity> found =
                this.aiConfigRepository.findByPublicId(Objects.requireNonNull(config.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(config.publicId, found.get().publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.aiConfigRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByCategory_returnsConfigsInCategory() {
        final AiConfigEntity config = this.createConfig("fcat");
        final List<AiConfigEntity> results = this.aiConfigRepository.findByCategory(ConfigCategory.GENERAL);
        Assertions.assertFalse(results.isEmpty());
        Assertions.assertTrue(results.stream().anyMatch(c -> Objects.equals(c.id, config.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByCategory_null() {
        Assertions.assertTrue(this.aiConfigRepository.findByCategory(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testPersist_config() {
        final AiConfigEntity config = new AiConfigEntity("test.key.persist_" + UUID.randomUUID(), "persist_value",
                ConfigType.INTEGER, ConfigCategory.OLLAMA);
        this.aiConfigRepository.persist(config);
        Assertions.assertNotNull(config.id);
        Assertions.assertNotNull(config.publicId);
    }

    @Test
    @TestTransaction
    public void testUpdate_config() {
        final AiConfigEntity config = this.createConfig("upd");
        config.configValue = "updated_value";
        final Optional<AiConfigEntity> found =
                this.aiConfigRepository.findByConfigKey(Objects.requireNonNull(config.configKey));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("updated_value", found.get().configValue);
    }

    @Test
    @TestTransaction
    public void testDeleteById_existing() {
        final AiConfigEntity config = this.createConfig("delid");
        final Long id = Objects.requireNonNull(config.id);
        this.aiConfigRepository.deleteById(id);
        Assertions.assertTrue(
                this.aiConfigRepository.findByConfigKey(Objects.requireNonNull(config.configKey)).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertDoesNotThrow(() -> this.aiConfigRepository.deleteById(999_999L));
    }
}
