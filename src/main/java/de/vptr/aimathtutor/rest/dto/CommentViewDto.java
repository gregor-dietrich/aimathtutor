package de.vptr.aimathtutor.rest.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.rest.entity.CommentEntity;

public class CommentViewDto {
    public Long id;
    public String content;
    public Long exerciseId;
    public String exerciseTitle;
    public Long userId;
    public String username;
    public LocalDateTime created;

    public CommentViewDto() {
    }

    public CommentViewDto(final CommentEntity entity) {
        this.id = entity.id;
        this.content = entity.content;
        this.created = entity.created;

        if (entity.exercise != null) {
            this.exerciseId = entity.exercise.id;
            this.exerciseTitle = entity.exercise.title;
        }

        if (entity.user != null) {
            this.userId = entity.user.id;
            this.username = entity.user.username;
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
