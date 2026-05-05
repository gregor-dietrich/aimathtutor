package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Size;

/**
 * DTO for lesson operations (POST, PUT, PATCH).
 * 
 * - POST: name required (validated by service), parentId optional
 * - PUT: name required (validated by service), parentId optional for parent
 * changes
 * - PATCH: name optional (allows null), parentId optional for parent changes
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "DTO public fields intentionally used for JSON mapping and convenience")
public class LessonDto {

    public String publicId;

    @Size(min = AppConstants.LESSON_NAME_MIN_LENGTH, max = AppConstants.LESSON_NAME_MAX_LENGTH, message = "Name must be between {min} and {max} characters")
    public String name;

    public String parentPublicId;

    public ParentField parent;

    /**
     * Nested field representing a parent lesson reference.
     */
    public static class ParentField {
        public String publicId;

        /**
         * Default constructor for JSON mapping.
         */
        public ParentField() {
        }

        /**
         * Constructs a ParentField with the given public ID.
         *
         * @param publicId the parent lesson's public identifier
         */
        public ParentField(final String publicId) {
            this.publicId = publicId;
        }
    }

    /**
     * Synchronizes the nested parent field with the flat parentPublicId field.
     */
    public void syncParent() {
        if (this.parent != null && this.parent.publicId != null) {
            this.parentPublicId = this.parent.publicId;
        } else if (this.parentPublicId != null) {
            this.parent = new ParentField(this.parentPublicId);
        }
    }
}
