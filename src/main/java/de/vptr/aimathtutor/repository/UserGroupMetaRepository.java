package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserGroupMetaRepository extends AbstractRepository {

    public List<UserGroupMetaEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserId", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    public UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserAndGroup", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    public boolean isUserInGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.countByUserAndGroup", Long.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        return q.getSingleResult() > 0;
    }

    @Transactional
    public void persist(final UserGroupMetaEntity meta) {
        if (meta == null) {
            return;
        }
        this.em.persist(meta);
    }

    @Transactional
    public void delete(final UserGroupMetaEntity meta) {
        if (meta == null) {
            return;
        }
        final var managed = this.em.find(UserGroupMetaEntity.class, meta.id);
        if (managed != null) {
            this.em.remove(managed);
        }
    }
}
