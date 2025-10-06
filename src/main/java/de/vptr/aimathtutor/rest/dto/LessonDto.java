package de.vptr.aimathtutor.rest.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for lesson operations (POST, PUT, PATCH).
 * 
 * - POST: name required (validated by service), parentId optional
 * - PUT: name required (validated by service), parentId optional for parent
 * changes
 * - PATCH: name optional (allows null), parentId optional for parent changes
 */
public class LessonDto {

    public Long id;

    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters when provided")
    public String name;

    // Optional parentId for setting parent lesson
    // null = make it a root lesson
    // positive value = set to that parent
    public Long parentId;

    // Helper field for compatibility with old code that used nested objects
    public ParentField parent;

    /**
     * Helper class for nested parent field access
     */
    public static class ParentField {
        public Long id;

        public ParentField() {
        }

        public ParentField(final Long id) {
            this.id = id;
        }
    }

    /**
     * Ensure parentId and parent stay in sync
     */
    public void syncParent() {
        if (this.parent != null && this.parent.id != null) {
            this.parentId = this.parent.id;
        } else if (this.parentId != null) {
            this.parent = new ParentField(this.parentId);
        }
    }
}
