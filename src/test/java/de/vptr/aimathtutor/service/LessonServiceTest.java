package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.entity.LessonEntity;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @InjectMocks
    private LessonService lessonService;

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with null name")
    void shouldThrowValidationExceptionWhenCreatingLessonWithNullName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = null;

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with empty name")
    void shouldThrowValidationExceptionWhenCreatingLessonWithEmptyName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with whitespace name")
    void shouldThrowValidationExceptionWhenCreatingLessonWithWhitespaceName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }
}
