package de.vptr.aimathtutor.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing user ranks in the system.
 */
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
    @Column(name = "admin_view")
    public Boolean adminView = false;

    // Exercise permissions
    @Column(name = "exercise_add")
    public Boolean exerciseAdd = false;

    @Column(name = "exercise_delete")
    public Boolean exerciseDelete = false;

    @Column(name = "exercise_edit")
    public Boolean exerciseEdit = false;

    // Lesson permissions
    @Column(name = "lesson_add")
    public Boolean lessonAdd = false;

    @Column(name = "lesson_delete")
    public Boolean lessonDelete = false;

    @Column(name = "lesson_edit")
    public Boolean lessonEdit = false;

    // Comment permissions
    @Column(name = "comment_add")
    public Boolean commentAdd = false;

    @Column(name = "comment_delete")
    public Boolean commentDelete = false;

    @Column(name = "comment_edit")
    public Boolean commentEdit = false;

    // User permissions
    @Column(name = "user_add")
    public Boolean userAdd = false;

    @Column(name = "user_delete")
    public Boolean userDelete = false;

    @Column(name = "user_edit")
    public Boolean userEdit = false;

    // User group permissions
    @Column(name = "user_group_add")
    public Boolean userGroupAdd = false;

    @Column(name = "user_group_delete")
    public Boolean userGroupDelete = false;

    @Column(name = "user_group_edit")
    public Boolean userGroupEdit = false;

    // User rank permissions
    @Column(name = "user_rank_add")
    public Boolean userRankAdd = false;

    @Column(name = "user_rank_delete")
    public Boolean userRankDelete = false;

    @Column(name = "user_rank_edit")
    public Boolean userRankEdit = false;

    @OneToMany(mappedBy = "rank")
    @JsonIgnore
    public List<UserEntity> users;
}
