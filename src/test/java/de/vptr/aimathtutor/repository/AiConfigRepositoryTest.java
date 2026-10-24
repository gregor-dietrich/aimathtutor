package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import de.vptr.aimathtutor.entity.AiConfigEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AiConfigRepositoryTest {

    @Inject
    AiConfigRepository aiConfigRepository;

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Null handling for repository methods")
    void testNullHandling() {
        assertTrue(this.aiConfigRepository.findByConfigKey(null).isEmpty());
        assertTrue(this.aiConfigRepository.findByCategory(null).isEmpty());
    }

    @Test
    @DisplayName("findAll returns non-null list with seeded entries")
    void testFindAll() {
        final var all = this.aiConfigRepository.findAll();
        assertNotNull(all);
        assertFalse(all.isEmpty());
    }

    @Test
    @DisplayName("findByConfigKey returns present for existing key and empty for unknown")
    void testFindByConfigKey() {
        assertTrue(this.aiConfigRepository.findByConfigKey("ai.tutor.provider").isPresent());
        assertTrue(this.aiConfigRepository.findByConfigKey("no.such.key.xyz").isEmpty());
    }

    @Test
    @DisplayName("findByCategory returns non-empty list for GENERAL category")
    void testFindByCategory() {
        final var results = this.aiConfigRepository.findByCategory(ConfigCategory.GENERAL);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("persist and update work correctly")
    @TestTransaction
    void testPersistAndUpdate() {
        final var entity = new AiConfigEntity("test.update." + UlidUtil.generate(), "original", ConfigType.STRING,
                ConfigCategory.GENERAL);
        entity.publicId = UlidUtil.generate();
        this.aiConfigRepository.persist(entity);
        this.aiConfigRepository.flush();

        entity.configValue = "updated";
        final var merged = this.aiConfigRepository.update(entity);
        assertNotNull(merged);
    }
}
