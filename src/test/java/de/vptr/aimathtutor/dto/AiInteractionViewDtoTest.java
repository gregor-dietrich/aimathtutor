package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;

@SuppressWarnings("NullAway")
class AiInteractionViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new AiInteractionViewDto();
        assertNull(dto.publicId);
        assertNull(dto.sessionId);
        assertNull(dto.userPublicId);
        assertNull(dto.username);
        assertNull(dto.exercisePublicId);
        assertNull(dto.exerciseTitle);
        assertNull(dto.eventType);
        assertNull(dto.studentMessage);
        assertNull(dto.expressionBefore);
        assertNull(dto.expressionAfter);
        assertNull(dto.feedbackType);
        assertNull(dto.feedbackMessage);
        assertNull(dto.confidenceScore);
        assertNull(dto.actionCorrect);
        assertNull(dto.created);
    }

    @Test
    @DisplayName("Entity constructor maps fields")
    void testEntityConstructor() {
        final var entity = new AiInteractionEntity();
        entity.publicId = "pid";
        entity.sessionId = "sess";
        entity.eventType = "simplify";
        entity.studentMessage = "student msg";
        entity.expressionBefore = "2+2";
        entity.expressionAfter = "4";
        entity.feedbackType = "POSITIVE";
        entity.feedbackMessage = "Good!";
        entity.confidenceScore = 0.95;
        entity.actionCorrect = true;
        entity.created = LocalDateTime.now(ZoneId.systemDefault());

        final var user = new UserEntity();
        user.publicId = "up";
        user.username = "alice";
        entity.user = user;

        final var exercise = new ExerciseEntity();
        exercise.publicId = "ep";
        exercise.title = "Basic Math";
        entity.exercise = exercise;

        final var dto = new AiInteractionViewDto(entity);
        assertEquals("pid", dto.publicId);
        assertEquals("sess", dto.sessionId);
        assertEquals("up", dto.userPublicId);
        assertEquals("alice", dto.username);
        assertEquals("ep", dto.exercisePublicId);
        assertEquals("Basic Math", dto.exerciseTitle);
        assertEquals("simplify", dto.eventType);
        assertEquals("student msg", dto.studentMessage);
        assertEquals("2+2", dto.expressionBefore);
        assertEquals("4", dto.expressionAfter);
        assertEquals("POSITIVE", dto.feedbackType);
        assertEquals("Good!", dto.feedbackMessage);
        assertEquals(0.95, dto.confidenceScore);
        assertEquals(true, dto.actionCorrect);
        assertNotNull(dto.created);
    }

    @Test
    @DisplayName("Entity constructor handles null entity")
    void testEntityConstructorNullEntity() {
        final var dto = new AiInteractionViewDto((AiInteractionEntity) null);
        assertNull(dto.publicId);
    }

    @Test
    @DisplayName("Entity constructor handles null user")
    void testEntityConstructorNullUser() {
        final var entity = new AiInteractionEntity();
        entity.publicId = "pid";
        entity.user = null;
        entity.exercise = null;

        final var dto = new AiInteractionViewDto(entity);
        assertEquals("pid", dto.publicId);
        assertNull(dto.userPublicId);
        assertNull(dto.username);
    }

    @Test
    @DisplayName("Entity constructor handles null exercise")
    void testEntityConstructorNullExercise() {
        final var entity = new AiInteractionEntity();
        entity.publicId = "pid";
        entity.exercise = null;

        final var dto = new AiInteractionViewDto(entity);
        assertEquals("pid", dto.publicId);
        assertNull(dto.exercisePublicId);
    }
}
