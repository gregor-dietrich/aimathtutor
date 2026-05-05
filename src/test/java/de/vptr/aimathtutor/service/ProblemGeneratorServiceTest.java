package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.dto.GraspableProblemDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto.ProblemCategory;

class ProblemGeneratorServiceTest {

    private ProblemGeneratorService service;

    @BeforeEach
    void setUp() {
        this.service = new ProblemGeneratorService();
    }

    @ParameterizedTest
    @EnumSource(ProblemCategory.class)
    @DisplayName("Should generate problem for every category at INTERMEDIATE difficulty")
    void shouldGenerateProblemForEveryCategory(final ProblemCategory category) {
        final GraspableProblemDto problem = this.service.generateProblem(DifficultyLevel.INTERMEDIATE, category);

        assertNotNull(problem);
        assertEquals(category, problem.category);
        assertEquals(DifficultyLevel.INTERMEDIATE, problem.difficulty);
        assertNotNull(problem.title);
        assertFalse(problem.title.isBlank());
        assertNotNull(problem.initialExpression);
        assertFalse(problem.initialExpression.isBlank());
        assertNotNull(problem.targetExpression);
        assertFalse(problem.targetExpression.isBlank());
        assertNotNull(problem.allowedOperations);
        assertFalse(problem.allowedOperations.isEmpty());
        assertNotNull(problem.hints);
        assertFalse(problem.hints.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("Should generate linear equation at every difficulty")
    void shouldGenerateLinearEquationAtEveryDifficulty(final DifficultyLevel difficulty) {
        final GraspableProblemDto problem = this.service.generateProblem(difficulty, ProblemCategory.LINEAR_EQUATIONS);

        assertNotNull(problem);
        assertEquals(difficulty, problem.difficulty);
        assertEquals(ProblemCategory.LINEAR_EQUATIONS, problem.category);
        assertTrue(problem.targetExpression.startsWith("x ="));
    }

    @Test
    @DisplayName("Should default to LINEAR_EQUATIONS when category is null")
    void shouldDefaultCategoryWhenNull() {
        final GraspableProblemDto problem = this.service.generateProblem(DifficultyLevel.BEGINNER, null);

        assertNotNull(problem);
        assertEquals(ProblemCategory.LINEAR_EQUATIONS, problem.category);
    }

    @Test
    @DisplayName("Should produce varied problems across invocations")
    void shouldProduceVariedProblemsAcrossInvocations() {
        final Random deterministicRandom = new Random(42L);
        final ProblemGeneratorService testService = new ProblemGeneratorService(deterministicRandom);
        final Set<String> expressions = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            final GraspableProblemDto p = testService.generateProblem(DifficultyLevel.INTERMEDIATE,
                    ProblemCategory.LINEAR_EQUATIONS);
            expressions.add(p.initialExpression);
        }
        assertTrue(expressions.size() > 1, "Expected randomized expressions, got " + expressions.size());
    }

    @Test
    @DisplayName("Should produce factorable quadratic with matching factored form")
    void shouldGenerateFactorableQuadratic() {
        final GraspableProblemDto problem = this.service.generateProblem(DifficultyLevel.BEGINNER,
                ProblemCategory.FACTORING);

        assertTrue(problem.initialExpression.startsWith("x^2"));
        assertTrue(problem.targetExpression.startsWith("(x + "));
        assertTrue(problem.allowedOperations.contains("factor"));
    }

    @Test
    @DisplayName("Should always set sensible operations and hints for exponents")
    void shouldGenerateExponentsProblem() {
        final GraspableProblemDto problem = this.service.generateProblem(DifficultyLevel.ADVANCED,
                ProblemCategory.EXPONENTS);

        assertTrue(problem.initialExpression.contains("x^"));
        assertTrue(problem.targetExpression.startsWith("x^"));
        assertTrue(problem.allowedOperations.contains("simplify"));
    }
}
