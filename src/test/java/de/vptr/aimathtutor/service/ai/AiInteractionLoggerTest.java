package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.PermissionService;
import de.vptr.aimathtutor.util.TestExerciseFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class AiInteractionLoggerTest {

    @Inject
    AiInteractionLogger aiInteractionLogger;

    @Inject
    AiInteractionRepository aiInteractionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseService exerciseService;

    @InjectMock
    PermissionService permissionService;

    private Long teacherId() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher must exist");
        return teacher.id;
    }

    private Long createExerciseId() {
        return TestExerciseFactory.createExercise(this.userRepository, this.exerciseService).id;
    }

    @Test
    @DisplayName("logInteraction persists one AI interaction record")
    @TestTransaction
    void testLogInteraction_persistsRecord() {
        final long countBefore = this.aiInteractionRepository.findAll().size();

        final var event = new GraspableEventDto();
        event.sessionId = "test-session-" + UUID.randomUUID();
        event.eventType = "simplify";
        event.expressionBefore = "2x+2";
        event.expressionAfter = "2(x+1)";
        event.correct = true;
        event.studentId = this.teacherId();
        event.exerciseId = this.createExerciseId();

        final var feedback = AiFeedbackDto.positive("Correct!");

        this.aiInteractionLogger.logInteraction(event, feedback);

        final long countAfter = this.aiInteractionRepository.findAll().size();
        assertEquals(countBefore + 1, countAfter, "One interaction record should be persisted");
    }

    @Test
    @DisplayName("logInteraction works without user or exercise ids")
    @TestTransaction
    void testLogInteraction_noUserOrExercise() {
        final long countBefore = this.aiInteractionRepository.findAll().size();

        final var event = new GraspableEventDto();
        event.sessionId = "anon-session-" + UUID.randomUUID();
        event.eventType = "expand";
        event.correct = false;
        // studentId and exerciseId intentionally null

        final var feedback = AiFeedbackDto.corrective("Check your work.");

        this.aiInteractionLogger.logInteraction(event, feedback);

        final long countAfter = this.aiInteractionRepository.findAll().size();
        assertEquals(countBefore + 1, countAfter, "Record should be persisted even without user/exercise ids");
    }

    @Test
    @DisplayName("logInteraction sets correct field values on persisted record")
    @TestTransaction
    void testLogInteraction_setsCorrectFields() {
        final String sessionId = "fields-session-" + UUID.randomUUID();
        final var event = new GraspableEventDto();
        event.sessionId = sessionId;
        event.eventType = "factor";
        event.expressionBefore = "x^2";
        event.expressionAfter = "x*x";
        event.correct = true;

        final var feedback = AiFeedbackDto.hint("Good step!");

        this.aiInteractionLogger.logInteraction(event, feedback);

        final var records = this.aiInteractionRepository.findBySessionId(sessionId);
        assertNotNull(records);
        assertTrue(records.stream().anyMatch(r -> sessionId.equals(r.sessionId)),
                "Persisted record should have matching sessionId");
    }

    @Test
    @DisplayName("logQuestionInteraction persists two records (question + answer)")
    @TestTransaction
    void testLogQuestionInteraction_persistsTwoRecords() {
        final long countBefore = this.aiInteractionRepository.findAll().size();
        final String sessionId = "q-session-" + UUID.randomUUID();

        this.aiInteractionLogger.logQuestionInteraction(sessionId, this.teacherId(), this.createExerciseId(),
                "What is the next step?", "Try factoring the expression.");

        final long countAfter = this.aiInteractionRepository.findAll().size();
        assertEquals(countBefore + 2, countAfter, "logQuestionInteraction should persist exactly two records");
    }

    @Test
    @DisplayName("logQuestionInteraction works with null user and exercise ids")
    @TestTransaction
    void testLogQuestionInteraction_noUserOrExercise() {
        final long countBefore = this.aiInteractionRepository.findAll().size();
        final String sessionId = "q-anon-session-" + UUID.randomUUID();

        this.aiInteractionLogger.logQuestionInteraction(sessionId, null, null, "Help me.", "Sure, here is help.");

        final long countAfter = this.aiInteractionRepository.findAll().size();
        assertEquals(countBefore + 2, countAfter, "Two records should be persisted even without ids");
    }

    @Test
    @DisplayName("logQuestionInteraction records have correct event types")
    @TestTransaction
    void testLogQuestionInteraction_eventTypes() {
        final String sessionId = "evt-session-" + UUID.randomUUID();

        this.aiInteractionLogger.logQuestionInteraction(sessionId, null, null, "My question.", "My answer.");

        final var records = this.aiInteractionRepository.findBySessionId(sessionId);
        assertNotNull(records);
        assertEquals(2, records.size(), "Exactly two records for this session");
        assertTrue(records.stream().anyMatch(r -> "QUESTION".equals(r.eventType)),
                "One record should have eventType QUESTION");
        assertTrue(records.stream().anyMatch(r -> "QUESTION_ANSWER".equals(r.eventType)),
                "One record should have eventType QUESTION_ANSWER");
    }
}
