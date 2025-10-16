package de.vptr.aimathtutor.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the conversation context for AI requests.
 * Includes recent actions, user questions, and AI messages.
 */
public class ConversationContextDto {

    @JsonProperty("recent_actions")
    public List<GraspableEventDto> recentActions = new ArrayList<>();

    @JsonProperty("recent_questions")
    public List<ChatMessageDto> recentQuestions = new ArrayList<>();

    @JsonProperty("recent_ai_messages")
    public List<ChatMessageDto> recentAIMessages = new ArrayList<>();

    public ConversationContextDto() {
    }

    public ConversationContextDto(final List<GraspableEventDto> recentActions,
            final List<ChatMessageDto> recentQuestions,
            final List<ChatMessageDto> recentAIMessages) {
        this.recentActions = recentActions != null ? recentActions : new ArrayList<>();
        this.recentQuestions = recentQuestions != null ? recentQuestions : new ArrayList<>();
        this.recentAIMessages = recentAIMessages != null ? recentAIMessages : new ArrayList<>();
    }

    /**
     * Adds an action to the context, keeping only the last 5
     */
    public void addAction(final GraspableEventDto action) {
        if (action != null) {
            this.recentActions.add(action);
            if (this.recentActions.size() > 5) {
                this.recentActions.remove(0); // Remove oldest
            }
        }
    }

    /**
     * Adds a user question to the context, keeping only the last 5
     */
    public void addQuestion(final ChatMessageDto question) {
        if (question != null && question.sender == ChatMessageDto.Sender.USER
                && question.messageType == ChatMessageDto.MessageType.QUESTION) {
            this.recentQuestions.add(question);
            if (this.recentQuestions.size() > 5) {
                this.recentQuestions.remove(0); // Remove oldest
            }
        }
    }

    /**
     * Adds an AI message to the context, keeping only the last 5
     */
    public void addAIMessage(final ChatMessageDto message) {
        if (message != null && message.sender == ChatMessageDto.Sender.AI
                && (message.messageType == ChatMessageDto.MessageType.FEEDBACK
                        || message.messageType == ChatMessageDto.MessageType.ANSWER)) {
            this.recentAIMessages.add(message);
            if (this.recentAIMessages.size() > 5) {
                this.recentAIMessages.remove(0); // Remove oldest
            }
        }
    }

    @Override
    public String toString() {
        return "ConversationContextDto{" +
                "recentActions=" + this.recentActions.size() +
                ", recentQuestions=" + this.recentQuestions.size() +
                ", recentAIMessages=" + this.recentAIMessages.size() +
                '}';
    }
}
