package de.vptr.aimathtutor.dto;

import java.util.List;

import jakarta.annotation.Nullable;

/**
 * Response DTO for the Ollama {@code /api/tags} endpoint. Lists installed models.
 */
public class OllamaTagsResponseDto {

    @Nullable
    public List<ModelInfo> models;

    /**
     * Represents a single installed model entry.
     */
    public static class ModelInfo {
        @Nullable
        public String name;
        @Nullable
        public String model;
    }
}
