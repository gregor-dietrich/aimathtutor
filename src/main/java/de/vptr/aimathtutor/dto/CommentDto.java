package de.vptr.aimathtutor.dto;

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

    public Long id;

    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters when provided")
    public String content;

    // Required for POST operations (creation)
    // Ignored for PUT/PATCH operations (exerciseId comes from the URL path)
    public Long exerciseId;

    // NEW: For threading support
    public Long parentCommentId;

    // NEW: For lesson comments (future extension)
    public Long lessonId;

    // NEW: For tracking which session the comment was made during
    public String sessionId;

    // Helper field for compatibility with old code that used nested objects
    public ExerciseField exercise;

    /**
     * Helper class for nested exercise field access
     */
    public static class ExerciseField {
        public Long id;

        public ExerciseField() {
        }

        public ExerciseField(final Long id) {
            this.id = id;
        }
    }

    /**
     * Ensure exerciseId and exercise stay in sync
     */
    public void syncExercise() {
        if (this.exercise != null && this.exercise.id != null) {
            this.exerciseId = this.exercise.id;
        } else if (this.exerciseId != null) {
            this.exercise = new ExerciseField(this.exerciseId);
        }
    }
}
