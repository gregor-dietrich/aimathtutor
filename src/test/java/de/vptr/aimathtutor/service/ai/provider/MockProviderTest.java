package de.vptr.aimathtutor.service.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.AiFeedbackDto.FeedbackType;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class MockProviderTest {

    @Inject
    MockProvider provider;

    @Test
    @DisplayName("isAvailable always returns true")
    void testIsAvailable() {
        assertTrue(this.provider.isAvailable());
    }

    @Test
    @DisplayName("analyzeMathAction throws for null event")
    void testAnalyzeMathAction_nullEvent() {
        assertThrows(IllegalArgumentException.class, () -> this.provider.analyzeMathAction(null, null));
    }

    @Test
    @DisplayName("analyzeMathAction returns POSITIVE feedback for correct simplify")
    void testAnalyzeMathAction_simplify_correct() {
        final var event = new GraspableEventDto("simplify", "x+1", "x+1", 1L, 1L, "s1");
        event.correct = true;
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.POSITIVE, feedback.type);
        assertNotNull(feedback.message);
        assertFalse(feedback.message.isBlank());
    }

    @Test
    @DisplayName("analyzeMathAction returns CORRECTIVE feedback for incorrect simplify")
    void testAnalyzeMathAction_simplify_incorrect() {
        final var event = new GraspableEventDto("simplify", "x+1", "x+2", 1L, 1L, "s1");
        event.correct = false;
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.CORRECTIVE, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns null for simplify without correctness info")
    void testAnalyzeMathAction_simplify_noCorrectness() {
        final var event = new GraspableEventDto("simplify", "x+1", "x+1", 1L, 1L, "s1");
        event.correct = null;
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNull(feedback);
    }

    @Test
    @DisplayName("analyzeMathAction returns POSITIVE feedback for expand")
    void testAnalyzeMathAction_expand() {
        final var event = new GraspableEventDto("expand", "x+1", "2x+2", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.POSITIVE, feedback.type);
        assertFalse(feedback.suggestedNextSteps.isEmpty(), "Expand should suggest a next step");
    }

    @Test
    @DisplayName("analyzeMathAction returns POSITIVE feedback for factor")
    void testAnalyzeMathAction_factor() {
        final var event = new GraspableEventDto("factor", "x^2+2x+1", "(x+1)^2", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.POSITIVE, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns SUGGESTION feedback for combine")
    void testAnalyzeMathAction_combine() {
        final var event = new GraspableEventDto("combine", "x+x", "2x", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.SUGGESTION, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns HINT feedback for AddSubInvertAction")
    void testAnalyzeMathAction_addSubInvert() {
        final var event = new GraspableEventDto("AddSubInvertAction", "x+1=3", "x=3-1", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.HINT, feedback.type);
        assertFalse(feedback.suggestedNextSteps.isEmpty());
    }

    @Test
    @DisplayName("analyzeMathAction returns HINT feedback for MulDivInvertAction")
    void testAnalyzeMathAction_mulDivInvert() {
        final var event = new GraspableEventDto("MulDivInvertAction", "2x=4", "x=2", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.HINT, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns POSITIVE for fractionCancelTermsAction")
    void testAnalyzeMathAction_fractionCancel() {
        final var event = new GraspableEventDto("fractionCancelTermsAction", "2/4", "1/2", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.POSITIVE, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns HINT for move when expression changed")
    void testAnalyzeMathAction_move_changed() {
        final var event = new GraspableEventDto("move", "x+1=3", "x=3-1", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(FeedbackType.HINT, feedback.type);
    }

    @Test
    @DisplayName("analyzeMathAction returns null for move when expression unchanged")
    void testAnalyzeMathAction_move_unchanged() {
        final var event = new GraspableEventDto("move", "x+1=3", "x+1=3", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNull(feedback);
    }

    @Test
    @DisplayName("analyzeMathAction returns null for unknown event type")
    void testAnalyzeMathAction_unknownType() {
        final var event = new GraspableEventDto("unknownAction", "x", "x", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNull(feedback);
    }

    @Test
    @DisplayName("analyzeMathAction returns null for null event type")
    void testAnalyzeMathAction_nullEventType() {
        final var event = new GraspableEventDto();
        event.eventType = null;
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNull(feedback);
    }

    @Test
    @DisplayName("analyzeMathAction sets confidence to 0.85 on non-null result")
    void testAnalyzeMathAction_confidence() {
        final var event = new GraspableEventDto("expand", "x+1", "2x+2", 1L, 1L, "s1");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals(0.85, feedback.confidence);
    }

    @Test
    @DisplayName("analyzeMathAction sets sessionId from event")
    void testAnalyzeMathAction_sessionId() {
        final var event = new GraspableEventDto("factor", "x^2", "(x)(x)", 1L, 1L, "my-session-42");
        final AiFeedbackDto feedback = this.provider.analyzeMathAction(event, null);
        assertNotNull(feedback);
        assertEquals("my-session-42", feedback.sessionId);
    }

    @Test
    @DisplayName("answerQuestion returns non-blank response for how-to-solve question")
    void testAnswerQuestion_howToSolve() {
        final String answer = this.provider.answerQuestion("How do I solve this?", "x+1=3", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    @DisplayName("answerQuestion returns non-blank response for what-next question")
    void testAnswerQuestion_whatNext() {
        final String answer = this.provider.answerQuestion("What should I do next?", "x+1=3", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    @DisplayName("answerQuestion returns non-blank response for why question")
    void testAnswerQuestion_why() {
        final String answer = this.provider.answerQuestion("Why do we do that?", "x=2", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    @DisplayName("answerQuestion returns non-blank response for stuck question")
    void testAnswerQuestion_stuck() {
        final String answer = this.provider.answerQuestion("I'm stuck, help!", "x+1=3", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    @DisplayName("answerQuestion returns non-blank response for hint request")
    void testAnswerQuestion_hint() {
        final String answer = this.provider.answerQuestion("Can I get a hint?", "x+1=3", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }

    @Test
    @DisplayName("answerQuestion returns non-blank fallback for unrecognized question")
    void testAnswerQuestion_fallback() {
        final String answer = this.provider.answerQuestion("zzz random question xyz", "x+1=3", "x+1=3", "x=2", null);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }
}
