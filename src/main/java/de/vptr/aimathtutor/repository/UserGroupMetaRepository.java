package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public class UserGroupMetaRepository extends AbstractRepository {

    /**
     * TODO: Document findByUserId().
     */
    public List<UserGroupMetaEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserId", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * TODO: Document findByUserAndGroup().
     */
    public UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserAndGroup", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * TODO: Document isUserInGroup().
     */
    public boolean isUserInGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.countByUserAndGroup", Long.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        return q.getSingleResult() > 0;
    }

    /**
     * TODO: Document persist().
     */
    @Transactional
    public void persist(final UserGroupMetaEntity meta) {
        if (meta == null) {
            return;
        }
        this.em.persist(meta);
    }

    /**
     * TODO: Document delete().
     */
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
