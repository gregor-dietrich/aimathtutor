package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserGroupEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserGroupRepository extends AbstractRepository {

    public List<UserGroupEntity> findAll() {
        return listNamed("UserGroup.findAll", UserGroupEntity.class);
    }

    public Optional<UserGroupEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    public UserGroupEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserGroupEntity.class, id);
    }

    public UserGroupEntity findByName(final String name) {
        final var q = this.em.createNamedQuery("UserGroup.findByName", UserGroupEntity.class);
        q.setParameter("n", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    public List<UserGroupEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("UserGroup.searchByName", UserGroupEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    @Transactional
    public void persist(final UserGroupEntity group) {
        if (group == null) {
            return;
        }
        this.em.persist(group);
    }

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
