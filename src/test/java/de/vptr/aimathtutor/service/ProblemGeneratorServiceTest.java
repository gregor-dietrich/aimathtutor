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

@SuppressWarnings("NullAway")
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
    @SuppressWarnings("NullAway")
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
            final GraspableProblemDto p =
                    testService.generateProblem(DifficultyLevel.INTERMEDIATE, ProblemCategory.LINEAR_EQUATIONS);
            expressions.add(p.initialExpression);
        }
        assertTrue(expressions.size() > 1, "Expected randomized expressions, got " + expressions.size());
    }

    // The four tests below pin every branch that generateProblem takes on a *random* value:
    // the "a == 1" and "b >= 0" ternaries in LINEAR_EQUATIONS, and the "bIneq >= 0" ternary
    // plus the "reducedDen == 1" if/else in INEQUALITIES. Without them those arms are only
    // reached when the dice happen to land there, which leaves the rendering unverified and
    // makes the JaCoCo branch count fluctuate between runs.
    //
    // The seeds are hard-coded because the constructor re-seeds from the supplied Random
    // (new Random(random.nextLong())), so a stub returning scripted values cannot be injected
    // -- the seed is the only lever. The JDK's Random is a contractually specified linear
    // congruential generator, so a seed always yields the same sequence on every JVM.

    @Test
    @DisplayName("Should omit the coefficient when the linear coefficient is 1")
    void shouldOmitCoefficientWhenLinearCoefficientIsOne() {
        // seed 1 -> a=1, b=-5, x=2: exercises the "a == 1" and "b < 0" arms
        final ProblemGeneratorService seeded = new ProblemGeneratorService(new Random(1L));
        final GraspableProblemDto problem =
                seeded.generateProblem(DifficultyLevel.BEGINNER, ProblemCategory.LINEAR_EQUATIONS);

        assertEquals("x - 5 = -3", problem.initialExpression);
        assertEquals("x = 2", problem.targetExpression);
    }

    @Test
    @DisplayName("Should render the coefficient and a plus sign when the linear coefficient exceeds 1")
    void shouldRenderCoefficientWhenLinearCoefficientExceedsOne() {
        // seed 2 -> a=4, b=4, x=-2: exercises the "a != 1" and "b >= 0" arms
        final ProblemGeneratorService seeded = new ProblemGeneratorService(new Random(2L));
        final GraspableProblemDto problem =
                seeded.generateProblem(DifficultyLevel.BEGINNER, ProblemCategory.LINEAR_EQUATIONS);

        assertEquals("4x + 4 = -4", problem.initialExpression);
        assertEquals("x = -2", problem.targetExpression);
    }

    @Test
    @DisplayName("Should render a whole-number bound when the inequality divides evenly")
    void shouldRenderWholeNumberBoundForInequality() {
        // seed 1 -> a=1, b=-5, c=6: exercises the "bIneq < 0" and "reducedDen == 1" arms
        final ProblemGeneratorService seeded = new ProblemGeneratorService(new Random(1L));
        final GraspableProblemDto problem =
                seeded.generateProblem(DifficultyLevel.BEGINNER, ProblemCategory.INEQUALITIES);

        assertEquals("1x - 5 < 6", problem.initialExpression);
        assertEquals("x < 11", problem.targetExpression);
    }

    @Test
    @DisplayName("Should render a reduced fraction bound when the inequality does not divide evenly")
    void shouldRenderFractionalBoundForInequality() {
        // seed 4 -> a=4, b=0, c=3: exercises the "bIneq >= 0" and "reducedDen != 1" arms
        final ProblemGeneratorService seeded = new ProblemGeneratorService(new Random(4L));
        final GraspableProblemDto problem =
                seeded.generateProblem(DifficultyLevel.BEGINNER, ProblemCategory.INEQUALITIES);

        assertEquals("4x + 0 < 3", problem.initialExpression);
        assertEquals("x < 3/4", problem.targetExpression);
    }

    @Test
    @DisplayName("Should produce factorable quadratic with matching factored form")
    void shouldGenerateFactorableQuadratic() {
        final GraspableProblemDto problem =
                this.service.generateProblem(DifficultyLevel.BEGINNER, ProblemCategory.FACTORING);

        assertTrue(problem.initialExpression.startsWith("x^2"));
        assertTrue(problem.targetExpression.startsWith("(x + "));
        assertTrue(problem.allowedOperations.contains("factor"));
    }

    @Test
    @DisplayName("Should always set sensible operations and hints for exponents")
    void shouldGenerateExponentsProblem() {
        final GraspableProblemDto problem =
                this.service.generateProblem(DifficultyLevel.ADVANCED, ProblemCategory.EXPONENTS);

        assertTrue(problem.initialExpression.contains("x^"));
        assertTrue(problem.targetExpression.startsWith("x^"));
        assertTrue(problem.allowedOperations.contains("simplify"));
    }
}
