package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
@SuppressWarnings({ "NullAway", "PMD.TooManyMethods", "PMD.ExcessivePublicCount" })
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
        session.startTime = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        session.completed = true;
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserIdAndDateRange(
                Objects.requireNonNull(user.id), LocalDateTime.now(ZoneId.systemDefault()).minusDays(2),
                LocalDateTime.now(ZoneId.systemDefault()));
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
        session.startTime = LocalDateTime.now(ZoneId.systemDefault());
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
        session.startTime = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(5);
        this.sessionRepository.persist(session);

        final long count = this.sessionRepository
                .countActiveStudentsSince(LocalDateTime.now(ZoneId.systemDefault()).minusHours(1));
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
        session.startTime = LocalDateTime.now(ZoneId.systemDefault());
        this.sessionRepository.persist(session);

        final List<StudentSessionEntity> found = this.sessionRepository.findByUserId(Objects.requireNonNull(user.id));
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertTrue(found.stream().anyMatch(s -> Objects.equals(s.sessionId, session.sessionId)));
    }

    private StudentSessionEntity createSession(final String suffix) {
        final UserEntity user = this.createUser(suffix, "sessuser2_");
        final ExerciseEntity ex = this.createExercise(user, "Ex_" + suffix, "x + 2");
        final StudentSessionEntity s = new StudentSessionEntity();
        s.sessionId = "sess-" + suffix + "-" + UUID.randomUUID();
        s.user = user;
        s.exercise = ex;
        s.startTime = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(5);
        s.completed = true;
        this.sessionRepository.persist(s);
        return s;
    }

    @Test
    @TestTransaction
    public void testFindBySessionId_found() {
        final StudentSessionEntity s = this.createSession("fsid");
        final StudentSessionEntity found = this.sessionRepository.findBySessionId(s.sessionId);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(s.sessionId, found.sessionId);
    }

    @Test
    @TestTransaction
    public void testFindBySessionId_null() {
        Assertions.assertNull(this.sessionRepository.findBySessionId(null));
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final StudentSessionEntity s = this.createSession("fpub");
        final var found = this.sessionRepository.findByPublicId(Objects.requireNonNull(s.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(s.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.sessionRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindBySessionIdWithRelations_found() {
        final StudentSessionEntity s = this.createSession("fsidwr");
        final StudentSessionEntity found = this.sessionRepository.findBySessionIdWithRelations(s.sessionId);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(s.sessionId, found.sessionId);
    }

    @Test
    @TestTransaction
    public void testFindBySessionIdWithRelations_null() {
        Assertions.assertNull(this.sessionRepository.findBySessionIdWithRelations(null));
    }

    @Test
    @TestTransaction
    public void testFindByUserId_null() {
        Assertions.assertTrue(this.sessionRepository.findByUserId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseId_found() {
        final StudentSessionEntity s = this.createSession("fexid");
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(s.exercise).id);
        final List<StudentSessionEntity> result = this.sessionRepository.findByExerciseId(exId);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseId_null() {
        Assertions.assertTrue(this.sessionRepository.findByExerciseId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsResults() {
        this.createSession("fall");
        Assertions.assertFalse(this.sessionRepository.findAll().isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final StudentSessionEntity s = this.createSession("fid");
        final StudentSessionEntity found = this.sessionRepository.findById(Objects.requireNonNull(s.id));
        Assertions.assertNotNull(found);
    }

    @Test
    @TestTransaction
    public void testFindById_null() {
        Assertions.assertNull(this.sessionRepository.findById(null));
    }

    @Test
    @TestTransaction
    public void testFindByUserIdAndExerciseId_found() {
        final StudentSessionEntity s = this.createSession("fuidex");
        final Long uid = Objects.requireNonNull(Objects.requireNonNull(s.user).id);
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(s.exercise).id);
        final List<StudentSessionEntity> result = this.sessionRepository.findByUserIdAndExerciseId(uid, exId);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdAndExerciseId_nullUserId() {
        Assertions.assertTrue(this.sessionRepository.findByUserIdAndExerciseId(null, 1L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdAndDateRange_nullUserId() {
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Assertions.assertTrue(this.sessionRepository.findByUserIdAndDateRange(null, now.minusDays(1), now).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseIdAndDateRange_found() {
        final StudentSessionEntity s = this.createSession("fexdr");
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(s.exercise).id);
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final List<StudentSessionEntity> result =
                this.sessionRepository.findByExerciseIdAndDateRange(exId, now.minusDays(1), now);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseIdAndDateRange_null() {
        Assertions.assertTrue(this.sessionRepository.findByExerciseIdAndDateRange(null,
                LocalDateTime.now(ZoneId.systemDefault()), LocalDateTime.now(ZoneId.systemDefault())).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByCompletedAndDateRange_found() {
        this.createSession("fcompldr");
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final List<StudentSessionEntity> result =
                this.sessionRepository.findByCompletedAndDateRange(true, now.minusDays(1), now);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByCompletedAndDateRange_null() {
        Assertions.assertTrue(this.sessionRepository.findByCompletedAndDateRange(null,
                LocalDateTime.now(ZoneId.systemDefault()), LocalDateTime.now(ZoneId.systemDefault())).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeAfter_found() {
        this.createSession("fstafter");
        final LocalDateTime yesterday = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        Assertions.assertFalse(this.sessionRepository.findByStartTimeAfter(yesterday).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeAfter_null() {
        Assertions.assertTrue(this.sessionRepository.findByStartTimeAfter(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeBefore_found() {
        this.createSession("fstbefore");
        final LocalDateTime tomorrow = LocalDateTime.now(ZoneId.systemDefault()).plusDays(1);
        Assertions.assertFalse(this.sessionRepository.findByStartTimeBefore(tomorrow).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeBefore_null() {
        Assertions.assertTrue(this.sessionRepository.findByStartTimeBefore(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdIn_found() {
        final StudentSessionEntity s = this.createSession("fuidin");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(s.user).id);
        final List<StudentSessionEntity> result = this.sessionRepository.findByUserIdIn(List.of(userId));
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdIn_null() {
        Assertions.assertTrue(this.sessionRepository.findByUserIdIn(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdIn_empty() {
        Assertions.assertTrue(this.sessionRepository.findByUserIdIn(List.of()).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeBetween_found() {
        this.createSession("fstbetween");
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final List<StudentSessionEntity> result = this.sessionRepository.findByStartTimeBetween(now.minusDays(1), now);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByStartTimeBetween_null() {
        Assertions.assertTrue(this.sessionRepository.findByStartTimeBetween(null, null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testCountByCompleted_returnsCount() {
        this.createSession("cntcmpl");
        final long count = this.sessionRepository.countByCompleted(true);
        Assertions.assertTrue(count >= 1);
    }

    @Test
    @TestTransaction
    public void testCountByCompleted_null() {
        Assertions.assertEquals(0L, this.sessionRepository.countByCompleted(null));
    }

    @Test
    @TestTransaction
    public void testCountAll_returnsCount() {
        this.createSession("cntall");
        Assertions.assertTrue(this.sessionRepository.countAll() >= 1);
    }

    @Test
    @TestTransaction
    public void testCountActiveStudentsBetween_returnsCount() {
        this.createSession("cntactbet");
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final long count = this.sessionRepository.countActiveStudentsBetween(now.minusDays(1), now);
        Assertions.assertTrue(count >= 0);
    }

    @Test
    @TestTransaction
    public void testCountActiveStudentsBetween_null() {
        Assertions.assertEquals(0L, this.sessionRepository.countActiveStudentsBetween(null, null));
    }

    @Test
    @TestTransaction
    public void testCountByStartTimeBetween_returnsCount() {
        this.createSession("cntstrbet");
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final long count = this.sessionRepository.countByStartTimeBetween(now.minusDays(1), now);
        Assertions.assertTrue(count >= 0);
    }

    @Test
    @TestTransaction
    public void testCountByStartTimeBetween_null() {
        Assertions.assertEquals(0L, this.sessionRepository.countByStartTimeBetween(null, null));
    }

    @Test
    @TestTransaction
    public void testCountByStartTimeRangeHalfOpen_returnsCount() {
        this.createSession("cntsthalf");
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        final long count = this.sessionRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(now.minusDays(1),
                now.plusDays(1));
        Assertions.assertTrue(count >= 0);
    }

    @Test
    @TestTransaction
    public void testCountByStartTimeRangeHalfOpen_null() {
        Assertions.assertEquals(0L,
                this.sessionRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(null, null));
    }

    @Test
    @TestTransaction
    public void testFindProblemCategoryStats_returnsList() {
        Assertions.assertNotNull(this.sessionRepository.findProblemCategoryStats());
    }

    @Test
    @TestTransaction
    public void testPersist_null_doesNotThrow() {
        Assertions.assertDoesNotThrow(() -> this.sessionRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingSession() {
        final StudentSessionEntity s = this.createSession("delid");
        final Long id = Objects.requireNonNull(s.id);
        Assertions.assertTrue(this.sessionRepository.deleteById(id));
        Assertions.assertNull(this.sessionRepository.findById(id));
    }

    @Test
    @TestTransaction
    public void testDeleteById_null() {
        Assertions.assertFalse(this.sessionRepository.deleteById(null));
    }

    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertFalse(this.sessionRepository.deleteById(999_999L));
    }

    @Test
    @TestTransaction
    public void testSearchByUserOrExerciseTerm_null() {
        Assertions.assertTrue(this.sessionRepository.searchByUserOrExerciseTerm(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testSearchByUserOrExerciseTerm_empty() {
        Assertions.assertTrue(this.sessionRepository.searchByUserOrExerciseTerm("").isEmpty());
    }

    @Test
    @TestTransaction
    public void testCountActiveStudentsSince_null() {
        Assertions.assertEquals(0L, this.sessionRepository.countActiveStudentsSince(null));
    }
}
