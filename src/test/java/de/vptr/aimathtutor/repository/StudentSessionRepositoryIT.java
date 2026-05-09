package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link StudentSessionRepository}.
 */
@QuarkusTest
public class StudentSessionRepositoryIT extends AbstractRepositoryIT {

    @Inject
    StudentSessionRepository sessionRepository;

    @Test
    @TestTransaction
    public void testFindByUserAndDateRange() {
        final UserEntity user = this.createUser("drng", "sessuser_");
        final ExerciseEntity ex = this.createExercise(user, "DateRangeExercise", "x + 2");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-drng-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now().minusDays(1);
        session.completed = true;
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserIdAndDateRange(
                Objects.requireNonNull(user.id), LocalDateTime.now().minusDays(2), LocalDateTime.now());
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> Objects.equals(s.sessionId, session.sessionId)));
    }

    @Test
    @TestTransaction
    public void testSearchByUserOrExerciseTerm_returnsMatchingSession() {
        final UserEntity user = this.createUser("srch", "sessuser_");
        final ExerciseEntity ex = this.createExercise(user, "SearchableExercise", "x + 2");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-srch-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now();
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.searchByUserOrExerciseTerm("sessuser_srch");
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> Objects.equals(s.sessionId, session.sessionId)));
    }

    @Test
    @TestTransaction
    public void testCountActiveStudentsSince_includesRecentSession() {
        final UserEntity user = this.createUser("cnt", "sessuser_");
        final ExerciseEntity ex = this.createExercise(user, "CountExercise", "x + 2");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-cnt-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now().minusMinutes(5);
        this.sessionRepository.persist(session);

        final long count = this.sessionRepository.countActiveStudentsSince(LocalDateTime.now().minusHours(1));
        Assertions.assertTrue(count >= 1, "Should count at least the student we just created a session for");
    }

    @Test
    @TestTransaction
    public void testFindByUserId_returnsUserSessions() {
        final UserEntity user = this.createUser("byuid", "sessuser_");
        final ExerciseEntity ex = this.createExercise(user, "ByUserExercise", "x + 2");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-byuid-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now();
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserId(Objects.requireNonNull(user.id));
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> Objects.equals(s.sessionId, session.sessionId)));
    }
}
