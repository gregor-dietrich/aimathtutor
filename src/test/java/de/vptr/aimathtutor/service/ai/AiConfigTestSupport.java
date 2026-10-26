package de.vptr.aimathtutor.service.ai;

import java.util.Map;
import java.util.stream.Collectors;

import de.vptr.aimathtutor.repository.AiConfigRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.persistence.EntityManager;

/** Shared helpers of the AI-config tests that commit rows of their own. */
final class AiConfigTestSupport {

    private AiConfigTestSupport() {
    }

    /** Deletes a committed key, if present, in a short transaction of its own. */
    static void deleteCommittedKey(final AiConfigRepository repository, final EntityManager entityManager,
            final String key) {
        QuarkusTransaction.requiringNew().run(() -> repository.findByConfigKey(key).ifPresent(entityManager::remove));
    }

    /** The built-in defaults by key. */
    static Map<String, String> defaults() {
        return AiConfigService.defaultUpdates().stream()
                .collect(Collectors.toMap(update -> update.configKey, update -> update.configValue));
    }

    /** Sets the value of an existing row through the repository. */
    static void setStoredValue(final AiConfigRepository repository, final String key, final String value) {
        final var entity = repository.findByConfigKey(key).orElseThrow();
        entity.configValue = value;
        repository.update(entity);
    }
}
