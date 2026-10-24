package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entity representing metadata for user group memberships.
 */
@Entity
@Table(name = "user_groups_meta")
@NamedQueries({ @NamedQuery(name = "UserGroupMeta.countByUserAndGroup",
        query = "SELECT COUNT(m) FROM UserGroupMetaEntity m WHERE m.user.id = :u AND m.group.id = :g") })
public class UserGroupMetaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Nullable
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @Nullable
    public UserGroupEntity group;

    @Generated(event = EventType.INSERT)
    @Column(name = "created")
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;
}
