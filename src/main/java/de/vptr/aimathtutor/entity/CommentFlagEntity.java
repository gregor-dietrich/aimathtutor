package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/**
 * CommentFlagEntity: Tracks which users have flagged which comments.
 * Prevents users from flagging the same comment multiple times.
 */
@Entity
@Table(name = "comment_flags", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "comment_id", "flagger_id" }, name = "uk_comment_flags_unique")
})
@NamedQueries({
        @NamedQuery(name = "CommentFlag.countByCommentAndFlagger", query = "SELECT COUNT(f) FROM CommentFlagEntity f WHERE f.comment.id = :c AND f.flagger.id = :u"),
        @NamedQuery(name = "CommentFlag.findByComment", query = "FROM CommentFlagEntity WHERE comment.id = :c")
})
public class CommentFlagEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    public CommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flagger_id", nullable = false)
    public UserEntity flagger;

    @Column(name = "created")
    public LocalDateTime created;

    /**
     * Check if a user has already flagged a specific comment
     */
    public static boolean hasUserFlaggedComment(final Long commentId, final Long flaggerId) {
        return find("comment.id = ?1 AND flagger.id = ?2", commentId, flaggerId).count() > 0;
    }

    /**
     * Get all flags for a comment
     */
    public static int countFlagsForComment(final Long commentId) {
        return (int) find("comment.id", commentId).count();
    }
}
