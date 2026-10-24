package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link ExerciseRepository}.
 */
@QuarkusTest
public class ExerciseRepositoryIT {

    @Inject
    ExerciseRepository exerciseRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserRankRepository userRankRepository;
    @Inject
    LessonRepository lessonRepository;

    private UserEntity createUser(final String suffix) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "Rank_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = "exuser_" + suffix;
        user.password = "pw";
        user.email = "exuser_" + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    private ExerciseEntity createExercise(final UserEntity user, final String suffix) {
        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = "Ex_" + suffix;
        ex.content = "x + 1";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);
        return ex;
    }

    @Test
    @TestTransaction
    public void testFindPublishedAndSearch() {
        final UserEntity user = this.createUser("srch");

        final ExerciseEntity ex1 = new ExerciseEntity();
        ex1.title = "Quadratic";
        ex1.content = "x^2 + 2x + 1";
        ex1.user = user;
        ex1.published = true;
        this.exerciseRepository.persist(ex1);

        final ExerciseEntity ex2 = new ExerciseEntity();
        ex2.title = "Linear";
        ex2.content = "x + 1";
        ex2.user = user;
        ex2.published = false;
        this.exerciseRepository.persist(ex2);

        final List<ExerciseEntity> published = this.exerciseRepository.findPublished();
        Assertions.assertTrue(published.stream().anyMatch(e -> "Quadratic".equals(e.title)));
        Assertions.assertFalse(published.stream().anyMatch(e -> "Linear".equals(e.title)));

        final List<ExerciseEntity> search = this.exerciseRepository.search("quad");
        Assertions.assertTrue(search.stream().anyMatch(e -> "Quadratic".equals(e.title)));
    }

    @Test
    @TestTransaction
    public void testFindByUserId_returnsUserExercises() {
        final UserEntity user = this.createUser("byuid");

        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = "UserExercise";
        ex.content = "y + 3";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);

        final List<ExerciseEntity> result = this.exerciseRepository.findByUserId(Objects.requireNonNull(user.id));
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(e -> "UserExercise".equals(e.title)));
    }

    @Test
    @TestTransaction
    public void testFindByDateRange_includesRecentExercise() {
        final UserEntity user = this.createUser("drng");

        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = "DateRangeEx";
        ex.content = "4z";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);

        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusDays(1);
        final List<ExerciseEntity> result = this.exerciseRepository.findByDateRange(start, end);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(e -> "DateRangeEx".equals(e.title)));
    }

    @Test
    @TestTransaction
    public void testSearch_blankQueryReturnsAll() {
        final UserEntity user = this.createUser("blnk");

        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = "BlankSearchEx";
        ex.content = "5w";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);

        final List<ExerciseEntity> result = this.exerciseRepository.search("");
        Assertions.assertFalse(result.isEmpty(), "Blank search should return all exercises");
        Assertions.assertTrue(result.stream().anyMatch(e -> "BlankSearchEx".equals(e.title)),
                "Blank search should include the test exercise");
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testSearch_nullReturnsAll() {
        final UserEntity user = this.createUser("nullsrch");
        this.createExercise(user, "nullsrch");
        final List<ExerciseEntity> result = this.exerciseRepository.search(null);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindAllOrdered_returnsResults() {
        final UserEntity user = this.createUser("fall");
        this.createExercise(user, "fall");
        Assertions.assertFalse(this.exerciseRepository.findAllOrdered().isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserEntity user = this.createUser("fpub");
        final ExerciseEntity ex = this.createExercise(user, "fpub");
        final Optional<ExerciseEntity> found =
                this.exerciseRepository.findByPublicId(Objects.requireNonNull(ex.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(ex.publicId, found.get().publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.exerciseRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final UserEntity user = this.createUser("fid");
        final ExerciseEntity ex = this.createExercise(user, "fid");
        final ExerciseEntity found = this.exerciseRepository.findById(Objects.requireNonNull(ex.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(ex.id, found.id);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindById_null() {
        Assertions.assertNull(this.exerciseRepository.findById(null));
    }

    @Test
    @TestTransaction
    public void testCountPublished_returnsCount() {
        final UserEntity user = this.createUser("cntpub");
        this.createExercise(user, "cntpub");
        Assertions.assertTrue(this.exerciseRepository.countPublished() >= 1);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testPersist_null_returnsNull() {
        Assertions.assertNull(this.exerciseRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existing() {
        final UserEntity user = this.createUser("delpub");
        final ExerciseEntity ex = this.createExercise(user, "delpub");
        final String publicId = Objects.requireNonNull(ex.publicId);
        Assertions.assertTrue(this.exerciseRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.exerciseRepository.findByPublicId(publicId).isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_nonExisting() {
        Assertions.assertFalse(this.exerciseRepository.deleteByPublicId("nonexistent"));
    }
}
