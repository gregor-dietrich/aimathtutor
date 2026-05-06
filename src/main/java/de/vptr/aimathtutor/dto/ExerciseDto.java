package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import org.jboss.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(ExerciseDto.class);

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

    public String publicId;

    @Size(min = AppConstants.EXERCISE_TITLE_MIN_LENGTH, max = AppConstants.EXERCISE_TITLE_MAX_LENGTH, message = "Title must be between {min} and {max} characters")
    public String title;

    @Size(min = AppConstants.EXERCISE_CONTENT_MIN_LENGTH, max = AppConstants.EXERCISE_CONTENT_MAX_LENGTH, message = "Content must be between {min} and {max} characters")
    public String content;

    public String userPublicId;

    public String lessonPublicId;

    public Boolean published;

    public Boolean commentable;

    public LocalDateTime created;

    public LocalDateTime lastEdit;

    public Boolean graspableEnabled;

    @Size(max = AppConstants.EXERCISE_EXPRESSION_MAX_LENGTH, message = "Initial expression must not exceed {max} characters")
    public String graspableInitialExpression;

    @Size(max = AppConstants.EXERCISE_EXPRESSION_MAX_LENGTH, message = "Target expression must not exceed {max} characters")
    public String graspableTargetExpression;

    public DifficultyLevel graspableDifficulty;

    @Size(max = AppConstants.EXERCISE_HINTS_MAX_LENGTH, message = "Hints must not exceed {max} characters")
    public String graspableHints;

    public UserField user;
    public LessonField lesson;

    /**
     * Default constructor for JSON mapping.
     */
    public ExerciseDto() {
    }

    /**
     * Constructs an ExerciseDto with the given exercise details.
     *
     * @param title          the exercise title
     * @param content        the exercise content
     * @param userPublicId   the author's public ID
     * @param lessonPublicId the parent lesson's public ID
     * @param published      whether the exercise is published
     * @param commentable    whether comments are enabled
     */
    public ExerciseDto(final String title, final String content, final String userPublicId, final String lessonPublicId,
            final Boolean published, final Boolean commentable) {
        this.title = title;
        this.content = content;
        this.userPublicId = userPublicId;
        this.lessonPublicId = lessonPublicId;
        this.published = published;
        this.commentable = commentable;
    }

    /**
     * Nested field representing a user reference.
     */
    public static class UserField {
        public String publicId;
        public String username;

        /**
         * Default constructor for JSON mapping.
         */
        public UserField() {
        }

        /**
         * Constructs a UserField with the given public ID.
         *
         * @param publicId the user's public identifier
         */
        public UserField(final String publicId) {
            this.publicId = publicId;
        }

        public void setPublicId(final String publicId) {
            this.publicId = publicId;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    /**
     * Nested field representing a lesson reference.
     */
    public static class LessonField {
        public String publicId;
        public String name;

        /**
         * Default constructor for JSON mapping.
         */
        public LessonField() {
        }

        /**
         * Constructs a LessonField with the given public ID.
         *
         * @param publicId the lesson's public identifier
         */
        public LessonField(final String publicId) {
            this.publicId = publicId;
        }

        public void setPublicId(final String publicId) {
            this.publicId = publicId;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    /**
     * Synchronizes the nested user and lesson fields with their flat public ID
     * fields.
     * When both a nested object and its corresponding flat field are present and
     * non-null, the nested object's publicId takes precedence and will overwrite
     * the flat field. A warning is logged when the two values differ.
     */
    public void syncNestedFields() {
        if (this.user != null && this.user.publicId != null) {
            if (this.userPublicId != null && !this.user.publicId.equals(this.userPublicId)) {
                LOG.warnf(
                        "Conflict in syncNestedFields: user.publicId (%s) differs from userPublicId (%s). Using nested value.", 
                        this.user.publicId,  this.userPublicId);
            }
            this.userPublicId = this.user.publicId;
        } else if (this.userPublicId != null && this.user == null) {
            this.user = new UserField(this.userPublicId);
        }

        if (this.lesson != null && this.lesson.publicId != null) {
            if (this.lessonPublicId != null && !this.lesson.publicId.equals(this.lessonPublicId)) {
                LOG.warnf(
                        "Conflict in syncNestedFields: lesson.publicId (%s) differs from lessonPublicId (%s). Using nested value.", 
                        this.lesson.publicId,  this.lessonPublicId);
            }
            this.lessonPublicId = this.lesson.publicId;
        } else if (this.lessonPublicId != null && this.lesson == null) {
            this.lesson = new LessonField(this.lessonPublicId);
        }
    }
}
