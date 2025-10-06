package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

public class ExerciseDto {

    public Long id;

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    public String title;

    @Size(min = 1, message = "Content must not be empty")
    public String content;

    public Long userId;

    public Long lessonId;

    public Boolean published;

    public Boolean commentable;

    public LocalDateTime created;

    public LocalDateTime lastEdit;

    // Graspable Math fields
    public Boolean graspableEnabled;
    public String graspableInitialExpression;
    public String graspableTargetExpression;
    public String graspableAllowedOperations;
    public String graspableDifficulty;
    public String graspableHints;
    public String graspableConfig;

    // Helper fields for compatibility with old code that used nested objects
    public UserField user;
    public LessonField lesson;

    public ExerciseDto() {
    }

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
