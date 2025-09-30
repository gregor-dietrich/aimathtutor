package de.vptr.aimathtutor.rest.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

public class PostDto {

    public Long id;

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    public String title;

    @Size(min = 1, message = "Content must not be empty")
    public String content;

    public Long userId;

    public Long categoryId;

    public Boolean published;

    public Boolean commentable;

    public LocalDateTime created;

    public LocalDateTime lastEdit;

    // Helper fields for compatibility with old code that used nested objects
    public UserField user;
    public CategoryField category;

    public PostDto() {
    }

    public PostDto(final String title, final String content, final Long userId, final Long categoryId,
            final Boolean published, final Boolean commentable) {
        this.title = title;
        this.content = content;
        this.userId = userId;
        this.categoryId = categoryId;
        this.published = published;
        this.commentable = commentable;
    }

    /**
     * Helper classes for nested field access
     */
    public static class UserField {
        public Long id;
        public String username;

        public UserField() {
        }

        public UserField(final Long id) {
            this.id = id;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    public static class CategoryField {
        public Long id;
        public String name;

        public CategoryField() {
        }

        public CategoryField(final Long id) {
            this.id = id;
        }
    }

    /**
     * Ensure userId/categoryId and nested objects stay in sync
     */
    public void syncNestedFields() {
        if (this.user != null && this.user.id != null) {
            this.userId = this.user.id;
        } else if (this.userId != null && this.user == null) {
            this.user = new UserField(this.userId);
        }

        if (this.category != null && this.category.id != null) {
            this.categoryId = this.category.id;
        } else if (this.categoryId != null && this.category == null) {
            this.category = new CategoryField(this.categoryId);
        }
    }
}
