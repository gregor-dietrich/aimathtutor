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
        assertNull(dto.lesson);
    }

    @Test
    @DisplayName("Parameterized constructor sets fields")
    void testParameterizedConstructor() {
        final var dto = new ExerciseDto("Title", "Content", "lp1", true, true);
        assertEquals("Title", dto.title);
        assertEquals("Content", dto.content);
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
}
