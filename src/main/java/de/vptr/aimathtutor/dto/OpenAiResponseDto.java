package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Response DTO for OpenAI Chat Completions API
 * Based on OpenAI REST API specification
 */
@SuppressFBWarnings(value = { "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
        "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD" }, justification = "DTO for JSON mapping from OpenAI; public fields intentionally used for Jackson mapping")
public class OpenAiResponseDto {

    public String id;
    public String object;
    public Long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    /**
     * Represents a choice in the OpenAI response.
     */
    public static class Choice {
        public Integer index;
        public Message message;
        @JsonProperty("finish_reason")
        public String finishReason;
    }

    /**
     * Represents a message in the OpenAI response.
     */
    public static class Message {
        public String role;
        public String content;
    }

    /**
     * Represents usage statistics in the OpenAI response.
     */
    public static class Usage {
        @JsonProperty("prompt_tokens")
        public Integer promptTokens;
        @JsonProperty("completion_tokens")
        public Integer completionTokens;
        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }

    /**
     * Extract the text content from the first choice
     */
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
}
