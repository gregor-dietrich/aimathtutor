package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/**
 * Response DTO for Ollama Generate API Based on Ollama REST API specification
 */
public class OllamaResponseDto {

    @Nullable
    public String model;
    @Nullable
    @JsonProperty("created_at")
    public String createdAt;
    @Nullable
    public String response;
    @Nullable
    public Boolean done;
    @Nullable
    @JsonProperty("done_reason")
    public String doneReason;
    @Nullable
    @JsonProperty("total_duration")
    public Long totalDuration;
    @Nullable
    @JsonProperty("load_duration")
    public Long loadDuration;
    @Nullable
    @JsonProperty("prompt_eval_count")
    public Integer promptEvalCount;
    @Nullable
    @JsonProperty("eval_count")
    public Integer evalCount;
    @Nullable
    @JsonProperty("eval_duration")
    public Long evalDuration;

    /**
     * Extract the text content
     */
    @Nullable
    public String getTextContent() {
        return this.response;
    }

    /**
     * Check if the response is complete
     */
    public boolean isComplete() {
        return this.done != null && this.done;
    }

    /**
     * Check if the response was truncated due to the max-tokens limit. Ollama reports {@code "length"} as the
     * {@code done_reason} when generation was cut off by {@code num_predict}.
     */
    public boolean isTruncated() {
        return "length".equalsIgnoreCase(this.doneReason);
    }

    /**
     * Get tokens per second (if available)
     */
    @Nullable
    public Double getTokensPerSecond() {
        if (this.evalCount != null && this.evalDuration != null && this.evalDuration > 0) {
            // evalDuration is in nanoseconds
            return this.evalCount / (this.evalDuration / 1_000_000_000.0);
        }
        return null;
    }
}
