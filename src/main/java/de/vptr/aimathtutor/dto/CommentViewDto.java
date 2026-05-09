package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import jakarta.annotation.Nullable;

/**
 * View DTO for comments used in UI grids and panels.
 */
public class CommentViewDto {
    @Nullable
    public String publicId;
    @Nullable
    public String content;
    @Nullable
    public String exercisePublicId;
    @Nullable
    public String exerciseTitle;
    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    @Nullable
    public String parentPublicId;
    @Nullable
    public CommentStatus status;
    @Nullable
    public Integer flagsCount;
    @Nullable
    public String sessionId;
    @Nullable
    public String authorPublicId;

    // Internal numeric IDs for UI component use
    @Nullable
    public Long exerciseId;
    @Nullable
    public Long userId;
    @Nullable
    public Long parentId;
    @Nullable
    public Long authorId;

    /**
     * Default constructor for JSON mapping.
     */
    public CommentViewDto() {
    }

    /**
     * Constructs a CommentViewDto from a comment entity.
     *
     * @param entity
     *            the comment entity to convert
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
            this.exerciseId = entity.exercise.id;
        }

        if (entity.user != null) {
            this.userPublicId = entity.user.publicId;
            this.username = entity.user.username;
            this.authorPublicId = entity.user.publicId;
            this.userId = entity.user.id;
            this.authorId = entity.user.id;
        }

        if (entity.parentComment != null) {
            this.parentPublicId = entity.parentComment.publicId;
            this.parentId = entity.parentComment.id;
        }
    }

    /**
     * Converts this view DTO to a comment operation DTO.
     *
     * @return a {@link CommentDto} with the same data
     */
    public CommentDto toCommentDto() {
        final CommentDto dto = new CommentDto();
        dto.publicId = this.publicId;
        dto.content = this.content;
        dto.exercisePublicId = this.exercisePublicId;
        dto.exerciseId = this.exerciseId;
        dto.parentCommentPublicId = this.parentPublicId;
        dto.parentCommentId = this.parentId;
        dto.sessionId = this.sessionId;
        return dto;
    }
}
