package de.vptr.aimathtutor.rest.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "post_comments")
public class CommentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    public ExerciseEntity exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    public LocalDateTime created;

    // Helper method to find comments by exercise
    public static List<CommentEntity> findByExerciseId(final Long exerciseId) {
        return find("post.id", exerciseId).list();
    }

    // Helper method to find comments by user
    public static List<CommentEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find recent comments
    public static List<CommentEntity> findRecentComments(final int limit) {
        return find("ORDER BY created DESC").page(0, limit).list();
    }
}
