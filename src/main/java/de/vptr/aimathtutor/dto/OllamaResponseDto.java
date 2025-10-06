package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Ollama Generate API
 * Based on Ollama REST API specification
 */
public class OllamaResponseDto {

    public String model;
    @JsonProperty("created_at")
    public String createdAt;
    public String response;
    public Boolean done;
    @JsonProperty("total_duration")
    public Long totalDuration;
    @JsonProperty("load_duration")
    public Long loadDuration;
    @JsonProperty("prompt_eval_count")
    public Integer promptEvalCount;
    @JsonProperty("eval_count")
    public Integer evalCount;
    @JsonProperty("eval_duration")
    public Long evalDuration;

    /**
     * Extract the text content
     */
    public String getTextContent() {
        return response;
    }

    /**
     * Check if the response is complete
     */
    public boolean isComplete() {
        return done != null && done;
    }

    /**
     * Get tokens per second (if available)
     */
    public Double getTokensPerSecond() {
        if (evalCount != null && evalDuration != null && evalDuration > 0) {
            // evalDuration is in nanoseconds
            return evalCount / (evalDuration / 1_000_000_000.0);
        }
        return null;
    }
}
