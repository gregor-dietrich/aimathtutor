package de.vptr.aimathtutor.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "user_ranks")
public class UserRankEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    // View permissions
    @Column(name = "admin_view", columnDefinition = "TINYINT(1)")
    public Boolean adminView = false;

    // Page permissions
    @Column(name = "page_add", columnDefinition = "TINYINT(1)")
    public Boolean pageAdd = false;

    @Column(name = "page_delete", columnDefinition = "TINYINT(1)")
    public Boolean pageDelete = false;

    @Column(name = "page_edit", columnDefinition = "TINYINT(1)")
    public Boolean pageEdit = false;

    // Post permissions
    @Column(name = "post_add", columnDefinition = "TINYINT(1)")
    public Boolean postAdd = false;

    @Column(name = "post_delete", columnDefinition = "TINYINT(1)")
    public Boolean postDelete = false;

    @Column(name = "post_edit", columnDefinition = "TINYINT(1)")
    public Boolean postEdit = false;

    // Post category permissions
    @Column(name = "post_category_add", columnDefinition = "TINYINT(1)")
    public Boolean postCategoryAdd = false;

    @Column(name = "post_category_delete", columnDefinition = "TINYINT(1)")
    public Boolean postCategoryDelete = false;

    @Column(name = "post_category_edit", columnDefinition = "TINYINT(1)")
    public Boolean postCategoryEdit = false;

    // Post comment permissions
    @Column(name = "post_comment_add", columnDefinition = "TINYINT(1)")
    public Boolean postCommentAdd = false;

    @Column(name = "post_comment_delete", columnDefinition = "TINYINT(1)")
    public Boolean postCommentDelete = false;

    @Column(name = "post_comment_edit", columnDefinition = "TINYINT(1)")
    public Boolean postCommentEdit = false;

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

    // Account permissions
    @Column(name = "user_account_add", columnDefinition = "TINYINT(1)")
    public Boolean userAccountAdd = false;

    @Column(name = "user_account_delete", columnDefinition = "TINYINT(1)")
    public Boolean userAccountDelete = false;

    @Column(name = "user_account_edit", columnDefinition = "TINYINT(1)")
    public Boolean userAccountEdit = false;

    // User rank permissions
    @Column(name = "user_rank_add", columnDefinition = "TINYINT(1)")
    public Boolean userRankAdd = false;

    @Column(name = "user_rank_delete", columnDefinition = "TINYINT(1)")
    public Boolean userRankDelete = false;

    @Column(name = "user_rank_edit", columnDefinition = "TINYINT(1)")
    public Boolean userRankEdit = false;

    @OneToMany(mappedBy = "rank", cascade = CascadeType.ALL)
    @JsonIgnore
    public List<UserEntity> users;
}
