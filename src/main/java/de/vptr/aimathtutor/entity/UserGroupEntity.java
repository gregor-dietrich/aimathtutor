package de.vptr.aimathtutor.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "user_groups")
public class UserGroupEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    public List<UserGroupMetaEntity> userGroupMetas;

    // Helper method to find group by name
    public static UserGroupEntity findByName(final String name) {
        return find("name", name).firstResult();
    }

    // Helper method to get users in this group
    public List<UserEntity> getUsers() {
        return this.userGroupMetas.stream()
                .map(meta -> meta.user)
                .toList();
    }

    // Helper method to get user count in this group
    public long getUserCount() {
        return this.userGroupMetas != null ? this.userGroupMetas.size() : 0;
    }
}
