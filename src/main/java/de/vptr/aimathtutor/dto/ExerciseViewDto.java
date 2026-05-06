package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.entity.ExerciseEntity;

/**
 * View DTO representing an exercise with denormalized user and lesson fields
 * suitable for display in the UI.
 */
public class ExerciseViewDto {

    public Long id;
    public String publicId;
    public String title;
    public String content;
    public String userPublicId;
    public String username;
    public String lessonPublicId;
    public String lessonName;
    public Boolean published;
    public Boolean commentable;
    public LocalDateTime created;
    public LocalDateTime lastEdit;
    public Long commentsCount;

    // Completion tracking for current user
    public Boolean userCompleted;
    public Integer userCompletionCount;

    // Graspable Math fields
    public Boolean graspableEnabled;
    public String graspableInitialExpression;
    public String graspableTargetExpression;
    public DifficultyLevel graspableDifficulty;
    public String graspableHints;

    public ExerciseViewDto() {
    }

    /**
     * Constructs an ExerciseViewDto from an ExerciseEntity.
     */
    public ExerciseViewDto(final ExerciseEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.publicId = entity.publicId;
            this.title = entity.title;
            this.content = entity.content;
            this.userPublicId = entity.user != null ? entity.user.publicId : null;
            this.username = entity.user != null ? entity.user.username : null;
            this.lessonPublicId = entity.lesson != null ? entity.lesson.publicId : null;
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
            this.graspableDifficulty = entity.graspableDifficulty;
            this.graspableHints = entity.graspableHints;
        }
    }

    /**
     * Getter for publicId
     */
    public String getPublicId() {
        return this.publicId;
    }

    /**
     * Convert this ViewDto to a ExerciseDto for create/update operations
     */
    public ExerciseDto toExerciseDto() {
        final ExerciseDto dto = new ExerciseDto();
        dto.publicId = this.publicId;
        dto.title = this.title;
        dto.content = this.content;
        dto.userPublicId = this.userPublicId;
        dto.lessonPublicId = this.lessonPublicId;
        dto.published = this.published;
        dto.commentable = this.commentable;
        dto.created = this.created;
        dto.lastEdit = this.lastEdit;

        // Graspable Math fields
        dto.graspableEnabled = this.graspableEnabled;
        dto.graspableInitialExpression = this.graspableInitialExpression;
        dto.graspableTargetExpression = this.graspableTargetExpression;
        dto.graspableDifficulty = this.graspableDifficulty;
        dto.graspableHints = this.graspableHints;

        return dto;
    }
}
