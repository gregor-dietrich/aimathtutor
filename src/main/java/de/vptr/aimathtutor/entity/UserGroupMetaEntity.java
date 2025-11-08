package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * Entity representing metadata for user group memberships.
 */
@Entity
@Table(name = "user_groups_meta")
@NamedQueries({
        @NamedQuery(name = "UserGroupMeta.findByUserId", query = "FROM UserGroupMetaEntity WHERE user.id = :u"),
        @NamedQuery(name = "UserGroupMeta.findByUserAndGroup", query = "FROM UserGroupMetaEntity m WHERE m.user.id = :u AND m.group.id = :g"),
        @NamedQuery(name = "UserGroupMeta.countByUserAndGroup", query = "SELECT COUNT(m) FROM UserGroupMetaEntity m WHERE m.user.id = :u AND m.group.id = :g"),
        @NamedQuery(name = "UserGroupMeta.findByGroupId", query = "FROM UserGroupMetaEntity WHERE group.id = :g")
})
public class UserGroupMetaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    public UserGroupEntity group;

    public LocalDateTime timestamp;

    // Helper method to find by user and group

    /**
     * Finds a user's membership record in a specific group.
     *
     * @param userId  the ID of the user to find
     * @param groupId the ID of the group to search in
     * @return the {@link UserGroupMetaEntity} if the membership exists, null
     *         otherwise
     */
    public static UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        return find("user.id = ?1 AND group.id = ?2", userId, groupId).firstResult();
    }

    // Helper method to check if user is in group

    /**
     * Checks if a user is a member of a specific group.
     *
     * @param userId  the ID of the user to check
     * @param groupId the ID of the group to check
     * @return true if the user is a member of the group, false otherwise
     */
    public static boolean isUserInGroup(final Long userId, final Long groupId) {
        return count("user.id = ?1 AND group.id = ?2", userId, groupId) > 0;
    }

    // Helper method to find all groups for a user

    /**
     * Finds all group memberships for a specific user.
     *
     * @param userId the ID of the user to find memberships for
     * @return a list of {@link UserGroupMetaEntity} objects representing all group
     *         memberships
     */
    public static List<UserGroupMetaEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find all users in a group

    /**
     * Finds all users that are members of a specific group.
     *
     * @param groupId the ID of the group to find members for
     * @return a list of {@link UserGroupMetaEntity} objects representing all
     *         members in the group
     */
    public static List<UserGroupMetaEntity> findByGroupId(final Long groupId) {
        return find("group.id", groupId).list();
    }
}
