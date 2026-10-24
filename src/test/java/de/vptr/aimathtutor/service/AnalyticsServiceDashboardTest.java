package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class AnalyticsServiceDashboardTest {

    @Inject
    AnalyticsService analyticsService;

    @Test
    @TestTransaction
    @DisplayName("getUserCount returns a positive count")
    void testGetUserCount() {
        final long count = this.analyticsService.getUserCount();
        assertTrue(count >= 4, "Should have at least 4 seeded users");
    }

    @Test
    @TestTransaction
    @DisplayName("getDailySessionCounts returns a map with days+1 entries")
    void testGetDailySessionCounts() {
        final Map<LocalDate, Long> counts = this.analyticsService.getDailySessionCounts(7);
        assertNotNull(counts);
        assertEquals(8, counts.size(), "Should return 8 entries for 7 days (0..7 inclusive)");
        assertTrue(counts.containsKey(LocalDate.now(ZoneId.systemDefault())), "Should include today's date");
    }

    @Test
    @TestTransaction
    @DisplayName("getTrendData returns a non-null DTO with valid values")
    void testGetTrendData() {
        final var trend = this.analyticsService.getTrendData();
        assertNotNull(trend);
        assertTrue(trend.totalSessions >= 0);
        assertTrue(trend.completedSessions >= 0);
        assertTrue(trend.activeStudents >= 0);
        assertTrue(trend.todaySessions >= 0);
        assertTrue(trend.totalUsers >= 4);
    }
}
