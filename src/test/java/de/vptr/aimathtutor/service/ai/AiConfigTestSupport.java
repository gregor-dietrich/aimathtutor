package de.vptr.aimathtutor.service.ai;

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
}
