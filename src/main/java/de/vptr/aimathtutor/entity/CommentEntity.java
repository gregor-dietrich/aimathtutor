package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing comments on exercises.
 */
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
        @NamedQuery(name = "Comment.findReplies", query = "FROM CommentEntity WHERE parentComment.id = :p AND status = 'VISIBLE' ORDER BY created ASC"),
        @NamedQuery(name = "Comment.findRepliesWithRelations", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE c.parentComment.id = :p AND c.status = 'VISIBLE' ORDER BY c.created ASC"),
        @NamedQuery(name = "Comment.findRepliesPaged", query = "FROM CommentEntity WHERE parentComment.id = :p AND status = 'VISIBLE' ORDER BY created ASC"),
        @NamedQuery(name = "Comment.findTopLevelByExercise", query = "FROM CommentEntity WHERE exercise.id = :e AND parentComment IS NULL AND status = 'VISIBLE' ORDER BY id DESC"),
        @NamedQuery(name = "Comment.countByUserSince", query = "SELECT COUNT(c) FROM CommentEntity c WHERE c.user.id = :u AND c.created >= :s"),
        @NamedQuery(name = "Comment.searchByTerm", query = "SELECT c FROM CommentEntity c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.exercise LEFT JOIN FETCH c.parentComment WHERE LOWER(c.content) LIKE LOWER(:s) AND c.status != 'DELETED' ORDER BY c.created DESC"),
        @NamedQuery(name = "Comment.findByDateRange", query = "FROM CommentEntity WHERE created BETWEEN :s AND :e ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findByStatus", query = "FROM CommentEntity WHERE status = :st ORDER BY created DESC"),
        @NamedQuery(name = "Comment.findFlaggedComments", query = "FROM CommentEntity WHERE flagsCount >= :m AND status = 'VISIBLE' ORDER BY flagsCount DESC"),
})
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_exercise_status_created", columnList = "exercise_id, status, created"),
        @Index(name = "idx_comment_user_created", columnList = "user_id, created"),
        @Index(name = "idx_comment_parent_status_created", columnList = "parent_comment_id, status, created"),
        @Index(name = "idx_comment_status_created", columnList = "status, created"),
        @Index(name = "idx_comment_session_created", columnList = "session_id, created"),
        @Index(name = "idx_comment_flags_status", columnList = "flags_count, status")
})
public class CommentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    public ExerciseEntity exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    // NEW: Threading support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    public CommentEntity parentComment;

    // NEW: Moderation support
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    public CommentStatus status = CommentStatus.VISIBLE; // VISIBLE, HIDDEN, DELETED

    @Column(name = "flags_count")
    public int flagsCount = 0;

    @Column(name = "session_id", length = 255)
    public String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    public UserEntity deletedBy;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;

    @Column(name = "moderation_reason", length = 500)
    public String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    public UserEntity moderator;

    @Column(name = "moderation_action", length = 20)
    public String moderationAction;

    @Column(name = "moderated_at")
    public LocalDateTime moderatedAt;
}
