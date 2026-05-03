package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Entity representing metadata for user group memberships.
 */
@Entity
@Table(name = "user_groups_meta", indexes = {
        @Index(name = "idx_ugm_group_user", columnList = "group_id, user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_ugm_group_user", columnNames = { "group_id", "user_id" })
})
@NamedQueries({
        @NamedQuery(name = "UserGroupMeta.findByUserId", query = "FROM UserGroupMetaEntity WHERE user.id = :u"),
        @NamedQuery(name = "UserGroupMeta.findByUserAndGroup", query = "FROM UserGroupMetaEntity m WHERE m.user.id = :u AND m.group.id = :g"),
        @NamedQuery(name = "UserGroupMeta.countByUserAndGroup", query = "SELECT COUNT(m) FROM UserGroupMetaEntity m WHERE m.user.id = :u AND m.group.id = :g"),
        @NamedQuery(name = "UserGroupMeta.findByGroupId", query = "FROM UserGroupMetaEntity WHERE group.id = :g"),
        @NamedQuery(name = "UserGroupMeta.findByGroupIdWithUsers", query = "SELECT m FROM UserGroupMetaEntity m LEFT JOIN FETCH m.user WHERE m.group.id = :g")
})
public class UserGroupMetaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    public UserGroupEntity group;

    @Generated(event = EventType.INSERT)
    @Column(name = "created")
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

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

}
