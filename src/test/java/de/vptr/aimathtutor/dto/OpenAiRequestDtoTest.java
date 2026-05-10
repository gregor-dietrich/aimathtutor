package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class OpenAiRequestDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new OpenAiRequestDto();
        assertNull(dto.model);
        assertNull(dto.messages);
        assertNull(dto.temperature);
        assertNull(dto.maxTokens);
        assertNull(dto.topP);
        assertNull(dto.frequencyPenalty);
        assertNull(dto.presencePenalty);
        assertNull(dto.responseFormat);
    }

    @Test
    @DisplayName("Message default constructor")
    void testMessageDefault() {
        final var msg = new OpenAiRequestDto.Message();
        assertNull(msg.role);
        assertNull(msg.content);
    }

    @Test
    @DisplayName("Message parameterized constructor")
    void testMessageParameterized() {
        final var msg = new OpenAiRequestDto.Message("user", "Hello");
        assertEquals("user", msg.role);
        assertEquals("Hello", msg.content);
    }

    @Test
    @DisplayName("ResponseFormat default constructor")
    void testResponseFormatDefault() {
        final var format = new OpenAiRequestDto.ResponseFormat();
        assertNull(format.type);
    }

    @Test
    @DisplayName("ResponseFormat parameterized constructor")
    void testResponseFormatParameterized() {
        final var format = new OpenAiRequestDto.ResponseFormat("json_object");
        assertEquals("json_object", format.type);
    }

    @Test
    @DisplayName("createChatRequest creates valid request")
    void testCreateChatRequest() {
        final var request = OpenAiRequestDto.createChatRequest("You are helpful", "What is 2+2?", "gpt-4", 0.7, 100);
        assertEquals("gpt-4", request.model);
        assertEquals(0.7, request.temperature);
        assertEquals(100, request.maxTokens);
        assertNotNull(request.messages);
        assertEquals(2, request.messages.size());
        assertEquals("system", request.messages.get(0).role);
        assertEquals("You are helpful", request.messages.get(0).content);
        assertEquals("user", request.messages.get(1).role);
        assertEquals("What is 2+2?", request.messages.get(1).content);
    }

    @Test
    @DisplayName("createJsonRequest sets json_object format")
    void testCreateJsonRequest() {
        final var request = OpenAiRequestDto.createJsonRequest("Extract JSON", "{key}", "gpt-4", 0.5, 200);
        assertNotNull(request.responseFormat);
        assertEquals("json_object", request.responseFormat.type);
        assertEquals("gpt-4", request.model);
    }
}
