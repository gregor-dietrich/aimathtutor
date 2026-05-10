package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class StudentProgressSummaryDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new StudentProgressSummaryDto();
        assertNull(dto.userPublicId);
        assertNull(dto.username);
        assertNull(dto.totalSessions);
        assertNull(dto.completedSessions);
        assertNull(dto.totalProblems);
        assertNull(dto.completedProblems);
        assertNull(dto.hintsUsed);
        assertNull(dto.averageActionsPerProblem);
        assertNull(dto.successRate);
        assertNull(dto.lastActivity);
    }

    @Test
    @DisplayName("Parameterized constructor sets all fields")
    void testParameterizedConstructor() {
        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var dto = new StudentProgressSummaryDto("up1", "alice", 10, 8, 20, 15, 5, 3.2, 0.85, now);
        assertEquals("up1", dto.userPublicId);
        assertEquals("alice", dto.username);
        assertEquals(10, dto.totalSessions);
        assertEquals(8, dto.completedSessions);
        assertEquals(20, dto.totalProblems);
        assertEquals(15, dto.completedProblems);
        assertEquals(5, dto.hintsUsed);
        assertEquals(3.2, dto.averageActionsPerProblem);
        assertEquals(0.85, dto.successRate);
        assertEquals(now, dto.lastActivity);
    }

    @Test
    @DisplayName("getCompletionRatePercentage returns formatted rate")
    void testGetCompletionRatePercentage() {
        final var dto = new StudentProgressSummaryDto();
        dto.totalSessions = 10;
        dto.completedSessions = 7;
        final var result = dto.getCompletionRatePercentage();
        assertTrue("70.0%".equals(result) || "70,0%".equals(result));
    }

    @Test
    @DisplayName("getCompletionRatePercentage returns 0% when total is 0")
    void testGetCompletionRatePercentageZeroTotal() {
        final var dto = new StudentProgressSummaryDto();
        dto.totalSessions = 0;
        assertEquals("0%", dto.getCompletionRatePercentage());
    }

    @Test
    @DisplayName("getCompletionRatePercentage returns 0% when total is null")
    void testGetCompletionRatePercentageNullTotal() {
        final var dto = new StudentProgressSummaryDto();
        assertEquals("0%", dto.getCompletionRatePercentage());
    }

    @Test
    @DisplayName("getCompletionRatePercentage returns 0% when completed is null")
    void testGetCompletionRatePercentageNullCompleted() {
        final var dto = new StudentProgressSummaryDto();
        dto.totalSessions = 10;
        assertEquals("0%", dto.getCompletionRatePercentage());
    }

    @Test
    @DisplayName("getSuccessRatePercentage returns formatted rate")
    void testGetSuccessRatePercentage() {
        final var dto = new StudentProgressSummaryDto();
        dto.successRate = 0.75;
        final var result = dto.getSuccessRatePercentage();
        assertTrue("75.0%".equals(result) || "75,0%".equals(result));
    }

    @Test
    @DisplayName("getSuccessRatePercentage returns 0% when null")
    void testGetSuccessRatePercentageNull() {
        final var dto = new StudentProgressSummaryDto();
        assertEquals("0%", dto.getSuccessRatePercentage());
    }

    @Test
    @DisplayName("getFormattedAverageActions returns formatted value")
    void testGetFormattedAverageActions() {
        final var dto = new StudentProgressSummaryDto();
        dto.averageActionsPerProblem = 4.5;
        final var result = dto.getFormattedAverageActions();
        assertTrue("4.5".equals(result) || "4,5".equals(result));
    }

    @Test
    @DisplayName("getFormattedAverageActions returns 0 when null")
    void testGetFormattedAverageActionsNull() {
        final var dto = new StudentProgressSummaryDto();
        assertEquals("0", dto.getFormattedAverageActions());
    }
}
