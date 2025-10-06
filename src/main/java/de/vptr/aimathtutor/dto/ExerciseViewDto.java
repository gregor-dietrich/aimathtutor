package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.ExerciseEntity;

public class ExerciseViewDto {

    public Long id;
    public String title;
    public String content;
    public Long userId;
    public String username;
    public Long lessonId;
    public String lessonName;
    public Boolean published;
    public Boolean commentable;
    public LocalDateTime created;
    public LocalDateTime lastEdit;
    public Long commentsCount;

    // Graspable Math fields
    public Boolean graspableEnabled;
    public String graspableInitialExpression;
    public String graspableTargetExpression;
    public String graspableAllowedOperations;
    public String graspableDifficulty;
    public String graspableHints;
    public String graspableConfig;

    public ExerciseViewDto() {
    }

    public ExerciseViewDto(final ExerciseEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.title = entity.title;
            this.content = entity.content;
            this.userId = entity.user != null ? entity.user.id : null;
            this.username = entity.user != null ? entity.user.username : null;
            this.lessonId = entity.lesson != null ? entity.lesson.id : null;
            this.lessonName = entity.lesson != null ? entity.lesson.name : null;
            this.published = entity.published;
            this.commentable = entity.commentable;
            this.created = entity.created;
            this.lastEdit = entity.lastEdit;
            this.commentsCount = entity.comments != null ? (long) entity.comments.size() : 0L;

            // Graspable Math fields
            this.graspableEnabled = entity.graspableEnabled;
            this.graspableInitialExpression = entity.graspableInitialExpression;
            this.graspableTargetExpression = entity.graspableTargetExpression;
            this.graspableAllowedOperations = entity.graspableAllowedOperations;
            this.graspableDifficulty = entity.graspableDifficulty;
            this.graspableHints = entity.graspableHints;
            this.graspableConfig = entity.graspableConfig;
        }
    }

    /**
     * Getter for name (returns title for compatibility)
     */
    public String getName() {
        return this.title;
    }

    /**
     * Getter for id
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Convert this ViewDto to a ExerciseDto for create/update operations
     */
    public ExerciseDto toExerciseDto() {
        final ExerciseDto dto = new ExerciseDto();
        dto.id = this.id;
        dto.title = this.title;
        dto.content = this.content;
        dto.userId = this.userId;
        dto.lessonId = this.lessonId;
        dto.published = this.published;
        dto.commentable = this.commentable;
        dto.created = this.created;
        dto.lastEdit = this.lastEdit;

        // Graspable Math fields
        dto.graspableEnabled = this.graspableEnabled;
        dto.graspableInitialExpression = this.graspableInitialExpression;
        dto.graspableTargetExpression = this.graspableTargetExpression;
        dto.graspableAllowedOperations = this.graspableAllowedOperations;
        dto.graspableDifficulty = this.graspableDifficulty;
        dto.graspableHints = this.graspableHints;
        dto.graspableConfig = this.graspableConfig;

        return dto;
    }
}
