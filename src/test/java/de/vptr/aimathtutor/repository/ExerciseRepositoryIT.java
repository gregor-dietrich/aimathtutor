package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

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
    public void testFindGraspableMathExercises_returnsOnlyGraspable() {
        final UserEntity user = this.createUser("grsp");

        final ExerciseEntity graspable = new ExerciseEntity();
        graspable.title = "GraspableEx";
        graspable.content = "2x + 1";
        graspable.user = user;
        graspable.published = true;
        graspable.graspableEnabled = true;
        graspable.graspableTargetExpression = "x=1";
        this.exerciseRepository.persist(graspable);

        final ExerciseEntity plain = new ExerciseEntity();
        plain.title = "PlainEx";
        plain.content = "3y";
        plain.user = user;
        plain.published = true;
        plain.graspableEnabled = false;
        this.exerciseRepository.persist(plain);

        final List<ExerciseEntity> result = this.exerciseRepository.findGraspableMathExercises();
        Assertions.assertTrue(result.stream().allMatch(e -> Boolean.TRUE.equals(e.graspableEnabled)),
                "All returned exercises should have graspableEnabled=true");
        Assertions.assertTrue(result.stream().anyMatch(e -> "GraspableEx".equals(e.title)));
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
}
