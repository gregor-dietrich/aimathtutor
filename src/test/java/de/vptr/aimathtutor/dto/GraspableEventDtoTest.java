package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraspableEventDtoTest {

    private GraspableEventDto eventDto;

    @BeforeEach
    void setUp() {
        this.eventDto = new GraspableEventDto();
    }

    @Test
    @DisplayName("Should create GraspableEventDto with default constructor")
    void shouldCreateGraspableEventDtoWithDefaultConstructor() {
        // Then
        assertNotNull(this.eventDto.timestamp);
        assertNull(this.eventDto.eventType);
        assertNull(this.eventDto.expressionBefore);
        assertNull(this.eventDto.expressionAfter);
        assertNull(this.eventDto.studentId);
        assertNull(this.eventDto.exerciseId);
        assertNull(this.eventDto.sessionId);
        assertNull(this.eventDto.correct);
    }

    @Test
    @DisplayName("Should create GraspableEventDto with parameterized constructor")
    void shouldCreateGraspableEventDtoWithParameterizedConstructor() {
        // When
        final GraspableEventDto dto = new GraspableEventDto(
                "simplify",
                "2x + 3x",
                "5x",
                1L,
                2L,
                "session-123");

        // Then
        assertEquals("simplify", dto.eventType);
        assertEquals("2x + 3x", dto.expressionBefore);
        assertEquals("5x", dto.expressionAfter);
        assertEquals(1L, dto.studentId);
        assertEquals(2L, dto.exerciseId);
        assertEquals("session-123", dto.sessionId);
        assertNotNull(dto.timestamp);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.eventDto.eventType = "expand";
        this.eventDto.expressionBefore = "(x + 2)(x + 3)";
        this.eventDto.expressionAfter = "x^2 + 5x + 6";
        this.eventDto.studentId = 5L;
        this.eventDto.exerciseId = 10L;
        this.eventDto.sessionId = "session-456";
        this.eventDto.actionDetails = "{\"step\": 1}";
        this.eventDto.correct = true;

        // Then
        assertEquals("expand", this.eventDto.eventType);
        assertEquals("(x + 2)(x + 3)", this.eventDto.expressionBefore);
        assertEquals("x^2 + 5x + 6", this.eventDto.expressionAfter);
        assertEquals(5L, this.eventDto.studentId);
        assertEquals(10L, this.eventDto.exerciseId);
        assertEquals("session-456", this.eventDto.sessionId);
        assertEquals("{\"step\": 1}", this.eventDto.actionDetails);
        assertTrue(this.eventDto.correct);
    }

    @Test
    @DisplayName("Should generate toString with all fields")
    void shouldGenerateToStringWithAllFields() {
        // Given
        this.eventDto.eventType = "simplify";
        this.eventDto.expressionBefore = "2x + 3";
        this.eventDto.expressionAfter = "2x + 3";
        this.eventDto.studentId = 1L;
        this.eventDto.exerciseId = 2L;
        this.eventDto.sessionId = "session-789";
        this.eventDto.correct = false;

        // When
        final String result = this.eventDto.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("simplify"));
        assertTrue(result.contains("2x + 3"));
        assertTrue(result.contains("session-789"));
        assertTrue(result.contains("correct=false"));
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // When
        this.eventDto.eventType = null;
        this.eventDto.expressionBefore = null;
        this.eventDto.expressionAfter = null;

        // Then
        assertNull(this.eventDto.eventType);
        assertNull(this.eventDto.expressionBefore);
        assertNull(this.eventDto.expressionAfter);
        assertDoesNotThrow(() -> this.eventDto.toString());
    }
}
