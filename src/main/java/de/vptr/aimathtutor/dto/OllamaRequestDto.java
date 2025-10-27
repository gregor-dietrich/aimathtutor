package de.vptr.aimathtutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Request DTO for Ollama Generate API
 * Based on Ollama REST API specification
 */
@SuppressFBWarnings(value = { "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
        "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "DTO used as public data carrier for JSON mapping; intentional public fields")
public class OllamaRequestDto {

    public String model;
    public String prompt;
    public Boolean stream; // false for single response
    public Options options;

    public static class Options {
        public Double temperature;
        @JsonProperty("num_predict")
        public Integer numPredict; // max tokens
        @JsonProperty("top_p")
        public Double topP;
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
            final Double temperature,
            final Integer maxTokens) {
        final var request = new OllamaRequestDto();
        request.model = model;
        request.prompt = prompt;
        request.stream = false;
        request.options = new Options(temperature, maxTokens);
        return request;
    }
}
