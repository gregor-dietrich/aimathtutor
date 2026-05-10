package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class ConversationContextDtoTest {

    @Test
    @DisplayName("Default constructor creates empty lists")
    void testDefaultConstructor() {
        final var ctx = new ConversationContextDto();
        assertNotNull(ctx.getRecentActions());
        assertTrue(ctx.getRecentActions().isEmpty());
        assertNotNull(ctx.getRecentQuestions());
        assertTrue(ctx.getRecentQuestions().isEmpty());
        assertNotNull(ctx.getRecentAiMessages());
        assertTrue(ctx.getRecentAiMessages().isEmpty());
    }

    @Test
    @DisplayName("Parameterized constructor copies lists and truncates to max 5")
    void testParameterizedConstructorTruncates() {
        final var actions = new ArrayList<GraspableEventDto>();
        for (int i = 0; i < 8; i++) {
            actions.add(new GraspableEventDto("type" + i, "before" + i, "after" + i, 1L, 1L, "session"));
        }
        final var questions = List.of(ChatMessageDto.userQuestion("q1"), ChatMessageDto.userQuestion("q2"));
        final var messages = List.of(ChatMessageDto.aiFeedback("m1"));

        final var ctx = new ConversationContextDto(actions, questions, messages);
        assertEquals(5, ctx.getRecentActions().size());
        assertEquals(2, ctx.getRecentQuestions().size());
        assertEquals(1, ctx.getRecentAiMessages().size());
    }

    @Test
    @DisplayName("Parameterized constructor handles null lists")
    void testParameterizedConstructorNulls() {
        final var ctx = new ConversationContextDto(null, null, null);
        assertTrue(ctx.getRecentActions().isEmpty());
        assertTrue(ctx.getRecentQuestions().isEmpty());
        assertTrue(ctx.getRecentAiMessages().isEmpty());
    }

    @Test
    @DisplayName("addAction adds action and limits to 5")
    void testAddAction() {
        final var ctx = new ConversationContextDto();
        for (int i = 0; i < 7; i++) {
            ctx.addAction(new GraspableEventDto("type" + i, "before" + i, "after" + i, 1L, 1L, "session"));
        }
        assertEquals(5, ctx.getRecentActions().size());
    }

    @Test
    @DisplayName("addAction ignores null")
    void testAddActionNull() {
        final var ctx = new ConversationContextDto();
        ctx.addAction(null);
        assertTrue(ctx.getRecentActions().isEmpty());
    }

    @Test
    @DisplayName("addQuestion adds only valid user questions")
    void testAddQuestion() {
        final var ctx = new ConversationContextDto();
        ctx.addQuestion(ChatMessageDto.userQuestion("q1"));
        assertEquals(1, ctx.getRecentQuestions().size());
    }

    @Test
    @DisplayName("addQuestion ignores null")
    void testAddQuestionNull() {
        final var ctx = new ConversationContextDto();
        ctx.addQuestion(null);
        assertTrue(ctx.getRecentQuestions().isEmpty());
    }

    @Test
    @DisplayName("addQuestion ignores AI messages")
    void testAddQuestionIgnoresAi() {
        final var ctx = new ConversationContextDto();
        ctx.addQuestion(ChatMessageDto.aiFeedback("not a question"));
        assertTrue(ctx.getRecentQuestions().isEmpty());
    }

    @Test
    @DisplayName("addQuestion limits to 5 entries")
    void testAddQuestionLimit() {
        final var ctx = new ConversationContextDto();
        for (int i = 0; i < 7; i++) {
            ctx.addQuestion(ChatMessageDto.userQuestion("q" + i));
        }
        assertEquals(5, ctx.getRecentQuestions().size());
    }

    @Test
    @DisplayName("addAiMessage adds valid AI feedback and answers")
    void testAddAiMessage() {
        final var ctx = new ConversationContextDto();
        ctx.addAiMessage(ChatMessageDto.aiFeedback("feedback"));
        ctx.addAiMessage(ChatMessageDto.aiAnswer("answer"));
        assertEquals(2, ctx.getRecentAiMessages().size());
    }

    @Test
    @DisplayName("addAiMessage ignores null and user messages")
    void testAddAiMessageInvalid() {
        final var ctx = new ConversationContextDto();
        ctx.addAiMessage(null);
        ctx.addAiMessage(ChatMessageDto.userQuestion("not ai"));
        assertTrue(ctx.getRecentAiMessages().isEmpty());
    }

    @Test
    @DisplayName("addAiMessage limits to 5 entries")
    void testAddAiMessageLimit() {
        final var ctx = new ConversationContextDto();
        for (int i = 0; i < 7; i++) {
            ctx.addAiMessage(ChatMessageDto.aiFeedback("msg" + i));
        }
        assertEquals(5, ctx.getRecentAiMessages().size());
    }

    @Test
    @DisplayName("Getters return unmodifiable actions list")
    void testUnmodifiableActionsList() {
        final var ctx = new ConversationContextDto();
        ctx.addAction(new GraspableEventDto("type", "b", "a", 1L, 1L, "s"));
        final var actions = ctx.getRecentActions();
        assertThrows(UnsupportedOperationException.class, () -> actions.add(null));
    }

    @Test
    @DisplayName("Getters return unmodifiable questions list")
    void testUnmodifiableQuestionsList() {
        final var ctx = new ConversationContextDto();
        ctx.addQuestion(ChatMessageDto.userQuestion("q"));
        final var questions = ctx.getRecentQuestions();
        assertThrows(UnsupportedOperationException.class, () -> questions.add(null));
    }

    @Test
    @DisplayName("Getters return unmodifiable AI messages list")
    void testUnmodifiableAiMessagesList() {
        final var ctx = new ConversationContextDto();
        ctx.addAiMessage(ChatMessageDto.aiFeedback("m"));
        final var messages = ctx.getRecentAiMessages();
        assertThrows(UnsupportedOperationException.class, () -> messages.add(null));
    }

    @Test
    @DisplayName("toString returns compact summary")
    void testToString() {
        final var ctx = new ConversationContextDto();
        ctx.addAction(new GraspableEventDto("type", "b", "a", 1L, 1L, "s"));
        final var str = ctx.toString();
        assertTrue(str.contains("recentActions=1"));
        assertTrue(str.contains("ConversationContextDto"));
    }
}
