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
        @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = { "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
                "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD" }, justification = "JSON mapping DTO fields are intentionally public and populated by Jackson")
        public List<Part> parts;
        public String role;
    }

    public static class Part {
        @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "JSON mapping DTO fields are public and populated by Jackson")
        public String text;
    }

    public static class SafetyRating {
        @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", justification = "Safety rating fields are optional in API responses")
        public String lesson;
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
        if (this.candidates == null || this.candidates.isEmpty()) {
            return null;
        }

        final var candidate = this.candidates.get(0);
        if (candidate.content == null || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
            return null;
        }

        return candidate.content.parts.get(0).text;
    }

    /**
     * Check if the response was blocked due to safety filters
     */
    public boolean isBlocked() {
        if (this.candidates == null || this.candidates.isEmpty()) {
            return true;
        }

        final var finishReason = this.candidates.get(0).finishReason;
        return "SAFETY".equals(finishReason) || "BLOCKED".equals(finishReason);
    }
}
