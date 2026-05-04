package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;

/**
 * View DTO for comments used in UI grids and panels.
 */
public class CommentViewDto {
    public Long id;
    public String content;
    public Long exerciseId;
    public String exerciseTitle;
    public Long userId;
    public String username;
    public LocalDateTime created;
    public LocalDateTime lastEdit;

    public Long parentId;
    public CommentStatus status; // VISIBLE, HIDDEN, DELETED
    public Integer flagsCount;
    public String sessionId;
    public Long authorId;

    public CommentViewDto() {
    }

    /**
     * Constructs a CommentViewDto from a CommentEntity.
     */
    public CommentViewDto(final CommentEntity entity) {
        this.id = entity.id;
        this.content = entity.content;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
        this.status = entity.status != null ? entity.status : CommentStatus.VISIBLE;
        this.flagsCount = entity.flagsCount;
        this.sessionId = entity.sessionId;

        if (entity.exercise != null) {
            this.exerciseId = entity.exercise.id;
            this.exerciseTitle = entity.exercise.title;
        }

        if (entity.user != null) {
            this.userId = entity.user.id;
            this.username = entity.user.username;
            this.authorId = entity.user.id;
        }

        if (entity.parentComment != null) {
            this.parentId = entity.parentComment.id;
        }
    }

    /**
     * Convert this ViewDto to a CommentDto for create/update operations.
     * Note: {@code status} is intentionally omitted because moderation state is
     * managed separately and is not part of the mutation DTO.
     */
    public CommentDto toCommentDto() {
        final CommentDto dto = new CommentDto();
        dto.id = this.id;
        dto.content = this.content;
        dto.exerciseId = this.exerciseId;
        dto.parentCommentId = this.parentId;
        dto.sessionId = this.sessionId;
        return dto;
    }
}
