package de.vptr.aimathtutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/**
 * Response DTO for Google Gemini API Based on Gemini REST API specification
 */
public class GeminiResponseDto {
    @Nullable
    public List<Candidate> candidates;

    @JsonProperty("promptFeedback")
    @Nullable
    public PromptFeedback promptFeedback;

    /**
     * Represents a candidate in the Gemini response.
     */
    public static class Candidate {
        @Nullable
        public Content content;
        @Nullable
        @JsonProperty("finishReason")
        public String finishReason;
        @Nullable
        public Integer index;
        @Nullable
        @JsonProperty("safetyRatings")
        public List<SafetyRating> safetyRatings;
    }

    /**
     * Represents content in the Gemini response.
     */
    public static class Content {
        @Nullable
        public List<Part> parts;
        @Nullable
        public String role;
    }

    /**
     * Represents a part in the Gemini response content. Models with thinking mode return parts where
     * {@code thought=true} containing reasoning that should not be shown to users. Only parts where {@code thought} is
     * absent or {@code false} contain the actual response text.
     */
    public static class Part {
        @Nullable
        public String text;
        @Nullable
        public Boolean thought;
    }

    /**
     * Represents a safety rating in the Gemini response.
     */
    public static class SafetyRating {
        @Nullable
        public String category;
        @Nullable
        public String probability;
    }

    /**
     * Represents prompt feedback in the Gemini response.
     */
    public static class PromptFeedback {
        @Nullable
        @JsonProperty("safetyRatings")
        public List<SafetyRating> safetyRatings;
    }

    /**
     * Extract the text content from the first candidate, skipping thought/reasoning parts. Models with thinking mode
     * return parts where {@code thought=true} containing internal reasoning that should not be shown to students. Only
     * parts where {@code thought} is absent or {@code false} contain the actual response text.
     */
    @Nullable
    public String getTextContent() {
        if (this.candidates == null || this.candidates.isEmpty()) {
            return null;
        }

        final var candidate = this.candidates.get(0);
        if (candidate == null || candidate.content == null || candidate.content.parts == null
                || candidate.content.parts.isEmpty()) {
            return null;
        }

        for (final var part : candidate.content.parts) {
            if (part == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(part.thought) && part.text != null && !part.text.isBlank()) {
                return part.text;
            }
        }

        return null;
    }

    /**
     * Check if the response was blocked due to safety filters
     */
    @Nullable
    public boolean isBlocked() {
        if (this.candidates == null || this.candidates.isEmpty()) {
            return false;
        }

        final var finishReason = this.candidates.get(0).finishReason;
        return "SAFETY".equals(finishReason) || "BLOCKED".equals(finishReason);
    }

    /**
     * Check if the response is empty or missing candidates
     */
    @Nullable
    public boolean isEmptyResponse() {
        return this.candidates == null || this.candidates.isEmpty();
    }

    /**
     * Check if the response was truncated due to the token limit (Gemini reports {@code "MAX_TOKENS"} as the finish
     * reason).
     */
    @Nullable
    public boolean isTruncated() {
        if (this.candidates == null || this.candidates.isEmpty()) {
            return false;
        }
        return "MAX_TOKENS".equals(this.candidates.get(0).finishReason);
    }

    /**
     * Returns the finish reason of the first candidate.
     *
     * @return the finish reason, or {@code null} if no candidates are present.
     */
    @Nullable
    public String getFinishReason() {
        if (this.candidates == null || this.candidates.isEmpty()) {
            return null;
        }
        return this.candidates.get(0).finishReason;
    }
}
