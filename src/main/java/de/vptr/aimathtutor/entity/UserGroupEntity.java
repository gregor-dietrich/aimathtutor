package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing user groups in the system.
 */
@Entity
@Table(name = "user_groups")
@NamedQueries({
        @NamedQuery(name = "UserGroup.findAll", query = "FROM UserGroupEntity ORDER BY id DESC"),
        @NamedQuery(name = "UserGroup.findByName", query = "FROM UserGroupEntity WHERE name = :n"),
        @NamedQuery(name = "UserGroup.searchByName", query = "FROM UserGroupEntity WHERE LOWER(name) LIKE :s ORDER BY id DESC")
})
public class UserGroupEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "group", cascade = CascadeType.REMOVE, orphanRemoval = true)
    public List<UserGroupMetaEntity> userGroupMetas;

    // Helper method to get users in this group

    /**
     * Retrieves all users that are members of this group.
     *
     * @return a list of {@link UserEntity} objects that are members of this group
     */
    public List<UserEntity> getUsers() {
        return this.userGroupMetas.stream()
                .map(meta -> meta.user)
                .toList();
    }

    // Helper method to get user count in this group

    /**
     * Counts the number of users that are members of this group.
     *
     * @return the number of members in this group, or 0 if no members or metadata
     *         is null
     */
    public long getUserCount() {
        return this.userGroupMetas != null ? this.userGroupMetas.size() : 0;
    }
}
