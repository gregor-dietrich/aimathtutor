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
        @NamedQuery(name = "UserGroupMeta.findByUserAndGroup", query = "FROM UserGroupMetaEntity WHERE user.id = :u AND m.group.id = :g"),
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
     * TODO: Document findByUserAndGroup().
     */
    public static UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        return find("user.id = ?1 AND group.id = ?2", userId, groupId).firstResult();
    }

    // Helper method to check if user is in group

    /**
     * TODO: Document isUserInGroup().
     */
    public static boolean isUserInGroup(final Long userId, final Long groupId) {
        return count("user.id = ?1 AND group.id = ?2", userId, groupId) > 0;
    }

    // Helper method to find all groups for a user

    /**
     * TODO: Document findByUserId().
     */
    public static List<UserGroupMetaEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find all users in a group

    /**
     * TODO: Document findByGroupId().
     */
    public static List<UserGroupMetaEntity> findByGroupId(final Long groupId) {
        return find("group.id", groupId).list();
    }
}
