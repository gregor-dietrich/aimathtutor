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

    /**
     * Enum for different math problem categories.
     */
    public enum ProblemCategory {
        LINEAR_EQUATIONS("Linear Equations", "algebra"),
        QUADRATIC_EQUATIONS("Quadratic Equations", "algebra"),
        POLYNOMIAL_SIMPLIFICATION("Polynomial Simplification", "algebra"),
        FACTORING("Factoring", "algebra"),
        FRACTIONS("Fraction Operations", "arithmetic"),
        EXPONENTS("Exponent Rules", "algebra"),
        SYSTEMS_OF_EQUATIONS("Systems of Equations", "algebra"),
        INEQUALITIES("Inequalities", "algebra");

        private final String displayName;
        private final String topic;

        ProblemCategory(final String displayName, final String topic) {
            this.displayName = displayName;
            this.topic = topic;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getTopic() {
            return this.topic;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public String title;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", justification = "Description is optional and used by front-end; public for JSON mapping")
    public String description;

    @JsonProperty("initial_expression")
    public String initialExpression; // The starting mathematical expression

    @JsonProperty("target_expression")
    public String targetExpression; // The goal expression (if any)

    @JsonProperty("allowed_operations")
    public List<String> allowedOperations; // e.g., ["simplify", "expand", "factor"]

    public String difficulty; // e.g., "beginner", "intermediate", "advanced"

    public ProblemCategory category; // The problem category/type

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
                "title='" + this.title + '\'' +
                ", initialExpression='" + this.initialExpression + '\'' +
                ", targetExpression='" + this.targetExpression + '\'' +
                ", difficulty='" + this.difficulty + '\'' +
                '}';
    }
}
