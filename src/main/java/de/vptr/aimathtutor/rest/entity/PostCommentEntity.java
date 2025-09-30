package de.vptr.aimathtutor.rest.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "post_comments")
public class PostCommentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    public PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    public LocalDateTime created;

    // Helper method to find comments by post
    public static List<PostCommentEntity> findByPostId(final Long postId) {
        return find("post.id", postId).list();
    }

    // Helper method to find comments by user
    public static List<PostCommentEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find recent comments
    public static List<PostCommentEntity> findRecentComments(final int limit) {
        return find("ORDER BY created DESC").page(0, limit).list();
    }
}
