package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GraspableMathServiceTest {

    @Inject
    private GraspableMathService graspableMathService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private UserRepository userRepository;

    private Long studentId() {
        final var user = this.userRepository.findByUsername("student1");
        assertNotNull(user, "Seeded student1 user should exist");
        return user.id;
    }

    private Long createExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "seeded teacher must exist");
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "ex_" + suffix;
        dto.content = "content " + suffix;
        dto.userId = teacher.id;
        dto.published = true;
        dto.commentable = false;
        final ExerciseViewDto created = this.exerciseService.createExercise(dto);
        return created.id;
    }

    @Test
    @DisplayName("Should create session with UUID-shaped id and persist it")
    @TestTransaction
    void shouldCreateSession() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());

        assertNotNull(sessionId);
        assertEquals(36, sessionId.length(), "UUID string should be 36 chars");
        final var session = this.graspableMathService.getSession(sessionId);
        assertNotNull(session);
        assertEquals(0, session.actionsCount);
        assertFalse(session.completed);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is missing")
    @TestTransaction
    void shouldThrowWhenUserMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> this.graspableMathService.createSession(999_999L, this.createExercise()));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when exercise is missing")
    @TestTransaction
    void shouldThrowWhenExerciseMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> this.graspableMathService.createSession(this.studentId(), 999_999L));
    }

    @Test
    @DisplayName("Should increment actions count and track correctness on processEvent")
    @TestTransaction
    void shouldProcessEventAndUpdateStats() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());
        final var event = new GraspableEventDto();
        event.sessionId = sessionId;
        event.eventType = "simplify";
        event.expressionAfter = "5x";
        event.correct = true;

        this.graspableMathService.processEvent(event);

        final var session = this.graspableMathService.getSession(sessionId);
        assertEquals(1, session.actionsCount);
        assertEquals(1, session.correctActions);
        assertEquals("5x", session.finalExpression);
    }

    @Test
    @DisplayName("Should compute accuracy from mixed correct/incorrect events")
    @TestTransaction
    void shouldComputeAccuracy() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());

        for (int i = 0; i < 4; i++) {
            final var event = new GraspableEventDto();
            event.sessionId = sessionId;
            event.eventType = "simplify";
            event.correct = i < 3;
            this.graspableMathService.processEvent(event);
        }

        final Double accuracy = this.graspableMathService.getSessionAccuracy(sessionId);
        assertNotNull(accuracy);
        assertEquals(75.0, accuracy, 0.01);
    }

    @Test
    @DisplayName("Should return null accuracy for unknown or empty session")
    @TestTransaction
    void shouldReturnNullAccuracyWithoutEvents() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());
        assertNull(this.graspableMathService.getSessionAccuracy(sessionId));
        assertNull(this.graspableMathService.getSessionAccuracy("not-a-session"));
    }

    @Test
    @DisplayName("Should record hint usage")
    @TestTransaction
    void shouldRecordHintUsed() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());

        this.graspableMathService.recordHintUsed(sessionId);
        this.graspableMathService.recordHintUsed(sessionId);

        final var session = this.graspableMathService.getSession(sessionId);
        assertEquals(2, session.hintsUsed);
    }

    @Test
    @DisplayName("Should mark session as complete")
    @TestTransaction
    void shouldCompleteSession() {
        final String sessionId = this.graspableMathService.createSession(this.studentId(), this.createExercise());

        this.graspableMathService.markSessionComplete(sessionId);

        final var session = this.graspableMathService.getSession(sessionId);
        assertTrue(session.completed);
        assertNotNull(session.endTime);
    }

    @Test
    @DisplayName("Should detect completion when expressions match (whitespace-insensitive)")
    void shouldDetectCompletionForMatchingExpressions() {
        assertTrue(this.graspableMathService.checkCompletion("x = 5", " x=5 "));
        assertTrue(this.graspableMathService.checkCompletion("X = 5", "x=5"));
    }

    @Test
    @DisplayName("Should treat 5=x as equivalent to x=5 after normalization")
    void shouldNormalizeEquationOrdering() {
        assertTrue(this.graspableMathService.checkCompletion("5 = x", "x = 5"));
    }

    @Test
    @DisplayName("Should not detect completion when expressions differ")
    void shouldNotDetectCompletionForDifferentExpressions() {
        assertFalse(this.graspableMathService.checkCompletion("x = 5", "x = 6"));
        assertFalse(this.graspableMathService.checkCompletion(null, "x = 5"));
        assertFalse(this.graspableMathService.checkCompletion("x = 5", null));
    }

    @Test
    @DisplayName("Should list user and exercise sessions")
    @TestTransaction
    void shouldListUserAndExerciseSessions() {
        final Long exerciseId = this.createExercise();
        final Long userId = this.studentId();
        final int userSessionsBefore = this.graspableMathService.getUserSessions(userId).size();
        final int exerciseSessionsBefore = this.graspableMathService.getExerciseSessions(exerciseId).size();

        this.graspableMathService.createSession(userId, exerciseId);

        final int userSessionsAfter = this.graspableMathService.getUserSessions(userId).size();
        final int exerciseSessionsAfter = this.graspableMathService.getExerciseSessions(exerciseId).size();

        assertEquals(userSessionsBefore + 1, userSessionsAfter);
        assertEquals(exerciseSessionsBefore + 1, exerciseSessionsAfter);
    }
}
