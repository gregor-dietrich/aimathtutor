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
     * TODO: Document getCommentId().
     */
    public Long getCommentId() {
        return this.commentId;
    }

    /**
     * TODO: Document getExerciseId().
     */
    public Long getExerciseId() {
        return this.exerciseId;
    }

    /**
     * TODO: Document getUserId().
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * TODO: Document getUsername().
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * TODO: Document getContent().
     */
    public String getContent() {
        return this.content;
    }

    /**
     * TODO: Document getCreatedAt().
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
}
