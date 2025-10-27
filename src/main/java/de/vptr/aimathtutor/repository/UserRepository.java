package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public class UserRepository extends AbstractRepository {

    /**
     * TODO: Document findById().
     */
    public UserEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserEntity.class, id);
    }

    /**
     * TODO: Document findByIdOptional().
     */
    public Optional<UserEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * TODO: Document findByUsernameOptional().
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
     * TODO: Document findByUsername().
     */
    public UserEntity findByUsername(final String username) {
        return this.findByUsernameOptional(username).orElse(null);
    }

    /**
     * TODO: Document findByEmailOptional().
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
     * TODO: Document persist().
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
     * TODO: Document findAll().
     */
    public List<UserEntity> findAll() {
        return this.listNamed("User.findAllOrdered", UserEntity.class);
    }

    /**
     * TODO: Document findActiveUsers().
     */
    public List<UserEntity> findActiveUsers() {
        return this.listNamed("User.findActive", UserEntity.class);
    }

    /**
     * TODO: Document findByRankId().
     */
    public List<UserEntity> findByRankId(final Long rankId) {
        final var q = this.em.createNamedQuery("User.findByRankId", UserEntity.class);
        q.setParameter("r", rankId);
        return q.getResultList();
    }

    /**
     * TODO: Document search().
     */
    public List<UserEntity> search(final String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return this.findAll();
        }
        final var q = this.em.createNamedQuery("User.searchByTerm", UserEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * TODO: Document deleteById().
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
