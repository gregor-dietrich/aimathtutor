package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.AiConfigEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing AI configuration entities.
 * Provides database access and query operations for AI configuration settings.
 */
@ApplicationScoped
public class AiConfigRepository extends AbstractRepository {

    /**
     * Retrieves all AI configuration entries.
     *
     * @return a list of all {@link AiConfigEntity} objects
     */
    public List<AiConfigEntity> findAll() {
        return this.listNamed("AiConfig.findAll", AiConfigEntity.class);
    }

    /**
     * Retrieves a configuration entry by its key.
     *
     * @param configKey the configuration key to search for
     * @return an Optional containing the entity if found, empty otherwise
     */
    public Optional<AiConfigEntity> findByConfigKey(final String configKey) {
        if (configKey == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("AiConfig.findByKey", AiConfigEntity.class);
        q.setParameter("key", configKey);
        final var results = q.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Retrieves all configuration entries in a specific category.
     *
     * @param category the category to filter by
     * @return a list of {@link AiConfigEntity} objects in the category
     */
    public List<AiConfigEntity> findByCategory(final String category) {
        if (category == null) {
            return List.of();
        }
        final var q = this.em.createNamedQuery("AiConfig.findByCategory", AiConfigEntity.class);
        q.setParameter("category", category);
        return q.getResultList();
    }

    /**
     * Persists a new AI configuration entity or updates an existing one.
     *
     * @param entity the entity to persist or update
     */
    @Transactional
    public void persist(final AiConfigEntity entity) {
        this.em.persist(entity);
    }

    /**
     * Updates an existing AI configuration entity.
     *
     * @param entity the entity to update
     * @return the merged entity
     */
    @Transactional
    public AiConfigEntity update(final AiConfigEntity entity) {
        return this.em.merge(entity);
    }

    /**
     * Removes an AI configuration entity by ID.
     *
     * @param id the ID of the entity to delete
     */
    @Transactional
    public void deleteById(final Long id) {
        final var entity = this.em.find(AiConfigEntity.class, id);
        if (entity != null) {
            this.em.remove(entity);
        }
    }
}
