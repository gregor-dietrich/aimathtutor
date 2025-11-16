package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.LessonEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class LessonServiceTest {

    @Inject
    private LessonService lessonService;

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with null name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithNullName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = null;

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with empty name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithEmptyName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with whitespace name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithWhitespaceName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }
}
