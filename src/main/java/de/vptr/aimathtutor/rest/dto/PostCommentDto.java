package de.vptr.aimathtutor.rest.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for post comment operations (POST, PUT, PATCH).
 * 
 * - POST: content required (validated by service), postId required
 * - PUT: content required (validated by service), postId ignored (from URL)
 * - PATCH: content optional (allows null), postId ignored (from URL)
 */
public class PostCommentDto {

    public Long id;

    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters when provided")
    public String content;

    // Required for POST operations (creation)
    // Ignored for PUT/PATCH operations (postId comes from the URL path)
    public Long postId;

    // Helper field for compatibility with old code that used nested objects
    public PostField post;

    /**
     * Helper class for nested post field access
     */
    public static class PostField {
        public Long id;

        public PostField() {
        }

        public PostField(final Long id) {
            this.id = id;
        }
    }

    /**
     * Ensure postId and post stay in sync
     */
    public void syncPost() {
        if (this.post != null && this.post.id != null) {
            this.postId = this.post.id;
        } else if (this.postId != null) {
            this.post = new PostField(this.postId);
        }
    }
}
