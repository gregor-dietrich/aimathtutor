package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

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
     * Retrieves a user group membership by its public identifier.
     *
     * @param publicId the public ID of the membership
     * @return an {@link Optional} containing the membership if found, empty otherwise
     */
    public Optional<UserGroupMetaEntity> findByPublicId(final String publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("UserGroupMeta.findByPublicId", UserGroupMetaEntity.class);
        q.setParameter("p", publicId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves all user group memberships for a user by their public identifier.
     *
     * @param userPublicId the public ID of the user
     * @return a list of all memberships for the given user, empty list if not found
     */
    public List<UserGroupMetaEntity> findByUserPublicId(final String userPublicId) {
        if (userPublicId == null) {
            return List.of();
        }
        final var q = this.em.createQuery(
                "FROM UserGroupMetaEntity m WHERE m.user.publicId = :p", UserGroupMetaEntity.class);
        q.setParameter("p", userPublicId);
        return q.getResultList();
    }

    /**
     * Retrieves all user group memberships for a specific group.
     *
     * @param groupPublicId the public ID of the group
     * @return a list of memberships for the group
     */
    public List<UserGroupMetaEntity> findByGroupPublicId(final String groupPublicId) {
        if (groupPublicId == null) {
            return List.of();
        }
        final var q = this.em.createQuery(
                "FROM UserGroupMetaEntity m WHERE m.group.publicId = :p", UserGroupMetaEntity.class);
        q.setParameter("p", groupPublicId);
        return q.getResultList();
    }

    /**
     * Retrieves all group memberships for a specific user with users eagerly
     * loaded.
     *
     * @param groupId the group ID to filter by
     * @return a list of {@link UserGroupMetaEntity} objects representing group
     *         members with loaded user data
     */
    public List<UserGroupMetaEntity> findByGroupIdWithUsers(final Long groupId) {
        final var q = this.em.createNamedQuery("UserGroupMeta.findByGroupIdWithUsers", UserGroupMetaEntity.class);
        q.setParameter("g", groupId);
        return q.getResultList();
    }

    /**
     * Retrieves all group memberships for a specific group with users eagerly
     * loaded, using public ID.
     *
     * @param groupPublicId the group public ID to filter by
     * @return a list of {@link UserGroupMetaEntity} objects representing group
     *         members with loaded user data
     */
    public List<UserGroupMetaEntity> findByGroupPublicIdWithUsers(final String groupPublicId) {
        if (groupPublicId == null) {
            return List.of();
        }
        final var q = this.em.createQuery(
                "SELECT m FROM UserGroupMetaEntity m JOIN FETCH m.user WHERE m.group.publicId = :p",
                UserGroupMetaEntity.class);
        q.setParameter("p", groupPublicId);
        return q.getResultList();
    }

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
     * Retrieves the group membership record for a specific user and group using public IDs.
     *
     * @param userPublicId  the user public ID to filter by
     * @param groupPublicId the group public ID to filter by
     * @return the {@link UserGroupMetaEntity} if the membership exists, null
     *         otherwise
     */
    public UserGroupMetaEntity findByUserPublicIdAndGroupPublicId(final String userPublicId, final String groupPublicId) {
        if (userPublicId == null || groupPublicId == null) {
            return null;
        }
        final var q = this.em.createQuery(
                "FROM UserGroupMetaEntity m WHERE m.user.publicId = :u AND m.group.publicId = :g",
                UserGroupMetaEntity.class);
        q.setParameter("u", userPublicId);
        q.setParameter("g", groupPublicId);
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
