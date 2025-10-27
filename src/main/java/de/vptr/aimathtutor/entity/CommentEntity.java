package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@NamedQueries({
        @NamedQuery(name = "Comment.findAllOrdered", query = "FROM CommentEntity ORDER BY id DESC"),
        @NamedQuery(name = "Comment.findAllWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment ORDER BY c.id DESC"),
        @NamedQuery(name = "Comment.findByIdWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.id = :id"),
        @NamedQuery(name = "Comment.findByExerciseId", query = "FROM CommentEntity WHERE exercise.id = :e ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findByExerciseIdWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.exercise.id = :e ORDER BY c.created DESC"),
        @NamedQuery(name = "Comment.findByUserId", query = "FROM CommentEntity WHERE user.id = :u ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findByUserIdWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.user.id = :u ORDER BY c.created DESC"),
        @NamedQuery(name = "Comment.findRecent", query = "FROM CommentEntity ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findRecentWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment ORDER BY c.created DESC"),
        @NamedQuery(name = "Comment.findBySessionId", query = "FROM CommentEntity WHERE sessionId = :s ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findBySessionIdWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.sessionId = :s ORDER BY c.created DESC"),
        @NamedQuery(name = "Comment.findReplies", query = "FROM CommentEntity WHERE parentComment.id = :p ORDER BY created ASC"),
        @NamedQuery(name = "Comment.findRepliesWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.parentComment.id = :p AND c.status = 'VISIBLE' ORDER BY c.created ASC"),
        @NamedQuery(name = "Comment.findTopLevelByExercise", query = "FROM CommentEntity WHERE exercise.id = :e AND parentComment IS NULL AND status = 'VISIBLE' ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findRepliesPaged", query = "FROM CommentEntity WHERE parentComment.id = :p AND status = 'VISIBLE' ORDER BY created ASC"),
        @NamedQuery(name = "Comment.countByUserSince", query = "SELECT COUNT(c) FROM CommentEntity c WHERE user.id = :u AND created > :s"),
        @NamedQuery(name = "Comment.searchByTerm", query = "FROM CommentEntity WHERE content LIKE :s OR LOWER(user.username) LIKE :s"),
        @NamedQuery(name = "Comment.findByDateRange", query = "FROM CommentEntity WHERE created >= :s AND created <= :e"),
        @NamedQuery(name = "Comment.findByStatus", query = "FROM CommentEntity WHERE status = :st"),
        @NamedQuery(name = "Comment.findFlaggedComments", query = "FROM CommentEntity WHERE flagsCount >= :m ORDER BY flagsCount DESC")
})
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

    /**
     * TODO: Document findByExerciseId().
     */
    public static List<CommentEntity> findByExerciseId(final Long exerciseId) {
        return find("exercise.id = ?1 AND status != 'DELETED' ORDER BY id DESC", exerciseId).list();
    }

    // Helper method to find comments by user

    /**
     * TODO: Document findByUserId().
     */
    public static List<CommentEntity> findByUserId(final Long userId) {
        return find("user.id = ?1 AND status != 'DELETED' ORDER BY id DESC", userId).list();
    }

    // Helper method to find recent comments

    /**
     * TODO: Document findRecentComments().
     */
    public static List<CommentEntity> findRecentComments(final int limit) {
        return find("status != 'DELETED' ORDER BY id DESC").page(0, limit).list();
    }

    // NEW: Find replies to a comment

    /**
     * TODO: Document findReplies().
     */
    public static List<CommentEntity> findReplies(final Long parentCommentId) {
        return find("parentComment.id = ?1 AND status != 'DELETED' ORDER BY created ASC", parentCommentId).list();
    }

    // NEW: Find top-level comments for an exercise

    /**
     * TODO: Document findTopLevelByExercise().
     */
    public static List<CommentEntity> findTopLevelByExercise(final Long exerciseId) {
        return find("exercise.id = ?1 AND parentComment IS NULL AND status != 'DELETED' ORDER BY id DESC",
                exerciseId).list();
    }

    // NEW: Find comments by session

    /**
     * TODO: Document findBySessionId().
     */
    public static List<CommentEntity> findBySessionId(final String sessionId) {
        return find("sessionId = ?1 AND status != 'DELETED' ORDER BY id DESC", sessionId).list();
    }

    // NEW: Count flagged comments

    /**
     * TODO: Document findFlaggedCommentCount().
     */
    public static int findFlaggedCommentCount() {
        return (int) find("flagsCount > 0 AND status = 'VISIBLE'").count();
    }
}
