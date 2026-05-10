package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardTrendDtoTest {

    @Test
    @DisplayName("Constructor stores all fields correctly")
    void testConstructor() {
        final var dto = new DashboardTrendDto(100, 80, 50, 40, 30, 25, 10, 8, 200, 150, 15, 12);
        assertEquals(100, dto.totalSessions);
        assertEquals(80, dto.prevTotalSessions);
        assertEquals(50, dto.completedSessions);
        assertEquals(40, dto.prevCompletedSessions);
        assertEquals(30, dto.activeStudents);
        assertEquals(25, dto.prevActiveStudents);
        assertEquals(10, dto.todaySessions);
        assertEquals(8, dto.prevTodaySessions);
        assertEquals(200, dto.totalUsers);
        assertEquals(150, dto.prevTotalUsers);
        assertEquals(15, dto.publishedExercises);
        assertEquals(12, dto.prevPublishedExercises);
    }

    @Test
    @DisplayName("totalSessionsChange calculates positive percentage change")
    void testTotalSessionsChangePositive() {
        final var dto = new DashboardTrendDto(120, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(20.0, dto.totalSessionsChange(), 0.01);
    }

    @Test
    @DisplayName("totalSessionsChange calculates negative percentage change")
    void testTotalSessionsChangeNegative() {
        final var dto = new DashboardTrendDto(80, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(-20.0, dto.totalSessionsChange(), 0.01);
    }

    @Test
    @DisplayName("totalSessionsChange returns 100 when previous is zero and current positive")
    void testTotalSessionsChangeZeroPrevPositive() {
        final var dto = new DashboardTrendDto(50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(100.0, dto.totalSessionsChange(), 0.01);
    }

    @Test
    @DisplayName("totalSessionsChange returns 0 when both previous and current are zero")
    void testTotalSessionsChangeZeroPrevZeroCurrent() {
        final var dto = new DashboardTrendDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0.0, dto.totalSessionsChange(), 0.01);
    }

    @Test
    @DisplayName("completedSessionsChange calculates correctly")
    void testCompletedSessionsChange() {
        final var dto = new DashboardTrendDto(0, 0, 60, 50, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(20.0, dto.completedSessionsChange(), 0.01);
    }

    @Test
    @DisplayName("activeStudentsChange returns 100 for new metric")
    void testActiveStudentsChangeNewMetric() {
        final var dto = new DashboardTrendDto(0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(100.0, dto.activeStudentsChange(), 0.01);
    }

    @Test
    @DisplayName("todaySessionsChange handles zero previous correctly")
    void testTodaySessionsChangeZeroPrev() {
        final var dto = new DashboardTrendDto(0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0);
        assertEquals(100.0, dto.todaySessionsChange(), 0.01);
    }

    @Test
    @DisplayName("totalUsersChange calculates percentage correctly")
    void testTotalUsersChange() {
        final var dto = new DashboardTrendDto(0, 0, 0, 0, 0, 0, 0, 0, 110, 100, 0, 0);
        assertEquals(10.0, dto.totalUsersChange(), 0.01);
    }

    @Test
    @DisplayName("publishedExercisesChange calculates percentage correctly")
    void testPublishedExercisesChange() {
        final var dto = new DashboardTrendDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 10);
        assertEquals(100.0, dto.publishedExercisesChange(), 0.01);
    }

    @Test
    @DisplayName("All change methods handle same period values as zero change")
    void testNoChange() {
        final var dto = new DashboardTrendDto(50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50);
        assertEquals(0.0, dto.totalSessionsChange(), 0.01);
        assertEquals(0.0, dto.completedSessionsChange(), 0.01);
        assertEquals(0.0, dto.activeStudentsChange(), 0.01);
        assertEquals(0.0, dto.todaySessionsChange(), 0.01);
        assertEquals(0.0, dto.totalUsersChange(), 0.01);
        assertEquals(0.0, dto.publishedExercisesChange(), 0.01);
    }
}
