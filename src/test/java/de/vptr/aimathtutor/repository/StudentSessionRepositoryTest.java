package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class StudentSessionRepositoryTest {

    @Inject
    StudentSessionRepository studentSessionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Test
    @DisplayName("findBySessionIdWithRelations should load relations")
    @TestTransaction
    void testFindBySessionIdWithRelations() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final String sid = UlidUtil.generate();

        final var session = new StudentSessionEntity();
        session.sessionId = sid;
        session.user = user;
        session.exercise = exercise;
        session.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(session);
        this.studentSessionRepository.flush();

        final var found = this.studentSessionRepository.findBySessionIdWithRelations(sid);
        assertNotNull(found);
        assertNotNull(found.user);
        assertNotNull(found.exercise);
        assertEquals("student1", found.user.username);
    }

    @Test
    @DisplayName("findByUserIdAndDateRange should filter correctly")
    @TestTransaction
    void testFindByUserIdAndDateRange() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var now = LocalDateTime.now(ZoneId.systemDefault());

        final var session = new StudentSessionEntity();
        session.sessionId = UlidUtil.generate();
        session.user = user;
        session.exercise = exercise;
        session.startTime = now;
        session.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(session);
        this.studentSessionRepository.flush();

        final var found = this.studentSessionRepository.findByUserIdAndDateRange(user.id, now.minusMinutes(1),
                now.plusMinutes(1));
        assertFalse(found.isEmpty());

        final var notFound =
                this.studentSessionRepository.findByUserIdAndDateRange(user.id, now.plusMinutes(2), now.plusMinutes(3));
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("countActiveStudentsSince should count unique users")
    @TestTransaction
    void testCountActiveStudentsSince() {
        final var user1 = this.userRepository.findByUsername("student1");
        final var user2 = this.userRepository.findByUsername("student2");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var now = LocalDateTime.now(ZoneId.systemDefault());

        final var s1 = new StudentSessionEntity();
        s1.sessionId = UlidUtil.generate();
        s1.user = user1;
        s1.exercise = exercise;
        s1.startTime = now;
        s1.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(s1);

        final var s2 = new StudentSessionEntity();
        s2.sessionId = UlidUtil.generate();
        s2.user = user1; // same user
        s2.exercise = exercise;
        s2.startTime = now;
        s2.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(s2);

        final var s3 = new StudentSessionEntity();
        s3.sessionId = UlidUtil.generate();
        s3.user = user2;
        s3.exercise = exercise;
        s3.startTime = now;
        s3.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(s3);

        this.studentSessionRepository.flush();

        final long count = this.studentSessionRepository.countActiveStudentsSince(now.minusSeconds(1));
        assertEquals(2, count);
    }

    @Test
    @DisplayName("searchByUserOrExerciseTerm should find matching sessions")
    @TestTransaction
    void testSearchByUserOrExerciseTerm() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        createAndPersistSession(user, exercise);

        final var results = this.studentSessionRepository.searchByUserOrExerciseTerm("%student1%");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(s -> s.user != null && "student1".equals(s.user.username)));
    }

    @Test
    @DisplayName("findById should return session with relations")
    @TestTransaction
    void testFindById() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        final var session = createAndPersistSession(user, exercise);

        final var found = this.studentSessionRepository.findById(session.id);
        assertNotNull(found);
        assertNotNull(found.user);
        assertEquals("student1", found.user.username);
    }

    private StudentSessionEntity createAndPersistSession(UserEntity user, ExerciseEntity exercise) {
        final var session = new StudentSessionEntity();
        session.sessionId = UlidUtil.generate();
        session.user = user;
        session.exercise = exercise;
        session.publicId = UlidUtil.generate();
        this.studentSessionRepository.persist(session);
        this.studentSessionRepository.flush();
        return session;
    }

    @Test
    @DisplayName("Null handling in repository methods")
    void testNullHandling() {
        assertNull(this.studentSessionRepository.findBySessionId(null));
        assertNull(this.studentSessionRepository.findBySessionIdWithRelations(null));
        assertTrue(this.studentSessionRepository.findByUserId(null).isEmpty());
        assertTrue(this.studentSessionRepository.findByExerciseId(null).isEmpty());
        assertNull(this.studentSessionRepository.findById(null));
        assertTrue(this.studentSessionRepository.findByUserIdAndExerciseId(null, 1L).isEmpty());
        assertTrue(this.studentSessionRepository.findByUserIdAndDateRange(null, null, null).isEmpty());
        assertTrue(this.studentSessionRepository.findByStartTimeAfter(null).isEmpty());
        assertEquals(0L, this.studentSessionRepository.countByCompleted(null));
        assertEquals(0L, this.studentSessionRepository.countActiveStudentsSince(null));
    }

    @Test
    @DisplayName("Additional null handling for methods not covered above")
    void testAdditionalNullHandling() {
        assertTrue(this.studentSessionRepository.findByPublicId(null).isEmpty());
        assertTrue(this.studentSessionRepository.findByStartTimeBefore(null).isEmpty());
        assertTrue(this.studentSessionRepository.findByUserIdIn(null).isEmpty());
        assertTrue(this.studentSessionRepository.findByUserIdIn(List.of()).isEmpty());
        assertTrue(this.studentSessionRepository.findByStartTimeBetween(null, null).isEmpty());
        assertEquals(0L, this.studentSessionRepository.countActiveStudentsBetween(null, null));
        assertEquals(0L, this.studentSessionRepository.countByStartTimeBetween(null, null));
        final long halfOpenCount =
                this.studentSessionRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(null, null);
        assertEquals(0L, halfOpenCount);
        assertTrue(this.studentSessionRepository.findByExerciseIdAndDateRange(null, null, null).isEmpty());
        assertTrue(this.studentSessionRepository.findByCompletedAndDateRange(null, null, null).isEmpty());
        assertTrue(this.studentSessionRepository.searchByUserOrExerciseTerm(null).isEmpty());
        assertTrue(this.studentSessionRepository.searchByUserOrExerciseTerm("").isEmpty());
    }

    @Test
    @DisplayName("findAll, countAll and findProblemCategoryStats return non-null results")
    void testFindAllAndCountAll() {
        assertNotNull(this.studentSessionRepository.findAll());
        assertTrue(this.studentSessionRepository.countAll() >= 0);
        assertNotNull(this.studentSessionRepository.findProblemCategoryStats());
    }

    @Test
    @DisplayName("persist(null) is no-op and deleteById covers null, not-found, and found paths")
    @TestTransaction
    void testPersistNullAndDeleteById() {
        this.studentSessionRepository.persist(null);
        assertFalse(this.studentSessionRepository.deleteById(null));
        assertFalse(this.studentSessionRepository.deleteById(999_999L));

        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var session = createAndPersistSession(user, exercise);

        assertTrue(this.studentSessionRepository.deleteById(session.id));
    }
}
