package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserGroupEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public class UserGroupRepository extends AbstractRepository {

    /**
     * TODO: Document findAll().
     */
    public List<UserGroupEntity> findAll() {
        return this.listNamed("UserGroup.findAll", UserGroupEntity.class);
    }

    /**
     * TODO: Document findByIdOptional().
     */
    public Optional<UserGroupEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * TODO: Document findById().
     */
    public UserGroupEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserGroupEntity.class, id);
    }

    /**
     * TODO: Document findByName().
     */
    public UserGroupEntity findByName(final String name) {
        final var q = this.em.createNamedQuery("UserGroup.findByName", UserGroupEntity.class);
        q.setParameter("n", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * TODO: Document search().
     */
    public List<UserGroupEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("UserGroup.searchByName", UserGroupEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    /**
     * TODO: Document persist().
     */
    @Transactional
    public void persist(final UserGroupEntity group) {
        if (group == null) {
            return;
        }
        this.em.persist(group);
    }

    /**
     * TODO: Document deleteById().
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
