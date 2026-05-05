package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonValue;

import de.vptr.aimathtutor.util.AppConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.Size;

/**
 * DTO for exercise data.
 */
@SuppressFBWarnings(value = { "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
        "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "DTO used for JSON mapping and UI binding; public fields are intentional")
public class ExerciseDto {

    /**
     * Enumeration of difficulty levels for exercises and math problems.
     * Maps to string values stored in the database and used in UI components.
     */
    public enum DifficultyLevel {

        BEGINNER("beginner"),
        INTERMEDIATE("intermediate"),
        ADVANCED("advanced"),
        EXPERT("expert");

        private final String value;

        DifficultyLevel(final String value) {
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
    }

    public Long id;

    @Size(min = AppConstants.EXERCISE_TITLE_MIN_LENGTH, max = AppConstants.EXERCISE_TITLE_MAX_LENGTH, message = "Title must be between {min} and {max} characters")
    public String title;

    @Size(min = AppConstants.EXERCISE_CONTENT_MIN_LENGTH, max = AppConstants.EXERCISE_CONTENT_MAX_LENGTH, message = "Content must be between {min} and {max} characters")
    public String content;

    public Long userId;

    public Long lessonId;

    public Boolean published;

    public Boolean commentable;

    public LocalDateTime created;

    public LocalDateTime lastEdit;

    // Graspable Math fields
    public Boolean graspableEnabled;

    @Size(max = AppConstants.EXERCISE_EXPRESSION_MAX_LENGTH, message = "Initial expression must not exceed {max} characters")
    public String graspableInitialExpression;

    @Size(max = AppConstants.EXERCISE_EXPRESSION_MAX_LENGTH, message = "Target expression must not exceed {max} characters")
    public String graspableTargetExpression;

    public DifficultyLevel graspableDifficulty;

    @Size(max = AppConstants.EXERCISE_HINTS_MAX_LENGTH, message = "Hints must not exceed {max} characters")
    public String graspableHints;

    // Helper fields for compatibility with old code that used nested objects
    public UserField user;
    public LessonField lesson;

    public ExerciseDto() {
    }

    /**
     * Constructs an ExerciseDto with the specified parameters.
     *
     * @param title       the title of the exercise
     * @param content     the content of the exercise
     * @param userId      the ID of the user who created the exercise
     * @param lessonId    the ID of the lesson associated with the exercise
     * @param published   whether the exercise is published or not
     * @param commentable whether the exercise is commentable or not
     */
    public ExerciseDto(final String title, final String content, final Long userId, final Long lessonId,
            final Boolean published, final Boolean commentable) {
        this.title = title;
        this.content = content;
        this.userId = userId;
        this.lessonId = lessonId;
        this.published = published;
        this.commentable = commentable;
    }

    /**
     * Helper class for nested user field access.
     */
    public static class UserField {
        public Long id;
        public String username;

        public UserField() {
        }

        public UserField(final Long id) {
            this.id = id;
        }

        /**
         * Set the nested user's id.
         *
         * @param id user id
         */
        public void setId(final Long id) {
            this.id = id;
        }

        /**
         * Set the nested user's username.
         *
         * @param username username string
         */
        public void setUsername(final String username) {
            this.username = username;
        }
    }

    /**
     * Helper class for nested lesson field access.
     */
    public static class LessonField {
        public Long id;
        public String name;

        public LessonField() {
        }

        public LessonField(final Long id) {
            this.id = id;
        }
    }

    /**
     * Ensure userId/lessonId and nested objects stay in sync
     */
    public void syncNestedFields() {
        if (this.user != null && this.user.id != null) {
            this.userId = this.user.id;
        } else if (this.userId != null && this.user == null) {
            this.user = new UserField(this.userId);
        }

        if (this.lesson != null && this.lesson.id != null) {
            this.lessonId = this.lesson.id;
        } else if (this.lessonId != null && this.lesson == null) {
            this.lesson = new LessonField(this.lessonId);
        }
    }
}
