package de.vptr.aimathtutor.dto;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Response DTO for the Ollama {@code /api/tags} endpoint.
 * Lists installed models.
 */
public class OllamaTagsResponseDto {

    @SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Populated by Jackson at runtime")
    public List<ModelInfo> models;

    /**
     * Represents a single installed model entry.
     */
    public static class ModelInfo {
        @SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Populated by Jackson at runtime")
        public String name;
        @SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Populated by Jackson at runtime")
        public String model;
    }
}
