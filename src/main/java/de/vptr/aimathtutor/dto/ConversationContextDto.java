package de.vptr.aimathtutor.dto;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the conversation context for AI requests.
 * Includes recent actions, user questions, and AI messages.
 */
public class ConversationContextDto {

    // Private final + unmodifiable getters — intentional encapsulation.
    // Do NOT revert to public fields.
    @JsonProperty("recent_actions")
    private final List<GraspableEventDto> recentActions = new CopyOnWriteArrayList<>();

    @JsonProperty("recent_questions")
    private final List<ChatMessageDto> recentQuestions = new CopyOnWriteArrayList<>();

    @JsonProperty("recent_ai_messages")
    private final List<ChatMessageDto> recentAiMessages = new CopyOnWriteArrayList<>();

    public ConversationContextDto() {
    }

    /**
     * Constructs a ConversationContextDto with the specified lists.
     */
    public ConversationContextDto(final List<GraspableEventDto> recentActions,
            final List<ChatMessageDto> recentQuestions,
            final List<ChatMessageDto> recentAiMessages) {
        if (recentActions != null) {
            final int start = Math.max(0, recentActions.size() - 5);
            this.recentActions.addAll(recentActions.subList(start, recentActions.size()));
        }
        if (recentQuestions != null) {
            final int start = Math.max(0, recentQuestions.size() - 5);
            this.recentQuestions.addAll(recentQuestions.subList(start, recentQuestions.size()));
        }
        if (recentAiMessages != null) {
            final int start = Math.max(0, recentAiMessages.size() - 5);
            this.recentAiMessages.addAll(recentAiMessages.subList(start, recentAiMessages.size()));
        }
    }

    public List<GraspableEventDto> getRecentActions() {
        return Collections.unmodifiableList(this.recentActions);
    }

    public List<ChatMessageDto> getRecentQuestions() {
        return Collections.unmodifiableList(this.recentQuestions);
    }

    public List<ChatMessageDto> getRecentAiMessages() {
        return Collections.unmodifiableList(this.recentAiMessages);
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
    public void addAiMessage(final ChatMessageDto message) {
        if (message != null && message.sender == ChatMessageDto.Sender.AI
                && (message.messageType == ChatMessageDto.MessageType.FEEDBACK
                        || message.messageType == ChatMessageDto.MessageType.ANSWER)) {
            this.recentAiMessages.add(message);
            if (this.recentAiMessages.size() > 5) {
                this.recentAiMessages.remove(0); // Remove oldest
            }
        }
    }

    /**
     * Return a compact summary of the conversation context for logging.
     */
    @Override
    public String toString() {
        return "ConversationContextDto{"
                + "recentActions=" + this.recentActions.size()
                + ", recentQuestions=" + this.recentQuestions.size()
                + ", recentAIMessages=" + this.recentAiMessages.size()
                + '}';
    }
}
