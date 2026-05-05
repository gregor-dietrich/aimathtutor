package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a student action in the Graspable Math workspace.
 * This DTO captures events like simplify, expand, factor, move, etc.
 */
public class GraspableEventDto {

    @JsonProperty("event_type")
    public String eventType; // e.g., "simplify", "expand", "factor", "move", "combine"

    @JsonProperty("expression_before")
    public String expressionBefore; // The mathematical expression before the action

    @JsonProperty("expression_after")
    public String expressionAfter; // The mathematical expression after the action

    @JsonProperty("action_details")
    public String actionDetails; // Additional context about the action (JSON string)

    @JsonProperty("student_public_id")
    public String studentPublicId;

    @JsonProperty("exercise_public_id")
    public String exercisePublicId;

    @JsonProperty("session_id")
    public String sessionId; // Unique identifier for this student session

    public LocalDateTime timestamp;

    public Boolean correct; // Whether the action was mathematically correct

    public Boolean isComplete; // Whether this action resulted in problem completion

    public GraspableEventDto() {
    }

    /**
     * Constructs a GraspableEventDto with the specified parameters.
     */
    public GraspableEventDto(final String eventType, final String expressionBefore,
            final String expressionAfter, final String studentPublicId,
            final String exercisePublicId, final String sessionId) {
        this();
        this.eventType = eventType;
        this.expressionBefore = expressionBefore;
        this.expressionAfter = expressionAfter;
        this.studentPublicId = studentPublicId;
        this.exercisePublicId = exercisePublicId;
        this.sessionId = sessionId;
    }

    /**
     * Debug-friendly string representation of the event.
     */
    @Override
    public String toString() {
        return "GraspableEventDto{"
                + "eventType='" + this.eventType + '\''
                + ", expressionBefore='" + this.expressionBefore + '\''
                + ", expressionAfter='" + this.expressionAfter + '\''
                + ", studentPublicId=" + this.studentPublicId
                + ", exercisePublicId=" + this.exercisePublicId
                + ", sessionId='" + this.sessionId + '\''
                + ", timestamp=" + this.timestamp
                + ", correct=" + this.correct
                + '}';
    }
}
