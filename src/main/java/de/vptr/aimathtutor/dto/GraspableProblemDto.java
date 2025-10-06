package de.vptr.aimathtutor.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Graspable Math problem definition.
 * This DTO is used to initialize the Graspable Math canvas with a specific
 * problem.
 */
public class GraspableProblemDto {

    public String title;

    public String description;

    @JsonProperty("initial_expression")
    public String initialExpression; // The starting mathematical expression

    @JsonProperty("target_expression")
    public String targetExpression; // The goal expression (if any)

    @JsonProperty("allowed_operations")
    public List<String> allowedOperations; // e.g., ["simplify", "expand", "factor"]

    public String difficulty; // e.g., "beginner", "intermediate", "advanced"

    public List<String> hints; // Pre-defined hints for this problem

    @JsonProperty("graspable_config")
    public String graspableConfig; // JSON configuration for Graspable Math widget

    public GraspableProblemDto() {
        this.allowedOperations = new ArrayList<>();
        this.hints = new ArrayList<>();
    }

    public GraspableProblemDto(final String title, final String initialExpression) {
        this();
        this.title = title;
        this.initialExpression = initialExpression;
    }

    @Override
    public String toString() {
        return "GraspableProblemDto{" +
                "title='" + title + '\'' +
                ", initialExpression='" + initialExpression + '\'' +
                ", targetExpression='" + targetExpression + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}
