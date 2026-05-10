package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;

@SuppressWarnings("NullAway")
class StudentSessionViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new StudentSessionViewDto();
        assertNull(dto.publicId);
        assertNull(dto.sessionId);
        assertNull(dto.userPublicId);
        assertNull(dto.username);
        assertNull(dto.exercisePublicId);
        assertNull(dto.exerciseTitle);
        assertNull(dto.startTime);
        assertNull(dto.endTime);
        assertNull(dto.completed);
        assertNull(dto.actionsCount);
        assertNull(dto.correctActions);
        assertNull(dto.hintsUsed);
        assertNull(dto.finalExpression);
        assertNull(dto.durationSeconds);
        assertNull(dto.successRate);
    }

    @Test
    @DisplayName("Entity constructor maps fields")
    void testEntityConstructor() {
        final var entity = new StudentSessionEntity();
        entity.publicId = "sp";
        entity.sessionId = "sess1";
        entity.startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        entity.endTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        entity.completed = true;
        entity.actionsCount = 10;
        entity.correctActions = 8;
        entity.hintsUsed = 2;
        entity.finalExpression = "x=2";

        final var user = new UserEntity();
        user.publicId = "up1";
        user.username = "bob";
        entity.user = user;

        final var exercise = new ExerciseEntity();
        exercise.publicId = "ep1";
        exercise.title = "Algebra";
        entity.exercise = exercise;

        final var dto = new StudentSessionViewDto(entity);
        assertEquals("sp", dto.publicId);
        assertEquals("sess1", dto.sessionId);
        assertEquals("up1", dto.userPublicId);
        assertEquals("bob", dto.username);
        assertEquals("ep1", dto.exercisePublicId);
        assertEquals("Algebra", dto.exerciseTitle);
        assertEquals(true, dto.completed);
        assertEquals(10, dto.actionsCount);
        assertEquals(8, dto.correctActions);
        assertEquals(2, dto.hintsUsed);
        assertEquals("x=2", dto.finalExpression);
        assertEquals(1800L, dto.durationSeconds);
        assertEquals(0.8, dto.successRate);
    }

    @Test
    @DisplayName("Entity constructor handles null entity")
    void testEntityConstructorNull() {
        final var dto = new StudentSessionViewDto((StudentSessionEntity) null);
        assertNull(dto.publicId);
    }

    @Test
    @DisplayName("Entity constructor handles null user")
    void testEntityConstructorNullUser() {
        final var entity = new StudentSessionEntity();
        entity.publicId = "sp";
        entity.user = null;
        entity.exercise = null;
        entity.actionsCount = 0;

        final var dto = new StudentSessionViewDto(entity);
        assertEquals("sp", dto.publicId);
        assertNull(dto.userPublicId);
    }

    @Test
    @DisplayName("Entity constructor computes null duration for incomplete session")
    void testEntityConstructorIncompleteDuration() {
        final var entity = new StudentSessionEntity();
        entity.startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        entity.endTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        entity.completed = false;
        entity.actionsCount = 0;

        final var dto = new StudentSessionViewDto(entity);
        assertNull(dto.durationSeconds);
    }

    @Test
    @DisplayName("getFormattedDuration returns null for incomplete")
    void testGetFormattedDurationIncomplete() {
        final var dto = new StudentSessionViewDto();
        dto.completed = false;
        dto.durationSeconds = 60L;
        assertNull(dto.getFormattedDuration());
    }

    @Test
    @DisplayName("getFormattedDuration returns seconds format")
    void testGetFormattedDurationSeconds() {
        final var dto = new StudentSessionViewDto();
        dto.completed = true;
        dto.durationSeconds = 45L;
        assertEquals("45s", dto.getFormattedDuration());
    }

    @Test
    @DisplayName("getFormattedDuration returns minutes format")
    void testGetFormattedDurationMinutes() {
        final var dto = new StudentSessionViewDto();
        dto.completed = true;
        dto.durationSeconds = 125L;
        assertEquals("02:05", dto.getFormattedDuration());
    }

    @Test
    @DisplayName("getFormattedDuration returns hours format")
    void testGetFormattedDurationHours() {
        final var dto = new StudentSessionViewDto();
        dto.completed = true;
        dto.durationSeconds = 3661L;
        assertEquals("01:01:01", dto.getFormattedDuration());
    }

    @Test
    @DisplayName("getFormattedDuration returns 0s for zero")
    void testGetFormattedDurationZero() {
        final var dto = new StudentSessionViewDto();
        dto.completed = true;
        dto.durationSeconds = 0L;
        assertEquals("0s", dto.getFormattedDuration());
    }

    @Test
    @DisplayName("getSuccessRatePercentage returns formatted rate")
    void testGetSuccessRatePercentage() {
        final var dto = new StudentSessionViewDto();
        dto.successRate = 0.85;
        final var result = dto.getSuccessRatePercentage();
        assertTrue("85.0%".equals(result) || "85,0%".equals(result));
    }

    @Test
    @DisplayName("getSuccessRatePercentage returns 0% when null")
    void testGetSuccessRatePercentageNull() {
        final var dto = new StudentSessionViewDto();
        assertEquals("0%", dto.getSuccessRatePercentage());
    }
}
