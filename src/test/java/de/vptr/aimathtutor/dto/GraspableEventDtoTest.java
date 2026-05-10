package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class GraspableEventDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new GraspableEventDto();
        assertNull(dto.eventType);
        assertNull(dto.expressionBefore);
        assertNull(dto.expressionAfter);
        assertNull(dto.actionDetails);
        assertNull(dto.studentId);
        assertNull(dto.exerciseId);
        assertNull(dto.sessionId);
        assertNull(dto.timestamp);
        assertNull(dto.correct);
        assertNull(dto.isComplete);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new GraspableEventDto("simplify", "2+2", "4", 1L, 2L, "sess");
        assertEquals("simplify", dto.eventType);
        assertEquals("2+2", dto.expressionBefore);
        assertEquals("4", dto.expressionAfter);
        assertEquals(1L, dto.studentId);
        assertEquals(2L, dto.exerciseId);
        assertEquals("sess", dto.sessionId);
    }

    @Test
    @DisplayName("toString returns summary")
    void testToString() {
        final var dto = new GraspableEventDto("factor", "x^2+2x", "x(x+2)", 1L, 2L, "s1");
        final var str = dto.toString();
        assertTrue(str.contains("GraspableEventDto"));
        assertTrue(str.contains("factor"));
        assertTrue(str.contains("x^2+2x"));
    }
}
