package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;

@SuppressWarnings("NullAway")
class ExerciseDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new ExerciseDto();
        assertNull(dto.publicId);
        assertNull(dto.title);
        assertNull(dto.content);
        assertNull(dto.userPublicId);
        assertNull(dto.lessonPublicId);
        assertNull(dto.published);
        assertNull(dto.commentable);
        assertNull(dto.created);
        assertNull(dto.lastEdit);
        assertNull(dto.graspableEnabled);
        assertNull(dto.graspableInitialExpression);
        assertNull(dto.graspableTargetExpression);
        assertNull(dto.graspableDifficulty);
        assertNull(dto.graspableHints);
        assertNull(dto.user);
        assertNull(dto.lesson);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new ExerciseDto("Title", "Content", "up1", "lp1", true, true);
        assertEquals("Title", dto.title);
        assertEquals("Content", dto.content);
        assertEquals("up1", dto.userPublicId);
        assertEquals("lp1", dto.lessonPublicId);
        assertEquals(true, dto.published);
        assertEquals(true, dto.commentable);
    }

    @Test
    @DisplayName("DifficultyLevel enum values")
    void testDifficultyLevelEnum() {
        assertEquals(4, DifficultyLevel.values().length);
        assertEquals("beginner", DifficultyLevel.BEGINNER.getValue());
        assertEquals("intermediate", DifficultyLevel.INTERMEDIATE.getValue());
        assertEquals("advanced", DifficultyLevel.ADVANCED.getValue());
        assertEquals("expert", DifficultyLevel.EXPERT.getValue());
    }

    @Test
    @DisplayName("DifficultyLevel toString returns value")
    void testDifficultyLevelToString() {
        assertEquals("beginner", DifficultyLevel.BEGINNER.toString());
        assertEquals("expert", DifficultyLevel.EXPERT.toString());
    }

    @Test
    @DisplayName("UserField default constructor")
    void testUserFieldDefault() {
        final var field = new ExerciseDto.UserField();
        assertNull(field.publicId);
        assertNull(field.username);
    }

    @Test
    @DisplayName("UserField parameterized constructor")
    void testUserFieldParameterized() {
        final var field = new ExerciseDto.UserField("up123");
        assertEquals("up123", field.publicId);
    }

    @Test
    @DisplayName("UserField setters")
    void testUserFieldSetters() {
        final var field = new ExerciseDto.UserField();
        field.setPublicId("up1");
        field.setUsername("alice");
        assertEquals("up1", field.publicId);
        assertEquals("alice", field.username);
    }

    @Test
    @DisplayName("LessonField default constructor")
    void testLessonFieldDefault() {
        final var field = new ExerciseDto.LessonField();
        assertNull(field.publicId);
        assertNull(field.name);
    }

    @Test
    @DisplayName("LessonField parameterized constructor")
    void testLessonFieldParameterized() {
        final var field = new ExerciseDto.LessonField("lp123");
        assertEquals("lp123", field.publicId);
    }

    @Test
    @DisplayName("LessonField setters")
    void testLessonFieldSetters() {
        final var field = new ExerciseDto.LessonField();
        field.setPublicId("lp1");
        field.setName("Algebra");
        assertEquals("lp1", field.publicId);
        assertEquals("Algebra", field.name);
    }

    @Test
    @DisplayName("syncNestedFields copies user from nested to flat")
    void testSyncNestedFieldsUserFromNested() {
        final var dto = new ExerciseDto();
        dto.user = new ExerciseDto.UserField("up1");
        dto.syncNestedFields();
        assertEquals("up1", dto.userPublicId);
    }

    @Test
    @DisplayName("syncNestedFields copies user from flat to nested")
    void testSyncNestedFieldsUserFromFlat() {
        final var dto = new ExerciseDto();
        dto.userPublicId = "up2";
        dto.syncNestedFields();
        assertNotNull(dto.user);
        assertEquals("up2", dto.user.publicId);
    }

    @Test
    @DisplayName("syncNestedFields copies lesson from nested to flat")
    void testSyncNestedFieldsLessonFromNested() {
        final var dto = new ExerciseDto();
        dto.lesson = new ExerciseDto.LessonField("lp1");
        dto.syncNestedFields();
        assertEquals("lp1", dto.lessonPublicId);
    }

    @Test
    @DisplayName("syncNestedFields copies lesson from flat to nested")
    void testSyncNestedFieldsLessonFromFlat() {
        final var dto = new ExerciseDto();
        dto.lessonPublicId = "lp2";
        dto.syncNestedFields();
        assertNotNull(dto.lesson);
        assertEquals("lp2", dto.lesson.publicId);
    }

    @Test
    @DisplayName("syncNestedFields nested takes precedence over flat with warning")
    void testSyncNestedFieldsConflict() {
        final var dto = new ExerciseDto();
        dto.userPublicId = "up-old";
        dto.user = new ExerciseDto.UserField("up-new");
        dto.syncNestedFields();
        assertEquals("up-new", dto.userPublicId);
    }
}
