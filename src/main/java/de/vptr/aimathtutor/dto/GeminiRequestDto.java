package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for Google Gemini API
 * Based on Gemini REST API specification
 */
public class GeminiRequestDto {

    public List<Content> contents;

    @JsonProperty("generationConfig")
    public GenerationConfig generationConfig;

    @JsonProperty("safetySettings")
    public List<SafetySetting> safetySettings;

    public static class Content {
        public List<Part> parts;
        public String role; // "user" or "model"

        public Content() {
        }

        public Content(final String text) {
            this.parts = List.of(new Part(text));
            this.role = "user";
        }
    }

    public static class Part {
        public String text;

        public Part() {
        }

        public Part(final String text) {
            this.text = text;
        }
    }

    public static class GenerationConfig {
        public Double temperature; // 0.0 to 1.0
        @JsonProperty("maxOutputTokens")
        public Integer maxOutputTokens;
        @JsonProperty("topP")
        public Double topP;
        @JsonProperty("topK")
        public Integer topK;

        public GenerationConfig() {
        }

        public GenerationConfig(final Double temperature, final Integer maxOutputTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
        }
    }

    public static class SafetySetting {
        public String lesson;
        public String threshold;

        public SafetySetting() {
        }

        public SafetySetting(final String lesson, final String threshold) {
            this.lesson = lesson;
            this.threshold = threshold;
        }
    }

    /**
     * Helper method to create a simple text request
     */
    public static GeminiRequestDto createTextRequest(final String prompt, final Double temperature, final Integer maxTokens) {
        final var request = new GeminiRequestDto();
        request.contents = List.of(new Content(prompt));
        request.generationConfig = new GenerationConfig(temperature, maxTokens);

        // Set safety settings to block only high-risk content
        request.safetySettings = List.of(
                new SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE"),
                new SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE"),
                new SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
                new SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE"));

        return request;
    }
}
