package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Request DTO for OpenAI Chat Completions API Based on OpenAI REST API specification
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
        justification = "Request DTO used for JSON mapping; public fields are intentional")
public class OpenAiRequestDto {

    @Nullable
    public String model;

    @Nullable
    public List<Message> messages;

    @Nullable
    public Double temperature; // 0.0 to 2.0

    @Nullable
    @JsonProperty("max_tokens")
    public Integer maxTokens;

    @Nullable
    @JsonProperty("top_p")
    public Double topP;

    @Nullable
    @JsonProperty("frequency_penalty")
    public Double frequencyPenalty;

    @Nullable
    @JsonProperty("presence_penalty")
    public Double presencePenalty;

    @Nullable
    @JsonProperty("response_format")
    public ResponseFormat responseFormat;

    /**
     * Represents a message in the OpenAI request.
     */
    public static class Message {
        @Nullable
        public String role; // "system", "user", "assistant"
        @Nullable
        public String content;

        public Message() {
        }

        public Message(final String role, final String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * Represents the response format in the OpenAI request.
     */
    public static class ResponseFormat {
        @Nullable
        public String type; // "text" or "json_object"

        public ResponseFormat() {
        }

        public ResponseFormat(final String type) {
            this.type = type;
        }
    }

    /**
     * Helper method to create a simple chat request
     */
    public static OpenAiRequestDto createChatRequest(final String systemPrompt, final String userPrompt,
            final String model, final Double temperature, final Integer maxTokens) {
        final var request = new OpenAiRequestDto();
        request.model = model;
        request.temperature = temperature;
        request.maxTokens = maxTokens;

        request.messages = List.of(new Message("system", systemPrompt), new Message("user", userPrompt));

        return request;
    }

    /**
     * Helper method to create a JSON mode request (guarantees valid JSON response)
     */
    public static OpenAiRequestDto createJsonRequest(final String systemPrompt, final String userPrompt,
            final String model, final Double temperature, final Integer maxTokens) {
        final var request = createChatRequest(systemPrompt, userPrompt, model, temperature, maxTokens);
        request.responseFormat = new ResponseFormat("json_object");
        return request;
    }
}
