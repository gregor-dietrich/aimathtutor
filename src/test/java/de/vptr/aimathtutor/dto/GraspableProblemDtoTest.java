package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraspableProblemDtoTest {

    private GraspableProblemDto problemDto;

    @BeforeEach
    void setUp() {
        this.problemDto = new GraspableProblemDto();
    }

    @Test
    @DisplayName("Should create GraspableProblemDto with default constructor")
    void shouldCreateGraspableProblemDtoWithDefaultConstructor() {
        // Then
        assertNotNull(this.problemDto.allowedOperations);
        assertNotNull(this.problemDto.hints);
        assertTrue(this.problemDto.allowedOperations.isEmpty());
        assertTrue(this.problemDto.hints.isEmpty());
        assertNull(this.problemDto.title);
        assertNull(this.problemDto.initialExpression);
    }

    @Test
    @DisplayName("Should create GraspableProblemDto with parameterized constructor")
    void shouldCreateGraspableProblemDtoWithParameterizedConstructor() {
        // When
        final GraspableProblemDto dto = new GraspableProblemDto(
                "Solve for x",
                "2x + 5 = 13");

        // Then
        assertEquals("Solve for x", dto.title);
        assertEquals("2x + 5 = 13", dto.initialExpression);
        assertNotNull(dto.allowedOperations);
        assertNotNull(dto.hints);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.problemDto.title = "Factor the expression";
        this.problemDto.description = "Find the factors";
        this.problemDto.initialExpression = "x^2 + 5x + 6";
        this.problemDto.targetExpression = "(x + 2)(x + 3)";
        this.problemDto.difficulty = "intermediate";
        this.problemDto.graspableConfig = "{\"mode\": \"algebra\"}";

        // Then
        assertEquals("Factor the expression", this.problemDto.title);
        assertEquals("Find the factors", this.problemDto.description);
        assertEquals("x^2 + 5x + 6", this.problemDto.initialExpression);
        assertEquals("(x + 2)(x + 3)", this.problemDto.targetExpression);
        assertEquals("intermediate", this.problemDto.difficulty);
        assertEquals("{\"mode\": \"algebra\"}", this.problemDto.graspableConfig);
    }

    @Test
    @DisplayName("Should add allowed operations")
    void shouldAddAllowedOperations() {
        // When
        this.problemDto.allowedOperations.add("simplify");
        this.problemDto.allowedOperations.add("expand");
        this.problemDto.allowedOperations.add("factor");

        // Then
        assertEquals(3, this.problemDto.allowedOperations.size());
        assertTrue(this.problemDto.allowedOperations.contains("simplify"));
        assertTrue(this.problemDto.allowedOperations.contains("expand"));
        assertTrue(this.problemDto.allowedOperations.contains("factor"));
    }

    @Test
    @DisplayName("Should add hints")
    void shouldAddHints() {
        // When
        this.problemDto.hints.add("Look for common factors");
        this.problemDto.hints.add("Try the FOIL method");

        // Then
        assertEquals(2, this.problemDto.hints.size());
        assertTrue(this.problemDto.hints.contains("Look for common factors"));
        assertTrue(this.problemDto.hints.contains("Try the FOIL method"));
    }

    @Test
    @DisplayName("Should generate toString with key fields")
    void shouldGenerateToStringWithKeyFields() {
        // Given
        this.problemDto.title = "Simplify";
        this.problemDto.initialExpression = "3x + 2x";
        this.problemDto.targetExpression = "5x";
        this.problemDto.difficulty = "beginner";

        // When
        final String result = this.problemDto.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Simplify"));
        assertTrue(result.contains("3x + 2x"));
        assertTrue(result.contains("5x"));
        assertTrue(result.contains("beginner"));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // When
        this.problemDto.title = null;
        this.problemDto.initialExpression = null;
        this.problemDto.targetExpression = null;
        this.problemDto.difficulty = null;

        // Then
        assertNull(this.problemDto.title);
        assertNull(this.problemDto.initialExpression);
        assertDoesNotThrow(() -> this.problemDto.toString());
    }

    @Test
    @DisplayName("Should support different difficulty levels")
    void shouldSupportDifferentDifficultyLevels() {
        // Test different difficulty settings
        this.problemDto.difficulty = "beginner";
        assertEquals("beginner", this.problemDto.difficulty);

        this.problemDto.difficulty = "intermediate";
        assertEquals("intermediate", this.problemDto.difficulty);

        this.problemDto.difficulty = "advanced";
        assertEquals("advanced", this.problemDto.difficulty);
    }
}
