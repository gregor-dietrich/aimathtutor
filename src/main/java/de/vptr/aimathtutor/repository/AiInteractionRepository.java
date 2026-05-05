package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing AI interaction entities.
 * Provides database access and query operations for AI interactions including
 * find by session, user, and exercise ID operations.
 */
@ApplicationScoped
public class AiInteractionRepository extends AbstractRepository {

    /**
     * Retrieves all AI interactions from the database.
     *
     * @return a list of all {@link AiInteractionEntity} objects
     */
    public List<AiInteractionEntity> findAll() {
        return this.listNamed("AiInteraction.findAll", AiInteractionEntity.class);
    }

    /**
     * Retrieves an AI interaction by its public identifier.
     *
     * @param publicId the public ID of the interaction
     * @return an {@link Optional} containing the interaction if found, empty otherwise
     */
    public Optional<AiInteractionEntity> findByPublicId(final String publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findByPublicId", AiInteractionEntity.class);
        q.setParameter("p", publicId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves all AI interactions for a specific student session.
     *
     * @param sessionId the session ID to filter by; null values return empty list
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
     * Retrieves all AI interactions initiated by a specific user.
     *
     * @param userId the user ID to filter by; null values return empty list
     * @return a list of {@link AiInteractionEntity} objects created by the user
     */
    public List<AiInteractionEntity> findByUserId(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findByUserId", AiInteractionEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Retrieves all AI interactions related to a specific exercise.
     *
     * @param exerciseId the exercise ID to filter by; null values return empty list
     * @return a list of {@link AiInteractionEntity} objects for the exercise
     */
    public List<AiInteractionEntity> findByExerciseId(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findByExerciseId", AiInteractionEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    /**
     * Persists an AI interaction entity to the database.
     *
     * @param interaction the AI interaction to persist; null values are ignored
     * @return the persisted {@link AiInteractionEntity}, or null if the input was
     *         null
     */
    @Transactional
    public AiInteractionEntity persist(final AiInteractionEntity interaction) {
        if (interaction == null) {
            return null;
        }
        this.em.persist(interaction);
        return interaction;
    }
}
