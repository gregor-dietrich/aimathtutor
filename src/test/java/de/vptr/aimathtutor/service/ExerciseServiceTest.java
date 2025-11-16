package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class ExerciseServiceTest {

    @Inject
    private ExerciseService exerciseService;

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null title")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullTitle() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = null;
        exerciseDto.content = "Content";
        exerciseDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with empty title")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithEmptyTitle() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "";
        exerciseDto.content = "Content";
        exerciseDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullContent() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "Title";
        exerciseDto.content = null;
        exerciseDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with empty content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithEmptyContent() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "Title";
        exerciseDto.content = "";
        exerciseDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }
}
