package de.vptr.aimathtutor.rest.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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

    @Column(name = "last_ip", length = 45)
    public String lastIp;

    public LocalDateTime created;

    @Column(name = "last_login")
    public LocalDateTime lastLogin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    public List<PostEntity> posts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    public List<PostCommentEntity> comments;
}
