package de.vptr.aimathtutor.rest.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.rest.entity.PostEntity;

public class PostViewDto {

    public Long id;
    public String title;
    public String content;
    public Long userId;
    public String username;
    public Long categoryId;
    public String categoryName;
    public Boolean published;
    public Boolean commentable;
    public LocalDateTime created;
    public LocalDateTime lastEdit;
    public Long commentsCount;

    public PostViewDto() {
    }

    public PostViewDto(final PostEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.title = entity.title;
            this.content = entity.content;
            this.userId = entity.user != null ? entity.user.id : null;
            this.username = entity.user != null ? entity.user.username : null;
            this.categoryId = entity.category != null ? entity.category.id : null;
            this.categoryName = entity.category != null ? entity.category.name : null;
            this.published = entity.published;
            this.commentable = entity.commentable;
            this.created = entity.created;
            this.lastEdit = entity.lastEdit;
            this.commentsCount = entity.comments != null ? (long) entity.comments.size() : 0L;
        }
    }

    /**
     * Getter for name (returns title for compatibility)
     */
    public String getName() {
        return this.title;
    }

    /**
     * Getter for id
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Convert this ViewDto to a PostDto for create/update operations
     */
    public PostDto toPostDto() {
        final PostDto dto = new PostDto();
        dto.id = this.id;
        dto.title = this.title;
        dto.content = this.content;
        dto.userId = this.userId;
        dto.categoryId = this.categoryId;
        dto.published = this.published;
        dto.commentable = this.commentable;
        dto.created = this.created;
        dto.lastEdit = this.lastEdit;
        return dto;
    }
}
