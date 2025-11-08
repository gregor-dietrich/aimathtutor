package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a message in the chat interface between student and AI tutor.
 * Can be either a user question or an AI response.
 */
public class ChatMessageDto {

    /**
     * Sender of the chat message (student or AI tutor).
     */
    public enum Sender {
        USER, // Message from student
        AI // Message from AI tutor
    }

    /**
     * Type of the chat message used to differentiate UI rendering and
     * handling.
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

    /**
     * Constructs a ChatMessageDto with the specified sender, type, and message.
     *
     * @param sender      the sender of the message (USER or AI)
     * @param messageType the type of the message (QUESTION, FEEDBACK, ANSWER,
     *                    SYSTEM)
     * @param message     the content of the message
     */
    public ChatMessageDto(final Sender sender, final MessageType messageType, final String message) {
        this();
        this.sender = sender;
        this.messageType = messageType;
        this.message = message;
    }

    /**
     * Create a user question message.
     *
     * @param message question text
     * @return ChatMessageDto instance for a user question
     */
    public static ChatMessageDto userQuestion(final String message) {
        return new ChatMessageDto(Sender.USER, MessageType.QUESTION, message);
    }

    /**
     * Create an AI feedback message.
     *
     * @param message feedback text
     * @return ChatMessageDto instance for AI feedback
     */
    public static ChatMessageDto aiFeedback(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.FEEDBACK, message);
    }

    /**
     * Create an AI answer message (response to a question).
     *
     * @param message answer text
     * @return ChatMessageDto instance for an AI answer
     */
    public static ChatMessageDto aiAnswer(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.ANSWER, message);
    }

    /**
     * Create a system message.
     *
     * @param message system text
     * @return ChatMessageDto instance for system messages
     */
    public static ChatMessageDto system(final String message) {
        return new ChatMessageDto(Sender.AI, MessageType.SYSTEM, message);
    }

    /**
     * Debug representation of the chat message.
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
