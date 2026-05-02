package de.vptr.aimathtutor.service;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.GraspableProblemDto;
import de.vptr.aimathtutor.enums.DifficultyLevel;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for generating random math problems for the Graspable Math workspace.
 *
 * <p>Extracted from {@link AiTutorService} to separate problem generation from
 * AI tutoring orchestration.</p>
 */
@ApplicationScoped
public class ProblemGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemGeneratorService.class);

    /**
     * Generates a new math problem based on the given category.
     *
     * @param difficulty The difficulty level
     * @param category   The problem category (type of math problem)
     * @return A new Graspable Math problem
     */
    public GraspableProblemDto generateProblem(final DifficultyLevel difficulty,
            final GraspableProblemDto.ProblemCategory category) {
        LOG.debug("Generating problem: difficulty={}, category={}", difficulty, category);

        final var problem = new GraspableProblemDto();
        problem.difficulty = difficulty;
        problem.category = category != null ? category : GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS;

        // Generate random problems based on category
        // ThreadLocalRandom is used (not SecureRandom) because:
        // - Problem generation does not require cryptographic security
        // - ThreadLocalRandom is more efficient for concurrent access
        // - Each thread has its own random instance, avoiding contention
        // - No initialization overhead compared to SecureRandom
        final var random = ThreadLocalRandom.current();

        // Difficulty-based parameter scaling
        final int coefMax = switch (difficulty) {
            case BEGINNER -> 4;
            case INTERMEDIATE -> 9;
            case ADVANCED -> 15;
            case EXPERT -> 20;
        };
        final int constMax = switch (difficulty) {
            case BEGINNER -> 6;
            case INTERMEDIATE -> 10;
            case ADVANCED -> 20;
            case EXPERT -> 30;
        };
        final int varMax = switch (difficulty) {
            case BEGINNER -> 6;
            case INTERMEDIATE -> 10;
            case ADVANCED -> 20;
            case EXPERT -> 30;
        };

        switch (problem.category) {
            case LINEAR_EQUATIONS -> {
                // Generate random linear equation: ax + b = c
                final int a = random.nextInt(coefMax) + 1; // 1 to coefMax
                final int b = random.nextInt(constMax * 2 + 1) - constMax; // -constMax to constMax
                final int x = random.nextInt(varMax * 2 + 1) - varMax; // -varMax to varMax
                final int c = a * x + b;
                problem.title = "Solve for x";
                problem.initialExpression = String.format("%dx %s %d = %d",
                        a,
                        b >= 0 ? "+" : "-",
                        Math.abs(b),
                        c);
                problem.targetExpression = "x = " + x;
                problem.allowedOperations.addAll(Arrays.asList("simplify", "move", "divide"));
                problem.hints.add("First, isolate the term with x");
                problem.hints.add("Remember to do the same operation on both sides");
            }
            case QUADRATIC_EQUATIONS -> {
                // Generate simple quadratic: x^2 = n (perfect square)
                final int sqrtMax = switch (difficulty) {
                    case BEGINNER -> 5;
                    case INTERMEDIATE -> 10;
                    case ADVANCED -> 15;
                    case EXPERT -> 20;
                };
                final int sqrtVal = random.nextInt(sqrtMax) + 1; // 1 to sqrtMax
                final int nSquared = sqrtVal * sqrtVal;
                problem.title = "Solve for x";
                problem.initialExpression = String.format("x^2 = %d", nSquared);
                problem.targetExpression = String.format("x = ±%d", sqrtVal);
                problem.allowedOperations.addAll(Arrays.asList("sqrt", "simplify"));
                problem.hints.add("Take the square root of both sides");
                problem.hints.add("Remember there are two solutions: positive and negative");
            }
            case POLYNOMIAL_SIMPLIFICATION -> {
                // Generate random simplification
                final int coef1 = random.nextInt(coefMax) + 1;
                final int coef2 = random.nextInt(coefMax) + 1;
                problem.title = "Simplify the expression";
                problem.initialExpression = String.format("%dx + %dx", coef1, coef2);
                problem.targetExpression = (coef1 + coef2) + "x";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "combine"));
                problem.hints.add("Combine like terms");
                problem.hints.add("Add the coefficients of x");
            }
            case FACTORING -> {
                // Generate random factorable quadratic
                final int factorMax = switch (difficulty) {
                    case BEGINNER -> 5;
                    case INTERMEDIATE -> 9;
                    case ADVANCED -> 15;
                    case EXPERT -> 20;
                };
                final int p = random.nextInt(factorMax) + 1;
                final int q = random.nextInt(factorMax) + 1;
                final int sum = p + q;
                final int product = p * q;
                problem.title = "Factor the expression";
                problem.initialExpression = String.format("x^2 + %dx + %d", sum, product);
                problem.targetExpression = String.format("(x + %d)(x + %d)", p, q);
                problem.allowedOperations.addAll(Arrays.asList("factor", "expand"));
                problem.hints
                        .add(String.format("Look for two numbers that multiply to %d and add to %d", product, sum));
            }
            case FRACTIONS -> {
                // Generate fraction addition: a/b + c/d
                final int fracMax = switch (difficulty) {
                    case BEGINNER -> 3;
                    case INTERMEDIATE -> 5;
                    case ADVANCED -> 9;
                    case EXPERT -> 12;
                };
                final int num1 = random.nextInt(fracMax) + 1;
                final int den1 = random.nextInt(fracMax) + 2;
                final int num2 = random.nextInt(fracMax) + 1;
                final int den2 = random.nextInt(fracMax) + 2;
                problem.title = "Add the fractions";
                problem.initialExpression = String.format("%d/%d + %d/%d", num1, den1, num2, den2);
                problem.targetExpression = "Simplified form";
                problem.allowedOperations.addAll(Arrays.asList("simplify", "add"));
                problem.hints.add("Find a common denominator");
                problem.hints.add("Add the numerators");
            }
            case EXPONENTS -> {
                // Generate exponent simplification: x^a * x^b = x^(a+b)
                final int expMax = switch (difficulty) {
                    case BEGINNER -> 3;
                    case INTERMEDIATE -> 5;
                    case ADVANCED -> 8;
                    case EXPERT -> 12;
                };
                final int exp1 = random.nextInt(expMax) + 2; // 2 to expMax+1
                final int exp2 = random.nextInt(expMax) + 2; // 2 to expMax+1
                problem.title = "Simplify using exponent rules";
                problem.initialExpression = String.format("x^%d * x^%d", exp1, exp2);
                problem.targetExpression = String.format("x^%d", exp1 + exp2);
                problem.allowedOperations.addAll(Arrays.asList("simplify", "multiply"));
                problem.hints.add("When multiplying powers with the same base, add the exponents");
                problem.hints.add(String.format("x^%d * x^%d = x^(%d+%d)", exp1, exp2, exp1, exp2));
            }
            case SYSTEMS_OF_EQUATIONS -> {
                // Generate simple system (substitution method)
                final int sysMax = switch (difficulty) {
                    case BEGINNER -> 5;
                    case INTERMEDIATE -> 10;
                    case ADVANCED -> 20;
                    case EXPERT -> 30;
                };
                final int yVal = random.nextInt(sysMax) + 1;
                final int xVal = random.nextInt(sysMax) + 1;
                final int coefX = random.nextInt(coefMax) + 1;
                problem.title = "Solve the system of equations";
                problem.initialExpression = String.format("y = %d; %dx + y = %d", yVal, coefX, coefX * xVal + yVal);
                problem.targetExpression = String.format("x = %d; y = %d", xVal, yVal);
                problem.allowedOperations.addAll(Arrays.asList("substitute", "solve", "simplify"));
                problem.hints.add("Substitute the value of y from the first equation into the second");
                problem.hints.add("Solve for x, then verify with y");
            }
            case INEQUALITIES -> {
                // Generate simple inequality: ax + b < c
                final int aIneq = random.nextInt(coefMax) + 1;
                final int bIneq = random.nextInt(constMax * 2 + 1) - constMax;
                final int cIneq = random.nextInt(constMax * 4);
                problem.title = "Solve the inequality";
                problem.initialExpression = String.format("%dx %s %d < %d",
                        aIneq,
                        bIneq >= 0 ? "+" : "-",
                        Math.abs(bIneq),
                        cIneq);
                final int numerator = cIneq - bIneq;
                final int denominator = aIneq;
                final int gcd = java.math.BigInteger.valueOf(numerator)
                        .gcd(java.math.BigInteger.valueOf(denominator)).intValueExact();
                final int reducedNum = numerator / gcd;
                final int reducedDen = denominator / gcd;
                final String targetValue;
                if (reducedDen == 1) {
                    targetValue = String.valueOf(reducedNum);
                } else {
                    targetValue = reducedNum + "/" + reducedDen;
                }
                problem.targetExpression = "x < " + targetValue;
                problem.allowedOperations.addAll(Arrays.asList("simplify", "move", "divide"));
                problem.hints.add("Solve like an equation, but keep the inequality sign");
                problem.hints.add("Remember: if dividing by a negative number, flip the inequality");
            }
            default -> {
                // Fallback to linear equations
                return this.generateProblem(difficulty, GraspableProblemDto.ProblemCategory.LINEAR_EQUATIONS);
            }
        }

        return problem;
    }
}
