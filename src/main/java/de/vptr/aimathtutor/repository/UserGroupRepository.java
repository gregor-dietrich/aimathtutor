package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserGroupEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing user group entities.
 * Provides database access and query operations for user groups including
 * find, search, persist, and delete operations.
 */
@ApplicationScoped
public class UserGroupRepository extends AbstractRepository {

    /**
     * Retrieves all user groups from the database.
     *
     * @return a list of all {@link UserGroupEntity} objects
     */
    public List<UserGroupEntity> findAll() {
        return this.listNamed("UserGroup.findAll", UserGroupEntity.class);
    }

    /**
     * Retrieves an optional user group by its unique identifier.
     *
     * @param id the user group ID
     * @return an {@link Optional} containing the user group if found, empty
     *         otherwise
     */
    public Optional<UserGroupEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves a user group by its unique identifier.
     *
     * @param id the user group ID
     * @return the {@link UserGroupEntity} if found, null otherwise
     */
    public UserGroupEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserGroupEntity.class, id);
    }

    /**
     * Retrieves a user group by its name.
     *
     * @param name the name of the user group to find
     * @return the {@link UserGroupEntity} if found, null otherwise
     */
    public UserGroupEntity findByName(final String name) {
        final var q = this.em.createNamedQuery("UserGroup.findByName", UserGroupEntity.class);
        q.setParameter("n", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * Searches for user groups matching the given search term.
     *
     * @param searchTerm the search term to match against user group names
     * @return a list of {@link UserGroupEntity} objects matching the search term
     */
    public List<UserGroupEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("UserGroup.searchByName", UserGroupEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * Persists a user group entity to the database.
     *
     * @param group the user group to persist; null values are ignored
     */
    @Transactional
    public void persist(final UserGroupEntity group) {
        if (group == null) {
            return;
        }
        this.em.persist(group);
    }

    /**
     * Deletes a user group by its unique identifier.
     *
     * @param id the ID of the user group to delete
     * @return true if the user group was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final UserGroupEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
