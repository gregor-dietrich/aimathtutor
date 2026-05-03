package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing users in the system.
 */
@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "FROM UserEntity WHERE username = :u"),
        @NamedQuery(name = "User.findByEmail", query = "FROM UserEntity WHERE email = :e"),
        @NamedQuery(name = "User.findAllOrdered", query = "FROM UserEntity ORDER BY id DESC"),
        @NamedQuery(name = "User.findActive", query = "FROM UserEntity WHERE activated = true and banned = false ORDER BY id DESC"),
        @NamedQuery(name = "User.findByRankId", query = "FROM UserEntity WHERE rank.id = :r ORDER BY id DESC"),
        @NamedQuery(name = "User.searchByTerm", query = "FROM UserEntity WHERE LOWER(username) LIKE :s OR LOWER(email) LIKE :s ORDER BY id DESC"),
        @NamedQuery(name = "User.countByRankId", query = "SELECT COUNT(u) FROM UserEntity u WHERE u.rank.id = :r")
})
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_rank", columnList = "rank_id"),
        @Index(name = "idx_user_activated_banned", columnList = "activated, banned")
})
public class UserEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @NotBlank
    @Column(nullable = false, unique = true)
    public String username;

    @NotBlank
    @Column(nullable = false)
    @JsonIgnore
    public String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    @JsonIgnoreProperties({ "users" })
    public UserRankEntity rank;

    @Email
    @Column(unique = true)
    public String email;

    @Column(nullable = false)
    public Boolean banned = false;

    @Column(nullable = false)
    public Boolean activated = false;

    @Column(name = "activation_key")
    @JsonIgnore
    public String activationKey;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

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

    @OneToMany(mappedBy = "flagger", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    public List<CommentFlagEntity> commentFlags;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    public List<UserGroupMetaEntity> userGroupMetas;
}
