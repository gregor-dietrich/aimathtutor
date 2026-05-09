package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import jakarta.annotation.Nullable;

/**
 * DTO for displaying AI interaction information in admin views. Used for analyzing AI feedback and student
 * interactions.
 */
public class AiInteractionViewDto {
    @Nullable
    public String publicId;
    @Nullable
    public String sessionId;
    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public String exercisePublicId;
    @Nullable
    public String exerciseTitle;
    @Nullable
    public String eventType;
    @Nullable
    public String studentMessage;
    @Nullable
    public String expressionBefore;
    @Nullable
    public String expressionAfter;
    @Nullable
    public String feedbackType;
    @Nullable
    public String feedbackMessage;
    @Nullable
    public Double confidenceScore;
    @Nullable
    public Boolean actionCorrect;
    @Nullable
    public LocalDateTime created;

    public AiInteractionViewDto() {
    }

    /**
     * Constructs an AiInteractionViewDto from an AiInteractionEntity.
     */
    public AiInteractionViewDto(final AiInteractionEntity entity) {
        if (entity != null) {
            this.publicId = entity.publicId;
            this.sessionId = entity.sessionId;
            this.eventType = entity.eventType;
            this.studentMessage = entity.studentMessage;
            this.expressionBefore = entity.expressionBefore;
            this.expressionAfter = entity.expressionAfter;
            this.feedbackType = entity.feedbackType;
            this.feedbackMessage = entity.feedbackMessage;
            this.confidenceScore = entity.confidenceScore;
            this.actionCorrect = entity.actionCorrect;
            this.created = entity.created;

            // Handle user information safely
            if (entity.user != null) {
                this.userPublicId = entity.user.publicId;
                this.username = entity.user.username;
            }

            // Handle exercise information safely
            if (entity.exercise != null) {
                this.exercisePublicId = entity.exercise.publicId;
                this.exerciseTitle = entity.exercise.title;
            }
        }
    }
}
