package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import de.vptr.aimathtutor.util.AppConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Size;

/**
 * DTO for exercise comment operations (POST, PUT, PATCH).
 * 
 * - POST: content required (validated by service), exerciseId required
 * - PUT: content required (validated by service), exerciseId ignored (from URL)
 * - PATCH: content optional (allows null), exerciseId ignored (from URL)
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "DTO public fields intentionally used for JSON mapping and simplicity")
public class CommentDto {

    /**
     * Enumeration of comment statuses.
     * Maps to string values stored in the database and used in UI components.
     */
    public enum CommentStatus {
        VISIBLE("VISIBLE"),
        HIDDEN("HIDDEN"),
        DELETED("DELETED");

        private final String value;

        CommentStatus(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        /**
         * Converts a string value to the corresponding CommentStatus enum.
         *
         * @param value the string value to convert
         * @return the matching CommentStatus, or null if no match
         */
        public static CommentStatus fromString(final String value) {
            if (value == null) {
                return null;
            }
            for (final CommentStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    public String publicId;

    @Size(min = AppConstants.COMMENT_CONTENT_MIN_LENGTH, max = AppConstants.COMMENT_CONTENT_MAX_LENGTH, message = "Content must be between {min} and {max} characters")
    public String content;

    // Required for POST operations (creation)
    // Ignored for PUT/PATCH operations (exerciseId comes from the URL path)
    public String exercisePublicId;

    // Internal numeric ID for UI component use
    public Long exerciseId;

    // NEW: For threading support
    public String parentCommentPublicId;

    // Internal numeric ID for UI component use
    public Long parentCommentId;

    // NEW: For lesson comments (future extension)
    public String lessonPublicId;

    // Internal numeric ID for UI component use
    public Long lessonId;

    // NEW: For tracking which session the comment was made during
    public String sessionId;

    // Helper field for compatibility with old code that used nested objects
    public ExerciseField exercise;

    /**
     * Nested field representing an exercise reference.
     */
    public static class ExerciseField {
        public String publicId;

        /**
         * Default constructor for JSON mapping.
         */
        public ExerciseField() {
        }

        /**
         * Constructs an ExerciseField with the given public ID.
         *
         * @param publicId the exercise's public identifier
         */
        public ExerciseField(final String publicId) {
            this.publicId = publicId;
        }
    }

    /**
     * Synchronizes the nested exercise field with the flat exercisePublicId field.
     */
    public void syncExercise() {
        if (this.exercise != null && this.exercise.publicId != null) {
            this.exercisePublicId = this.exercise.publicId;
        } else if (this.exercisePublicId != null) {
            this.exercise = new ExerciseField(this.exercisePublicId);
        }
    }
}
