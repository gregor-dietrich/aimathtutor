package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestExerciseFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

@QuarkusTest
@SuppressWarnings("NullAway")
class GraspableMathServiceTest {

    @Inject
    private GraspableMathService graspableMathService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private UserRepository userRepository;

    @InjectMock
    private PermissionService permissionService;

    @InjectMock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Mockito.doNothing().when(this.permissionService).requireExerciseAdd();
        final var teacher = this.userRepository.findByUsername("teacher");
        Mockito.when(this.authService.getCurrentUserEntity()).thenReturn(teacher);
        Mockito.when(this.authService.requireCurrentUser()).thenReturn(this.student());
    }

    private UserEntity student() {
        final var user = this.userRepository.findByUsername("student1");
        assertNotNull(user, "Seeded student1 user should exist");
        return user;
    }

    private Long studentId() {
        final var user = this.userRepository.findByUsername("student1");
        assertNotNull(user, "Seeded student1 user should exist");
        return user.id;
    }

    private Long createExercise() {
        return TestExerciseFactory.createExercise(this.userRepository, this.exerciseService).id;
    }

    @Test
    @DisplayName("Should create session with UUID-shaped id and persist it")
    @TestTransaction
    void shouldCreateSession() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());

        assertNotNull(sessionId);
        assertEquals(36, sessionId.length(), "UUID string should be 36 chars");
        final var session = this.graspableMathService.getSession(sessionId);
        assertNotNull(session);
        assertEquals(0, session.actionsCount);
        assertFalse(session.completed);
    }

    @Test
    @DisplayName("Should refuse createSession when the session has ended")
    @TestTransaction
    void shouldRefuseCreateSessionWhenSessionEnded() {
        final Long exerciseId = this.createExercise();
        Mockito.when(this.authService.requireCurrentUser())
                .thenThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE));
        final long before = this.graspableMathService.getExerciseSessions(exerciseId).size();

        assertEquals(AppConstants.SESSION_ENDED_MESSAGE,
                assertThrows(ValidationException.class, () -> this.graspableMathService.createSession(exerciseId))
                        .getMessage());
        assertEquals(before, this.graspableMathService.getExerciseSessions(exerciseId).size());
    }

    @Test
    @DisplayName("Should refuse writes on another user's session and an ended session, leaving counters unchanged")
    @TestTransaction
    void shouldRefuseWritesByNonOwnerOrEndedSession() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());
        final var event = new GraspableEventDto();
        event.sessionId = sessionId;
        event.eventType = "simplify";
        event.correct = true;

        // another user
        final var teacher = this.userRepository.findByUsername("teacher");
        Mockito.when(this.authService.requireCurrentUser()).thenReturn(teacher);
        this.assertWritesRefused(sessionId, event);

        // ended session
        Mockito.when(this.authService.requireCurrentUser())
                .thenThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE));
        this.assertWritesRefused(sessionId, event);

        final var session = this.graspableMathService.getSession(sessionId);
        assertEquals(0, session.actionsCount);
        assertEquals(0, session.correctActions);
        assertEquals(0, session.hintsUsed);
        assertFalse(session.completed);
        assertNull(session.endTime);
    }

    private void assertWritesRefused(final String sessionId, final GraspableEventDto event) {
        for (final Executable write : new Executable[] { () -> this.graspableMathService.processEvent(event),
                () -> this.graspableMathService.recordHintUsed(sessionId),
                () -> this.graspableMathService.markSessionComplete(sessionId),
                () -> this.graspableMathService.completeSession(sessionId) }) {
            assertEquals(AppConstants.SESSION_ENDED_MESSAGE,
                    assertThrows(ValidationException.class, write).getMessage());
        }
    }

    @Test
    @DisplayName("Should silently ignore writes on an unknown session")
    @TestTransaction
    void shouldIgnoreWritesOnUnknownSession() {
        final var event = new GraspableEventDto();
        event.sessionId = "not-a-session";
        this.graspableMathService.processEvent(event);
        this.graspableMathService.recordHintUsed("not-a-session");
        this.graspableMathService.markSessionComplete("not-a-session");
        this.graspableMathService.completeSession("not-a-session");
        assertNull(this.graspableMathService.getSession("not-a-session"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when exercise is missing")
    @TestTransaction
    void shouldThrowWhenExerciseMissing() {
        assertThrows(IllegalArgumentException.class, () -> this.graspableMathService.createSession(999_999L));
    }

    @Test
    @DisplayName("Should increment actions count and track correctness on processEvent")
    @TestTransaction
    void shouldProcessEventAndUpdateStats() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());
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
        final String sessionId = this.graspableMathService.createSession(this.createExercise());

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
        final String sessionId = this.graspableMathService.createSession(this.createExercise());
        assertNull(this.graspableMathService.getSessionAccuracy(sessionId));
        assertNull(this.graspableMathService.getSessionAccuracy("not-a-session"));
    }

    @Test
    @DisplayName("Should record hint usage")
    @TestTransaction
    void shouldRecordHintUsed() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());

        this.graspableMathService.recordHintUsed(sessionId);
        this.graspableMathService.recordHintUsed(sessionId);

        final var session = this.graspableMathService.getSession(sessionId);
        assertEquals(2, session.hintsUsed);
    }

    @Test
    @DisplayName("Should mark session as complete")
    @TestTransaction
    void shouldCompleteSession() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());

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

        this.graspableMathService.createSession(exerciseId);

        final int userSessionsAfter = this.graspableMathService.getUserSessions(userId).size();
        final int exerciseSessionsAfter = this.graspableMathService.getExerciseSessions(exerciseId).size();

        assertEquals(userSessionsBefore + 1, userSessionsAfter);
        assertEquals(exerciseSessionsBefore + 1, exerciseSessionsAfter);
    }

    @Test
    @DisplayName("checkCompletion returns false for both null inputs")
    void testCheckCompletion_bothNull() {
        assertFalse(this.graspableMathService.checkCompletion(null, null));
    }

    @Test
    @DisplayName("completeSession marks session completed with endTime set")
    @TestTransaction
    void testCompleteSession() {
        final String sessionId = this.graspableMathService.createSession(this.createExercise());

        this.graspableMathService.completeSession(sessionId);

        final var session = this.graspableMathService.getSession(sessionId);
        assertNotNull(session);
        assertTrue(session.completed);
        assertNotNull(session.endTime);
    }

    @Test
    @DisplayName("getSession returns null for unknown session id")
    @TestTransaction
    void testGetSession_notFound() {
        assertNull(this.graspableMathService.getSession(UUID.randomUUID().toString()));
    }
}
