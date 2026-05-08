package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
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
        final UserEntity teacher = userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher must exist");
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "ex_" + suffix;
        dto.content = "content " + suffix;
        dto.userPublicId = teacher.publicId;
        dto.published = true;
        dto.commentable = false;
        return exerciseService.createExercise(dto);
    }
}
