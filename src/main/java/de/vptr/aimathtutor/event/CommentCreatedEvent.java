package de.vptr.aimathtutor.event;

import java.time.LocalDateTime;

/**
 * CDI Event fired when a comment is created.
 * Used for real-time updates via Vaadin Push.
 */
public class CommentCreatedEvent {

    public final Long commentId;
    public final Long exerciseId;
    public final Long userId;
    public final String username;
    public final String content;
    public final LocalDateTime createdAt;

    /**
     * Constructs a CommentCreatedEvent with the specified parameters.
     */
    public CommentCreatedEvent(final Long commentId, final Long exerciseId, final Long userId,
            final String username, final String content, final LocalDateTime createdAt) {
        this.commentId = commentId;
        this.exerciseId = exerciseId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.createdAt = createdAt;
    }

    /**
     * Get the id of the created comment.
     */
    public Long getCommentId() {
        return this.commentId;
    }

    /**
     * Get the exercise id the comment belongs to.
     */
    public Long getExerciseId() {
        return this.exerciseId;
    }

    /**
     * Get the id of the user who created the comment.
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * Get the username of the comment author.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Get the comment content text.
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Get the timestamp when the comment was created.
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
}
