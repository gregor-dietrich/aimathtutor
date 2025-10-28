package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "FROM UserEntity WHERE username = :u"),
        @NamedQuery(name = "User.findByEmail", query = "FROM UserEntity WHERE email = :e"),
        @NamedQuery(name = "User.findAllOrdered", query = "FROM UserEntity ORDER BY id DESC"),
        @NamedQuery(name = "User.findActive", query = "FROM UserEntity WHERE activated = true and banned = false ORDER BY id DESC"),
        @NamedQuery(name = "User.findByRankId", query = "FROM UserEntity WHERE rank.id = :r ORDER BY id DESC"),
        @NamedQuery(name = "User.searchByTerm", query = "FROM UserEntity WHERE LOWER(username) LIKE :s OR LOWER(email) LIKE :s ORDER BY id DESC")
})
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true)
    public String username;

    @NotBlank
    public String password;

    @NotBlank
    public String salt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    @JsonIgnoreProperties({ "users" })
    public UserRankEntity rank;

    @Email
    @Column(unique = true)
    public String email;

    @Column(columnDefinition = "TINYINT(1)")
    public Boolean banned;

    @Column(columnDefinition = "TINYINT(1)")
    public Boolean activated;

    @Column(name = "activation_key")
    public String activationKey;

    public LocalDateTime created;

    @Column(name = "last_login")
    public LocalDateTime lastLogin;

    @Column(name = "user_avatar_emoji", length = 10)
    public String userAvatarEmoji;

    @Column(name = "tutor_avatar_emoji", length = 10)
    public String tutorAvatarEmoji;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    public List<ExerciseEntity> exercises;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    public List<CommentEntity> comments;
}
