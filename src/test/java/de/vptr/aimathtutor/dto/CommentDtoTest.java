package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;

@SuppressWarnings("NullAway")
class CommentDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new CommentDto();
        assertNull(dto.publicId);
        assertNull(dto.content);
        assertNull(dto.exercisePublicId);
        assertNull(dto.exerciseId);
        assertNull(dto.parentCommentPublicId);
        assertNull(dto.parentCommentId);
        assertNull(dto.lessonPublicId);
        assertNull(dto.lessonId);
        assertNull(dto.sessionId);
        assertNull(dto.exercise);
    }

    @Test
    @DisplayName("CommentStatus enum values")
    void testCommentStatusEnum() {
        assertEquals(3, CommentStatus.values().length);
        assertEquals("VISIBLE", CommentStatus.VISIBLE.name());
        assertEquals("HIDDEN", CommentStatus.HIDDEN.name());
        assertEquals("DELETED", CommentStatus.DELETED.name());
    }

    @Test
    @DisplayName("CommentStatus getValue returns value")
    void testCommentStatusGetValue() {
        assertEquals("VISIBLE", CommentStatus.VISIBLE.getValue());
        assertEquals("HIDDEN", CommentStatus.HIDDEN.getValue());
        assertEquals("DELETED", CommentStatus.DELETED.getValue());
    }

    @Test
    @DisplayName("CommentStatus toString returns value")
    void testCommentStatusToString() {
        assertEquals("VISIBLE", CommentStatus.VISIBLE.toString());
        assertEquals("DELETED", CommentStatus.DELETED.toString());
    }

    @Test
    @DisplayName("CommentStatus fromString returns matching status")
    void testCommentStatusFromString() {
        assertEquals(CommentStatus.VISIBLE, CommentStatus.fromString("VISIBLE"));
        assertEquals(CommentStatus.HIDDEN, CommentStatus.fromString("HIDDEN"));
        assertEquals(CommentStatus.DELETED, CommentStatus.fromString("DELETED"));
    }

    @Test
    @DisplayName("CommentStatus fromString is case-insensitive")
    void testCommentStatusFromStringCaseInsensitive() {
        assertEquals(CommentStatus.VISIBLE, CommentStatus.fromString("visible"));
        assertEquals(CommentStatus.HIDDEN, CommentStatus.fromString("Hidden"));
    }

    @Test
    @DisplayName("CommentStatus fromString returns null for unknown")
    void testCommentStatusFromStringUnknown() {
        assertNull(CommentStatus.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("CommentStatus fromString returns null for null input")
    void testCommentStatusFromStringNull() {
        assertNull(CommentStatus.fromString(null));
    }

    @Test
    @DisplayName("ExerciseField default constructor")
    void testExerciseFieldDefault() {
        final var field = new CommentDto.ExerciseField();
        assertNull(field.publicId);
    }

    @Test
    @DisplayName("ExerciseField parameterized constructor")
    void testExerciseFieldParameterized() {
        final var field = new CommentDto.ExerciseField("ep123");
        assertEquals("ep123", field.publicId);
    }

    @Test
    @DisplayName("syncExercise copies from nested to flat")
    void testSyncExerciseFromNested() {
        final var dto = new CommentDto();
        dto.exercise = new CommentDto.ExerciseField("ep123");
        dto.syncExercise();
        assertEquals("ep123", dto.exercisePublicId);
    }

    @Test
    @DisplayName("syncExercise copies from flat to nested")
    void testSyncExerciseFromFlat() {
        final var dto = new CommentDto();
        dto.exercisePublicId = "ep456";
        dto.syncExercise();
        assertNotNull(dto.exercise);
        assertEquals("ep456", dto.exercise.publicId);
    }

    @Test
    @DisplayName("syncExercise does nothing when both null")
    void testSyncExerciseBothNull() {
        final var dto = new CommentDto();
        dto.syncExercise();
        assertNull(dto.exercisePublicId);
        assertNull(dto.exercise);
    }
}
