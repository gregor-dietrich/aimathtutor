package de.vptr.aimathtutor.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "user_ranks")
@NamedQueries({
        @NamedQuery(name = "UserRank.findAll", query = "FROM UserRankEntity ORDER BY id DESC"),
        @NamedQuery(name = "UserRank.findByName", query = "FROM UserRankEntity WHERE name = :n"),
        @NamedQuery(name = "UserRank.searchByName", query = "FROM UserRankEntity WHERE LOWER(name) LIKE :s ORDER BY id DESC")
})
public class UserRankEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    // View permissions
    @Column(name = "admin_view", columnDefinition = "TINYINT(1)")
    public Boolean adminView = false;

    // Exercise permissions
    @Column(name = "exercise_add", columnDefinition = "TINYINT(1)")
    public Boolean exerciseAdd = false;

    @Column(name = "exercise_delete", columnDefinition = "TINYINT(1)")
    public Boolean exerciseDelete = false;

    @Column(name = "exercise_edit", columnDefinition = "TINYINT(1)")
    public Boolean exerciseEdit = false;

    // Lesson permissions
    @Column(name = "lesson_add", columnDefinition = "TINYINT(1)")
    public Boolean lessonAdd = false;

    @Column(name = "lesson_delete", columnDefinition = "TINYINT(1)")
    public Boolean lessonDelete = false;

    @Column(name = "lesson_edit", columnDefinition = "TINYINT(1)")
    public Boolean lessonEdit = false;

    // Comment permissions
    @Column(name = "comment_add", columnDefinition = "TINYINT(1)")
    public Boolean commentAdd = false;

    @Column(name = "comment_delete", columnDefinition = "TINYINT(1)")
    public Boolean commentDelete = false;

    @Column(name = "comment_edit", columnDefinition = "TINYINT(1)")
    public Boolean commentEdit = false;

    // User permissions
    @Column(name = "user_add", columnDefinition = "TINYINT(1)")
    public Boolean userAdd = false;

    @Column(name = "user_delete", columnDefinition = "TINYINT(1)")
    public Boolean userDelete = false;

    @Column(name = "user_edit", columnDefinition = "TINYINT(1)")
    public Boolean userEdit = false;

    // User group permissions
    @Column(name = "user_group_add", columnDefinition = "TINYINT(1)")
    public Boolean userGroupAdd = false;

    @Column(name = "user_group_delete", columnDefinition = "TINYINT(1)")
    public Boolean userGroupDelete = false;

    @Column(name = "user_group_edit", columnDefinition = "TINYINT(1)")
    public Boolean userGroupEdit = false;

    // User rank permissions
    @Column(name = "user_rank_add", columnDefinition = "TINYINT(1)")
    public Boolean userRankAdd = false;

    @Column(name = "user_rank_delete", columnDefinition = "TINYINT(1)")
    public Boolean userRankDelete = false;

    @Column(name = "user_rank_edit", columnDefinition = "TINYINT(1)")
    public Boolean userRankEdit = false;

    @OneToMany(mappedBy = "rank")
    @JsonIgnore
    public List<UserEntity> users;
}
