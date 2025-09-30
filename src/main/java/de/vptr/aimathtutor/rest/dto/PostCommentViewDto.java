package de.vptr.aimathtutor.rest.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.rest.entity.PostCommentEntity;

public class PostCommentViewDto {
    public Long id;
    public String content;
    public Long postId;
    public String postTitle;
    public Long userId;
    public String username;
    public LocalDateTime created;

    public PostCommentViewDto() {
    }

    public PostCommentViewDto(final PostCommentEntity entity) {
        this.id = entity.id;
        this.content = entity.content;
        this.created = entity.created;

        if (entity.post != null) {
            this.postId = entity.post.id;
            this.postTitle = entity.post.title;
        }

        if (entity.user != null) {
            this.userId = entity.user.id;
            this.username = entity.user.username;
        }
    }

    /**
     * Convert this ViewDto to a PostCommentDto for create/update operations
     */
    public PostCommentDto toPostCommentDto() {
        final PostCommentDto dto = new PostCommentDto();
        dto.id = this.id;
        dto.content = this.content;
        dto.postId = this.postId;
        return dto;
    }
}
