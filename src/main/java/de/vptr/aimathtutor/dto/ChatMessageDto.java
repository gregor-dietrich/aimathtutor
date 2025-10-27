package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a message in the chat interface between student and AI tutor.
 * Can be either a user question or an AI response.
 */
public class ChatMessageDto {

    /**
     * TODO: Class documentation.
     */
    public enum Sender {
        USER, // Message from student
        AI // Message from AI tutor
    }

    /**
     * TODO: Class documentation.
     */
    public enum MessageType {
        QUESTION, // User asking a question
        FEEDBACK, // AI providing automatic feedback on an action
        ANSWER, // AI answering a direct question
        SYSTEM // System messages (e.g., "Problem loaded")
    }

    public Sender sender;

    @JsonProperty("message_type")
    public MessageType messageType;

    public String message;

    @JsonProperty("session_id")
    public String sessionId;

    public LocalDateTime timestamp;

    // Optional: Reference to the action that triggered this message (for feedback)
    @JsonProperty("related_action")
    public String relatedAction;

    public ChatMessageDto() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessageDto(final Sender sender, final MessageType messageType, final String message) {
        this();
        this.sender = sender;
        this.messageType = messageType;
        this.message = message;
    }

    /**
     * TODO: Document userQuestion().
     */
    public static ChatMessageDto userQuestion(final String message) {
        return new ChatMessageDto(Sender.USER, MessageType.QUESTION, message);
    }

    /**
     * TODO: Document aiFeedback().
     */
    public static ChatMessageDto aiFeedback(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.FEEDBACK, message);
    }

    /**
     * TODO: Document aiAnswer().
     */
    public static ChatMessageDto aiAnswer(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.ANSWER, message);
    }

    /**
     * TODO: Document system().
     */
    public static ChatMessageDto system(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.SYSTEM, message);
    }

    /**
     * TODO: Document toString().
     */
    @Override
    public String toString() {
        return "ChatMessageDto{"
                + "sender=" + this.sender
                + ", messageType=" + this.messageType
                + ", message='" + this.message + '\''
                + ", timestamp=" + this.timestamp
                + ", sessionId='" + this.sessionId + '\''
                + '}';
    }
}
