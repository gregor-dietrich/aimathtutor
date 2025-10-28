package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents AI-generated feedback for a student's math action.
 * Contains hints, encouragement, corrections, and next steps.
 */
public class AiFeedbackDto {

    /**
     * TODO: Class documentation.
     */
    public enum FeedbackType {
        POSITIVE, // Encouragement for correct action
        CORRECTIVE, // Pointing out an error
        HINT, // Providing a hint without giving away the answer
        SUGGESTION, // Suggesting a better approach
        NEUTRAL // Informational feedback
    }

    public FeedbackType type;

    public String message; // Main feedback message

    @JsonProperty("detailed_explanation")
    public String detailedExplanation; // Optional detailed explanation

    public List<String> hints; // Additional hints if student is stuck

    @JsonProperty("suggested_next_steps")
    public List<String> suggestedNextSteps; // What the student should try next

    @JsonProperty("related_concepts")
    public List<String> relatedConcepts; // Math concepts related to this feedback

    public Double confidence; // AI confidence score (0.0 to 1.0)

    public LocalDateTime timestamp;

    @JsonProperty("session_id")
    public String sessionId;

    /**
     * Constructs an AiFeedbackDto with default values.
     */
    public AiFeedbackDto() {
        this.timestamp = LocalDateTime.now();
        this.hints = new ArrayList<>();
        this.suggestedNextSteps = new ArrayList<>();
        this.relatedConcepts = new ArrayList<>();
        this.confidence = 1.0;
    }

    /**
     * Constructs an AiFeedbackDto with the specified type and message.
     */
    public AiFeedbackDto(final FeedbackType type, final String message) {
        this();
        this.type = type;
        this.message = message;
    }

    /**
     * Creates a positive feedback DTO with the specified message.
     */
    public static AiFeedbackDto positive(final String message) {
        return new AiFeedbackDto(FeedbackType.POSITIVE, message);
    }

    /**
     * TODO: Document corrective().
     */
    public static AiFeedbackDto corrective(final String message) {
        return new AiFeedbackDto(FeedbackType.CORRECTIVE, message);
    }

    /**
     * TODO: Document hint().
     */
    public static AiFeedbackDto hint(final String message) {
        return new AiFeedbackDto(FeedbackType.HINT, message);
    }

    /**
     * TODO: Document suggestion().
     */
    public static AiFeedbackDto suggestion(final String message) {
        return new AiFeedbackDto(FeedbackType.SUGGESTION, message);
    }

    /**
     * TODO: Document neutral().
     */
    public static AiFeedbackDto neutral(final String message) {
        return new AiFeedbackDto(FeedbackType.NEUTRAL, message);
    }

    /**
     * TODO: Document error().
     */
    public static AiFeedbackDto error(final String message) {
        return new AiFeedbackDto(FeedbackType.CORRECTIVE, message);
    }

    /**
     * TODO: Document toString().
     */
    @Override
    public String toString() {
        return "AIFeedbackDto{"
                + "type=" + this.type
                + ", message='" + this.message + '\''
                + ", confidence=" + this.confidence
                + ", timestamp=" + this.timestamp
                + ", sessionId='" + this.sessionId + '\''
                + '}';
    }
}
