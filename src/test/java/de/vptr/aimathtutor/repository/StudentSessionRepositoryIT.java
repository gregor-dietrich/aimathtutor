package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link StudentSessionRepository}.
 */
@QuarkusTest
public class StudentSessionRepositoryIT {

    @Inject
    StudentSessionRepository sessionRepository;
    @Inject
    ExerciseRepository exerciseRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserRankRepository userRankRepository;

    private UserEntity createUser(final String suffix) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "SessRank_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = "sessuser_" + suffix;
        user.password = "pw";
        user.email = "sessuser_" + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    private ExerciseEntity createExercise(final UserEntity user, final String title) {
        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = title;
        ex.content = "x + 2";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);
        return ex;
    }

    @Test
    @TestTransaction
    public void testFindByUserAndDateRange() {
        final UserEntity user = this.createUser("drng");
        final ExerciseEntity ex = this.createExercise(user, "DateRangeExercise");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-drng-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now().minusDays(1);
        session.completed = true;
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserIdAndDateRange(user.id,
                LocalDateTime.now().minusDays(2), LocalDateTime.now());
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> s.sessionId.equals(session.sessionId)));
    }

    @Test
    @TestTransaction
    public void testSearchByUserOrExerciseTerm_returnsMatchingSession() {
        final UserEntity user = this.createUser("srch");
        final ExerciseEntity ex = this.createExercise(user, "SearchableExercise");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-srch-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now();
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.searchByUserOrExerciseTerm("sessuser_srch");
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> s.sessionId.equals(session.sessionId)));
    }

    @Test
    @TestTransaction
    public void testCountActiveStudentsSince_includesRecentSession() {
        final UserEntity user = this.createUser("cnt");
        final ExerciseEntity ex = this.createExercise(user, "CountExercise");

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
        final UserEntity user = this.createUser("byuid");
        final ExerciseEntity ex = this.createExercise(user, "ByUserExercise");

        final StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-byuid-" + UUID.randomUUID();
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now();
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserId(user.id);
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> s.sessionId.equals(session.sessionId)));
    }
}
