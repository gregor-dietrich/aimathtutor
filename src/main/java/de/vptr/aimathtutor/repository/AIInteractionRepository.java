package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.AIInteractionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AIInteractionRepository extends AbstractRepository {

    public List<AIInteractionEntity> findAll() {
        return listNamed("AIInteraction.findAll", AIInteractionEntity.class);
    }

    public List<AIInteractionEntity> findBySessionId(final String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AIInteraction.findBySessionId", AIInteractionEntity.class);
        q.setParameter("s", sessionId);
        return q.getResultList();
    }

    public List<AIInteractionEntity> findByUserId(final Long userId) {
        if (userId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AIInteraction.findByUserId", AIInteractionEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    public List<AIInteractionEntity> findByExerciseId(final Long exerciseId) {
        if (exerciseId == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AIInteraction.findByExerciseId", AIInteractionEntity.class);
        q.setParameter("e", exerciseId);
        return q.getResultList();
    }

    @Transactional
    public AIInteractionEntity persist(final AIInteractionEntity interaction) {
        if (interaction == null) {
            return null;
        }
        this.em.persist(interaction);
        return interaction;
    }
}
