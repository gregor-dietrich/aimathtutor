package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserRankEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing user rank entities.
 * Provides database access and query operations for user ranks including
 * find, search, persist, and delete operations.
 */
@ApplicationScoped
public class UserRankRepository extends AbstractRepository {

    /**
     * Retrieves all user ranks from the database.
     *
     * @return a list of all {@link UserRankEntity} objects
     */
    public List<UserRankEntity> findAll() {
        return this.listNamed("UserRank.findAll", UserRankEntity.class);
    }

    /**
     * Retrieves an optional user rank by its unique identifier.
     *
     * @param id the user rank ID
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    public Optional<UserRankEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves a user rank by its unique identifier.
     *
     * @param id the user rank ID
     * @return the {@link UserRankEntity} if found, null otherwise
     */
    public UserRankEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserRankEntity.class, id);
    }

    /**
     * Retrieves an optional user rank by its name.
     *
     * @param name the name of the user rank to find
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    public Optional<UserRankEntity> findByName(final String name) {
        final var q = this.em.createNamedQuery("UserRank.findByName", UserRankEntity.class);
        q.setParameter("n", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Searches for user ranks matching the given search term.
     *
     * @param searchTerm the search term to match against user rank names
     * @return a list of {@link UserRankEntity} objects matching the search term
     */
    public List<UserRankEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("UserRank.searchByName", UserRankEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * Persists a user rank entity to the database.
     *
     * @param rank the user rank to persist; null values are ignored
     */
    @Transactional
    public void persist(final UserRankEntity rank) {
        if (rank == null) {
            return;
        }
        this.em.persist(rank);
    }

    /**
     * Deletes a user rank by its unique identifier.
     *
     * @param id the ID of the user rank to delete
     * @return true if the rank was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final UserRankEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
