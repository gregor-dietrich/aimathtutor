package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;

/**
 * View DTO for comments used in UI grids and panels.
 */
public class CommentViewDto {
    public String publicId;
    public String content;
    public String exercisePublicId;
    public String exerciseTitle;
    public String userPublicId;
    public String username;
    public LocalDateTime created;
    public LocalDateTime lastEdit;

    public String parentPublicId;
    public CommentStatus status; // VISIBLE, HIDDEN, DELETED
    public Integer flagsCount;
    public String sessionId;
    public String authorPublicId;

    public CommentViewDto() {
    }

    /**
     * Constructs a CommentViewDto from a CommentEntity.
     */
    public CommentViewDto(final CommentEntity entity) {
        this.publicId = entity.publicId;
        this.content = entity.content;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
        this.status = entity.status != null ? entity.status : CommentStatus.VISIBLE;
        this.flagsCount = entity.flagsCount;
        this.sessionId = entity.sessionId;

        if (entity.exercise != null) {
            this.exercisePublicId = entity.exercise.publicId;
            this.exerciseTitle = entity.exercise.title;
        }

        if (entity.user != null) {
            this.userPublicId = entity.user.publicId;
            this.username = entity.user.username;
            this.authorPublicId = entity.user.publicId;
        }

        if (entity.parentComment != null) {
            this.parentPublicId = entity.parentComment.publicId;
        }
    }

    /**
     * Convert this ViewDto to a CommentDto for create/update operations.
     * Note: {@code status} is intentionally omitted because moderation state is
     * managed separately and is not part of the mutation DTO.
     */
    public CommentDto toCommentDto() {
        final CommentDto dto = new CommentDto();
        dto.publicId = this.publicId;
        dto.content = this.content;
        dto.exercisePublicId = this.exercisePublicId;
        dto.parentCommentPublicId = this.parentPublicId;
        dto.sessionId = this.sessionId;
        return dto;
    }
}
