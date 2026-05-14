package de.vptr.aimathtutor.util;

import java.util.UUID;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.ExerciseService;

/**
 * Factory for creating test exercises in integration tests.
 */
public final class TestExerciseFactory {

    private TestExerciseFactory() {
    }

    /**
     * Creates a test exercise owned by the seeded teacher user.
     *
     * @param userRepository
     *            the user repository
     * @param exerciseService
     *            the exercise service
     * @return the created exercise view DTO
     */
    public static ExerciseViewDto createExercise(final UserRepository userRepository,
            final ExerciseService exerciseService) {
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        final var dto = new ExerciseDto("ex_" + suffix, "content " + suffix, null, true, false);
        return exerciseService.createExercise(dto);
    }
}
