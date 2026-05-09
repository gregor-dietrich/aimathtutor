package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.TestExerciseFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class ExerciseCompletionServiceTest {

    @Inject
    ExerciseCompletionService exerciseCompletionService;

    @Inject
    ExerciseService exerciseService;

    @Inject
    GraspableMathService graspableMathService;

    @Inject
    UserRepository userRepository;

    @InjectMock
    AuthService authService;

    @InjectMock
    PermissionService permissionService;

    private ExerciseViewDto createExercise() {
        return TestExerciseFactory.createExercise(this.userRepository, this.exerciseService);
    }

    @Test
    @DisplayName("enrichWithCompletionData returns dto unchanged when user is not authenticated")
    @TestTransaction
    void testEnrichWithCompletionData_unauthenticated() {
        when(this.authService.getUserId()).thenReturn(null);
        final ExerciseViewDto dto = this.createExercise();

        final ExerciseViewDto result = this.exerciseCompletionService.enrichWithCompletionData(dto);

        assertNotNull(result);
        assertNull(result.userCompleted, "userCompleted should remain null when not authenticated");
    }

    @Test
    @DisplayName("enrichWithCompletionData returns dto unchanged for null input")
    void testEnrichWithCompletionData_nullInput() {
        assertNull(this.exerciseCompletionService.enrichWithCompletionData(null));
    }

    @Test
    @DisplayName("enrichWithCompletionData sets userCompleted=true after session completed")
    @TestTransaction
    void testEnrichWithCompletionData_completedSession() {
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "Seeded student1 must exist");
        when(this.authService.getUserId()).thenReturn(student.id);

        final ExerciseViewDto exercise = this.createExercise();
        final String sessionId = this.graspableMathService.createSession(Objects.requireNonNull(student.id),
                Objects.requireNonNull(exercise.id));
        this.graspableMathService.markSessionComplete(sessionId);

        final ExerciseViewDto result = this.exerciseCompletionService.enrichWithCompletionData(exercise);

        assertNotNull(result);
        assertTrue(Boolean.TRUE.equals(result.userCompleted),
                "userCompleted should be true after completing a session");
        assertTrue(result.userCompletionCount != null && result.userCompletionCount >= 1,
                "userCompletionCount should be at least 1");
    }

    @Test
    @DisplayName("enrichWithCompletionData sets userCompleted=false when no completed sessions")
    @TestTransaction
    void testEnrichWithCompletionData_noCompletedSessions() {
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "Seeded student1 must exist");
        when(this.authService.getUserId()).thenReturn(student.id);

        final ExerciseViewDto exercise = this.createExercise();
        this.graspableMathService.createSession(Objects.requireNonNull(student.id),
                Objects.requireNonNull(exercise.id));

        final ExerciseViewDto result = this.exerciseCompletionService.enrichWithCompletionData(exercise);

        assertNotNull(result);
        assertFalse(Boolean.TRUE.equals(result.userCompleted),
                "userCompleted should be false when session is not completed");
    }

    @Test
    @DisplayName("enrichListWithCompletionData returns list unchanged when user is not authenticated")
    @TestTransaction
    void testEnrichListWithCompletionData_unauthenticated() {
        when(this.authService.getUserId()).thenReturn(null);
        final var dtos = List.of(this.createExercise(), this.createExercise());

        final var result = this.exerciseCompletionService.enrichListWithCompletionData(dtos);

        assertNotNull(result);
        result.forEach(dto -> assertNull(dto.userCompleted, "userCompleted should remain null when not authenticated"));
    }

    @Test
    @DisplayName("enrichListWithCompletionData returns same list for null input")
    @SuppressWarnings("NullAway")
    void testEnrichListWithCompletionData_nullInput() {
        assertNull(this.exerciseCompletionService.enrichListWithCompletionData(null));
    }
}
