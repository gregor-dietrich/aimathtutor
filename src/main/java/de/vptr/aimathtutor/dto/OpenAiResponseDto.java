package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/**
 * Response DTO for OpenAI Chat Completions API Based on OpenAI REST API specification
 */
public class OpenAiResponseDto {

    @Nullable
    public String id;
    @Nullable
    public String object;
    @Nullable
    public Long created;
    @Nullable
    public String model;
    @Nullable
    public List<Choice> choices;
    @Nullable
    public Usage usage;

    /**
     * Represents a choice in the OpenAI response.
     */
    public static class Choice {
        @Nullable
        public Integer index;
        @Nullable
        public Message message;
        @Nullable
        @JsonProperty("finish_reason")
        public String finishReason;
    }

    /**
     * Represents a message in the OpenAI response.
     */
    public static class Message {
        @Nullable
        public String role;
        @Nullable
        public String content;
    }

    /**
     * Represents usage statistics in the OpenAI response.
     */
    public static class Usage {
        @Nullable
        @JsonProperty("prompt_tokens")
        public Integer promptTokens;
        @Nullable
        @JsonProperty("completion_tokens")
        public Integer completionTokens;
        @Nullable
        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }

    /**
     * Extract the text content from the first choice
     */
    @Nullable
    public String getTextContent() {
        if (this.choices == null || this.choices.isEmpty()) {
            return null;
        }

        final var choice = this.choices.get(0);
        if (choice.message == null || choice.message.content == null) {
            return null;
        }

        return choice.message.content;
    }

    /**
     * Check if the response was completed successfully
     */
    public boolean isComplete() {
        if (this.choices == null || this.choices.isEmpty()) {
            return false;
        }

        final var finishReason = this.choices.get(0).finishReason;
        return "stop".equals(finishReason);
    }

    /**
     * Check if response was truncated due to token limit
     */
    public boolean isTruncated() {
        if (this.choices == null || this.choices.isEmpty()) {
            return false;
        }

        final var finishReason = this.choices.get(0).finishReason;
        return "length".equals(finishReason);
    }

    /**
     * Check if response was filtered due to content policy
     */
    public boolean isContentFiltered() {
        if (this.choices == null || this.choices.isEmpty()) {
            return false;
        }

        final var finishReason = this.choices.get(0).finishReason;
        return "content_filter".equals(finishReason);
    }
}
