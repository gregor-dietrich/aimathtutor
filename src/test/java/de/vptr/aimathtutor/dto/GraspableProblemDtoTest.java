package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.dto.GraspableProblemDto.ProblemCategory;

@SuppressWarnings("NullAway")
class GraspableProblemDtoTest {

    @Test
    @DisplayName("Default constructor initializes lists")
    void testDefaultConstructor() {
        final var dto = new GraspableProblemDto();
        assertNull(dto.title);
        assertNull(dto.description);
        assertNull(dto.initialExpression);
        assertNull(dto.targetExpression);
        assertNotNull(dto.allowedOperations);
        assertTrue(dto.allowedOperations.isEmpty());
        assertNull(dto.difficulty);
        assertNull(dto.category);
        assertNotNull(dto.hints);
        assertTrue(dto.hints.isEmpty());
        assertNull(dto.graspableConfig);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new GraspableProblemDto("Solve", "2x + 3 = 7");
        assertEquals("Solve", dto.title);
        assertEquals("2x + 3 = 7", dto.initialExpression);
    }

    @Test
    @DisplayName("ProblemCategory enum values")
    void testProblemCategoryEnum() {
        assertEquals(8, ProblemCategory.values().length);
        assertEquals("Linear Equations", ProblemCategory.LINEAR_EQUATIONS.getDisplayName());
        assertEquals("algebra", ProblemCategory.LINEAR_EQUATIONS.getTopic());
        assertEquals("Quadratic Equations", ProblemCategory.QUADRATIC_EQUATIONS.getDisplayName());
        assertEquals("Fraction Operations", ProblemCategory.FRACTIONS.getDisplayName());
        assertEquals("Inequalities", ProblemCategory.INEQUALITIES.getDisplayName());
    }

    @Test
    @DisplayName("ProblemCategory toString returns displayName")
    void testProblemCategoryToString() {
        assertEquals("Linear Equations", ProblemCategory.LINEAR_EQUATIONS.toString());
        assertEquals("Factoring", ProblemCategory.FACTORING.toString());
    }

    @Test
    @DisplayName("toString returns summary")
    void testToString() {
        final var dto = new GraspableProblemDto("Test", "1+1");
        dto.targetExpression = "2";
        dto.difficulty = DifficultyLevel.BEGINNER;
        final var str = dto.toString();
        assertTrue(str.contains("GraspableProblemDto"));
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("1+1"));
    }
}
