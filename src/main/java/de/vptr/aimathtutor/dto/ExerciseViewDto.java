package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import jakarta.annotation.Nullable;

/**
 * View DTO representing an exercise with denormalized user and lesson fields suitable for display in the UI.
 */
public class ExerciseViewDto {
    @Nullable
    public Long id;
    @Nullable
    public String publicId;
    @Nullable
    public String title;
    @Nullable
    public String content;
    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public String lessonPublicId;
    @Nullable
    public String lessonName;
    @Nullable
    public Boolean published;
    @Nullable
    public Boolean commentable;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;
    @Nullable
    public Long commentsCount;

    // Completion tracking for current user
    @Nullable
    public Boolean userCompleted;
    @Nullable
    public Integer userCompletionCount;

    // Graspable Math fields
    @Nullable
    public Boolean graspableEnabled;
    @Nullable
    public String graspableInitialExpression;
    @Nullable
    public String graspableTargetExpression;
    @Nullable
    public DifficultyLevel graspableDifficulty;
    @Nullable
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
    @Nullable
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
