package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for OpenAI Chat Completions API
 * Based on OpenAI REST API specification
 */
public class OpenAIRequestDto {

    public String model;

    public List<Message> messages;

    public Double temperature; // 0.0 to 2.0

    @JsonProperty("max_tokens")
    public Integer maxTokens;

    @JsonProperty("top_p")
    public Double topP;

    @JsonProperty("frequency_penalty")
    public Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    public Double presencePenalty;

    @JsonProperty("response_format")
    public ResponseFormat responseFormat;

    public static class Message {
        public String role; // "system", "user", "assistant"
        public String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ResponseFormat {
        public String type; // "text" or "json_object"

        public ResponseFormat() {
        }

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    /**
     * Helper method to create a simple chat request
     */
    public static OpenAIRequestDto createChatRequest(String systemPrompt, String userPrompt, String model,
            Double temperature, Integer maxTokens) {
        final var request = new OpenAIRequestDto();
        request.model = model;
        request.temperature = temperature;
        request.maxTokens = maxTokens;

        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt));

        return request;
    }

    /**
     * Helper method to create a JSON mode request (guarantees valid JSON response)
     */
    public static OpenAIRequestDto createJsonRequest(String systemPrompt, String userPrompt, String model,
            Double temperature, Integer maxTokens) {
        final var request = createChatRequest(systemPrompt, userPrompt, model, temperature, maxTokens);
        request.responseFormat = new ResponseFormat("json_object");
        return request;
    }
}
