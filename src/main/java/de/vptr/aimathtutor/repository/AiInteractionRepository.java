package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing AI interaction entities. Provides database access and query operations for AI interactions
 * including find by session ID.
 */
@ApplicationScoped
public class AiInteractionRepository extends AbstractRepository {

    /**
     * Retrieves all AI interactions for a specific student session.
     *
     * @param sessionId
     *            the session ID to filter by; null values return empty list
     * @return a list of {@link AiInteractionEntity} objects for the session
     */
    public List<AiInteractionEntity> findBySessionId(final String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findBySessionId", AiInteractionEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    /**
     * Persists an AI interaction entity to the database.
     *
     * @param interaction
     *            the AI interaction to persist; null values are ignored
     * @return the persisted {@link AiInteractionEntity}, or null if the input was null
     */
    @Transactional
    @Nullable
    public AiInteractionEntity persist(final AiInteractionEntity interaction) {
        if (interaction == null) {
            return null;
        }
        this.em.persist(interaction);
        return interaction;
    }
}
