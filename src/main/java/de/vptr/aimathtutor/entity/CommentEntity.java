package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "comments")
public class CommentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    public ExerciseEntity exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    public LocalDateTime created;

    // NEW: Threading support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    public CommentEntity parentComment;

    // NEW: Moderation support
    @Column(length = 20)
    public String status = "VISIBLE"; // VISIBLE, HIDDEN, DELETED

    @Column(name = "flags_count")
    public Integer flagsCount = 0;

    @Column(name = "session_id", length = 255)
    public String sessionId;

    @Column(name = "edited_at")
    public LocalDateTime editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    public UserEntity deletedBy;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;

    // Helper method to find comments by exercise
    public static List<CommentEntity> findByExerciseId(final Long exerciseId) {
        return find("exercise.id = ?1 AND status != 'DELETED' ORDER BY id DESC", exerciseId).list();
    }

    // Helper method to find comments by user
    public static List<CommentEntity> findByUserId(final Long userId) {
        return find("user.id = ?1 AND status != 'DELETED' ORDER BY id DESC", userId).list();
    }

    // Helper method to find recent comments
    public static List<CommentEntity> findRecentComments(final int limit) {
        return find("status != 'DELETED' ORDER BY id DESC").page(0, limit).list();
    }

    // NEW: Find replies to a comment
    public static List<CommentEntity> findReplies(final Long parentCommentId) {
        return find("parentComment.id = ?1 AND status != 'DELETED' ORDER BY created ASC", parentCommentId).list();
    }

    // NEW: Find top-level comments for an exercise
    public static List<CommentEntity> findTopLevelByExercise(final Long exerciseId) {
        return find("exercise.id = ?1 AND parentComment IS NULL AND status != 'DELETED' ORDER BY id DESC",
                exerciseId).list();
    }

    // NEW: Find comments by session
    public static List<CommentEntity> findBySessionId(final String sessionId) {
        return find("sessionId = ?1 AND status != 'DELETED' ORDER BY id DESC", sessionId).list();
    }

    // NEW: Count flagged comments
    public static int findFlaggedCommentCount() {
        return (int) find("flagsCount > 0 AND status = 'VISIBLE'").count();
    }
}
