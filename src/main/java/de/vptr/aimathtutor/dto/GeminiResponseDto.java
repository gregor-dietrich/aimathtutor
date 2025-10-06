package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Google Gemini API
 * Based on Gemini REST API specification
 */
public class GeminiResponseDto {

    public List<Candidate> candidates;

    @JsonProperty("promptFeedback")
    public PromptFeedback promptFeedback;

    public static class Candidate {
        public Content content;
        @JsonProperty("finishReason")
        public String finishReason;
        public Integer index;
        @JsonProperty("safetyRatings")
        public List<SafetyRating> safetyRatings;
    }

    public static class Content {
        public List<Part> parts;
        public String role;
    }

    public static class Part {
        public String text;
    }

    public static class SafetyRating {
        public String category;
        public String probability;
    }

    public static class PromptFeedback {
        @JsonProperty("safetyRatings")
        public List<SafetyRating> safetyRatings;
    }

    /**
     * Extract the text content from the first candidate
     */
    public String getTextContent() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        final var candidate = candidates.get(0);
        if (candidate.content == null || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
            return null;
        }

        return candidate.content.parts.get(0).text;
    }

    /**
     * Check if the response was blocked due to safety filters
     */
    public boolean isBlocked() {
        if (candidates == null || candidates.isEmpty()) {
            return true;
        }

        final var finishReason = candidates.get(0).finishReason;
        return "SAFETY".equals(finishReason) || "BLOCKED".equals(finishReason);
    }
}
