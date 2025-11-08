package de.vptr.aimathtutor.repository;

import java.util.List;

import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Repository for managing user group membership entities (UserGroupMeta).
 * Provides database access for user-group associations including
 * find by user, group, and membership checking operations.
 */
@ApplicationScoped
public class UserGroupMetaRepository extends AbstractRepository {

    /**
     * Retrieves all group memberships for a specific user.
     *
     * @param userId the user ID to filter by
     * @return a list of {@link UserGroupMetaEntity} objects representing group
     *         memberships
     */
    public List<UserGroupMetaEntity> findByUserId(final Long userId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserId", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        return q.getResultList();
    }

    /**
     * Retrieves the group membership record for a specific user and group.
     *
     * @param userId  the user ID to filter by
     * @param groupId the group ID to filter by
     * @return the {@link UserGroupMetaEntity} if the membership exists, null
     *         otherwise
     */
    public UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByUserAndGroup", UserGroupMetaEntity.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst().orElse(null);
    }

    /**
     * Checks if a user is a member of a specific group.
     *
     * @param userId  the user ID to check
     * @param groupId the group ID to check
     * @return true if the user is a member of the group, false otherwise
     */
    public boolean isUserInGroup(final Long userId, final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.countByUserAndGroup", Long.class);
        q.setParameter("u", userId);
        q.setParameter("g", groupId);
        return q.getSingleResult() > 0;
    }

    /**
     * Persists a user group membership entity to the database.
     *
     * @param meta the user group membership to persist; null values are ignored
     */
    @Transactional
    public void persist(final UserGroupMetaEntity meta) {
        if (meta == null) {
            return;
        }
        this.em.persist(meta);
    }

    /**
     * Deletes a user group membership entity from the database.
     *
     * @param meta the user group membership to delete; null values are ignored
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
