package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Request DTO for Ollama Generate API Based on Ollama REST API specification
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
        justification = "DTO used as public data carrier for JSON mapping; intentional public fields")
public class OllamaRequestDto {

    @Nullable
    public String model;
    @Nullable
    public String prompt;
    @Nullable
    public Boolean stream; // false for single response
    @Nullable
    public Options options;

    /**
     * Represents options for the Ollama request.
     */
    public static class Options {
        @Nullable
        public Double temperature;
        @Nullable
        @JsonProperty("num_predict")
        public Integer numPredict; // max tokens
        @Nullable
        @JsonProperty("top_p")
        public Double topP;
        @Nullable
        @JsonProperty("top_k")
        public Integer topK;

        public Options() {
        }

        public Options(final Double temperature, final Integer numPredict) {
            this.temperature = temperature;
            this.numPredict = numPredict;
        }
    }

    /**
     * Helper method to create a simple generate request
     */
    public static OllamaRequestDto createGenerateRequest(final String prompt, final String model,
            final Double temperature, final Integer maxTokens) {
        final var request = new OllamaRequestDto();
        request.model = model;
        request.prompt = prompt;
        request.stream = false;
        request.options = new Options(temperature, maxTokens);
        return request;
    }
}
