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

    @JsonProperty("student_id")
    public Long studentId;

    @JsonProperty("exercise_id")
    public Long exerciseId;

    @JsonProperty("session_id")
    public String sessionId; // Unique identifier for this student session

    public LocalDateTime timestamp;

    public Boolean correct; // Whether the action was mathematically correct

    public GraspableEventDto() {
        this.timestamp = LocalDateTime.now();
    }

    public GraspableEventDto(final String eventType, final String expressionBefore,
            final String expressionAfter, final Long studentId,
            final Long exerciseId, final String sessionId) {
        this();
        this.eventType = eventType;
        this.expressionBefore = expressionBefore;
        this.expressionAfter = expressionAfter;
        this.studentId = studentId;
        this.exerciseId = exerciseId;
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "GraspableEventDto{" +
                "eventType='" + eventType + '\'' +
                ", expressionBefore='" + expressionBefore + '\'' +
                ", expressionAfter='" + expressionAfter + '\'' +
                ", studentId=" + studentId +
                ", exerciseId=" + exerciseId +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", correct=" + correct +
                '}';
    }
}
