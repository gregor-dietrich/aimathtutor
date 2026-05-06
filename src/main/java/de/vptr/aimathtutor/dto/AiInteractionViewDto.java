package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.AiInteractionEntity;

/**
 * DTO for displaying AI interaction information in admin views.
 * Used for analyzing AI feedback and student interactions.
 */
public class AiInteractionViewDto {

    public String publicId;
    public String sessionId;
    public String userPublicId;
    public String username;
    public String exercisePublicId;
    public String exerciseTitle;
    public String eventType;
    public String studentMessage;
    public String expressionBefore;
    public String expressionAfter;
    public String feedbackType;
    public String feedbackMessage;
    public Double confidenceScore;
    public Boolean actionCorrect;
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
