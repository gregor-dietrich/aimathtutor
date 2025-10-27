package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.CommentEntity;

/**
 * TODO: Class documentation.
 */
public class CommentViewDto {
    public Long id;
    public String content;
    public Long exerciseId;
    public String exerciseTitle;
    public Long userId;
    public String username;
    public LocalDateTime created;

    public Long parentId;
    public String status; // VISIBLE, HIDDEN, DELETED
    public Integer flagsCount;
    public String sessionId;
    public LocalDateTime editedAt;
    public Long authorId;

    public CommentViewDto() {
    }

    public CommentViewDto(final CommentEntity entity) {
        this.id = entity.id;
        this.content = entity.content;
        this.created = entity.created;
        this.status = entity.status != null ? entity.status : "VISIBLE";
        this.flagsCount = entity.flagsCount != null ? entity.flagsCount : 0;
        this.sessionId = entity.sessionId;
        this.editedAt = entity.editedAt;

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
     * Convert this ViewDto to a CommentDto for create/update operations
     */
    public CommentDto toCommentDto() {
        final CommentDto dto = new CommentDto();
        dto.id = this.id;
        dto.content = this.content;
        dto.exerciseId = this.exerciseId;
        return dto;
    }
}
