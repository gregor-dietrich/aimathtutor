package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing user entities.
 * Provides database access and query operations for users including
 * find by ID, username, email, and search operations.
 */
@ApplicationScoped
public class UserRepository extends AbstractRepository {

    /**
     * Retrieves a user by its unique identifier.
     *
     * @param id the user ID
     * @return the {@link UserEntity} if found, null otherwise
     */
    public UserEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserEntity.class, id);
    }

    /**
     * Retrieves an optional user by its unique identifier.
     *
     * @param id the user ID
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves an optional user by its username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByUsernameOptional(final String username) {
        if (username == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("User.findByUsername", UserEntity.class);
        q.setParameter("u", username);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves a user by its username.
     *
     * @param username the username to search for
     * @return the {@link UserEntity} if found, null otherwise
     */
    public UserEntity findByUsername(final String username) {
        return this.findByUsernameOptional(username).orElse(null);
    }

    /**
     * Retrieves an optional user by its email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByEmailOptional(final String email) {
        if (email == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("User.findByEmail", UserEntity.class);
        q.setParameter("e", email);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Persists a user entity to the database.
     *
     * @param user the user to persist; null values are ignored
     * @return the persisted {@link UserEntity}, or null if the input was null
     */
    @Transactional
    public UserEntity persist(final UserEntity user) {
        if (user == null) {
            return null;
        }
        this.em.persist(user);
        return user;
    }

    /**
     * Retrieves all users from the database in a defined order.
     *
     * @return a list of all {@link UserEntity} objects
     */
    public List<UserEntity> findAll() {
        return this.listNamed("User.findAllOrdered", UserEntity.class);
    }

    /**
     * Retrieves all active users from the database.
     *
     * @return a list of active {@link UserEntity} objects
     */
    public List<UserEntity> findActiveUsers() {
        return this.listNamed("User.findActive", UserEntity.class);
    }

    /**
     * Retrieves all users with a specific rank.
     *
     * @param rankId the rank ID to filter by
     * @return a list of {@link UserEntity} objects with the specified rank
     */
    public List<UserEntity> findByRankId(final Long rankId) {
        final var q = this.em.createNamedQuery("User.findByRankId", UserEntity.class);
        q.setParameter("r", rankId);
        return q.getResultList();
    }

    /**
     * Searches for users matching the given search term.
     *
     * @param searchTerm the search term to match against user properties;
     *                   if null or empty, returns all users
     * @return a list of {@link UserEntity} objects matching the search term
     */
    public List<UserEntity> search(final String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return this.findAll();
        }
        final var q = this.em.createNamedQuery("User.searchByTerm", UserEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * Deletes a user by its unique identifier.
     *
     * @param id the ID of the user to delete
     * @return true if the user was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final UserEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
