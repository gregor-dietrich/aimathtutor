package de.vptr.aimathtutor.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
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

    @NotBlank
    public String name;

    @OneToMany(mappedBy = "group")
    public List<UserGroupMetaEntity> userGroupMetas;

    // Helper method to find group by name

    /**
     * Finds a user group by its name.
     *
     * @param name the name of the group to find
     * @return the {@link UserGroupEntity} if found, null otherwise
     */
    public static UserGroupEntity findByName(final String name) {
        return find("name", name).firstResult();
    }

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
