package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AiInteractionRepository extends AbstractRepository {

    public List<AiInteractionEntity> findAll() {
        return this.listNamed("AiInteraction.findAll", AiInteractionEntity.class);
    }

    public List<AiInteractionEntity> findBySessionId(final String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findBySessionId", AiInteractionEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    public List<AiInteractionEntity> findByUserId(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findByUserId", AiInteractionEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    public List<AiInteractionEntity> findByExerciseId(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiInteraction.findByExerciseId", AiInteractionEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    @Transactional
    public AiInteractionEntity persist(final AiInteractionEntity interaction) {
        if (interaction == null) {
            return null;
        }
        this.em.persist(interaction);
        return interaction;
    }
}
