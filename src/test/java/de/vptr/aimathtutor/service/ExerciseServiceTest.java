package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.service.ExerciseService;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null title")
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
