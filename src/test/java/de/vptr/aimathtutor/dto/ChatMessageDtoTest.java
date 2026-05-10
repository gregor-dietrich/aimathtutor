package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ChatMessageDto.MessageType;
import de.vptr.aimathtutor.dto.ChatMessageDto.Sender;

@SuppressWarnings("NullAway")
class ChatMessageDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new ChatMessageDto();
        assertNull(dto.sender);
        assertNull(dto.messageType);
        assertNull(dto.message);
        assertNull(dto.sessionId);
        assertNull(dto.timestamp);
        assertNull(dto.relatedAction);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new ChatMessageDto(Sender.USER, MessageType.QUESTION, "What is 2+2?");
        assertEquals(Sender.USER, dto.sender);
        assertEquals(MessageType.QUESTION, dto.messageType);
        assertEquals("What is 2+2?", dto.message);
    }

    @Test
    @DisplayName("Parameterized constructor handles null message")
    void testParameterizedConstructorNullMessage() {
        final var dto = new ChatMessageDto(Sender.AI, MessageType.FEEDBACK, null);
        assertEquals(Sender.AI, dto.sender);
        assertEquals(MessageType.FEEDBACK, dto.messageType);
        assertEquals("", dto.message);
    }

    @Test
    @DisplayName("userQuestion factory method")
    void testUserQuestion() {
        final var dto = ChatMessageDto.userQuestion("Help me");
        assertEquals(Sender.USER, dto.sender);
        assertEquals(MessageType.QUESTION, dto.messageType);
        assertEquals("Help me", dto.message);
    }

    @Test
    @DisplayName("aiFeedback factory method")
    void testAiFeedback() {
        final var dto = ChatMessageDto.aiFeedback("Good work!");
        assertEquals(Sender.AI, dto.sender);
        assertEquals(MessageType.FEEDBACK, dto.messageType);
    }

    @Test
    @DisplayName("aiAnswer factory method")
    void testAiAnswer() {
        final var dto = ChatMessageDto.aiAnswer("The answer is 4");
        assertEquals(Sender.AI, dto.sender);
        assertEquals(MessageType.ANSWER, dto.messageType);
    }

    @Test
    @DisplayName("system factory method")
    void testSystem() {
        final var dto = ChatMessageDto.system("Problem loaded");
        assertEquals(Sender.AI, dto.sender);
        assertEquals(MessageType.SYSTEM, dto.messageType);
    }

    @Test
    @DisplayName("Sender enum values")
    void testSenderEnum() {
        assertEquals(2, Sender.values().length);
        assertEquals("USER", Sender.USER.name());
        assertEquals("AI", Sender.AI.name());
    }

    @Test
    @DisplayName("MessageType enum values")
    void testMessageTypeEnum() {
        assertEquals(4, MessageType.values().length);
        assertEquals("QUESTION", MessageType.QUESTION.name());
        assertEquals("FEEDBACK", MessageType.FEEDBACK.name());
        assertEquals("ANSWER", MessageType.ANSWER.name());
        assertEquals("SYSTEM", MessageType.SYSTEM.name());
    }

    @Test
    @DisplayName("toString returns summary")
    void testToString() {
        final var dto = ChatMessageDto.userQuestion("Test");
        final var str = dto.toString();
        assertTrue(str.contains("ChatMessageDto"));
        assertTrue(str.contains("USER"));
        assertTrue(str.contains("Test"));
    }
}
