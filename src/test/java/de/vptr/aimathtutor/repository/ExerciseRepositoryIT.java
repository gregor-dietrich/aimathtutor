package de.vptr.aimathtutor.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.quarkus.test.TestTransaction;

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

    @Test
    @TestTransaction
    public void testFindPublishedAndSearch() {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "ExerciseTestRank";
        this.userRankRepository.persist(rank);

        UserEntity user = new UserEntity();
        user.username = "searchuser";
        user.password = "pw";
        user.email = "search@example.com";
        user.activated = true;
        user.rank = rank;
        userRepository.persist(user);

        ExerciseEntity ex1 = new ExerciseEntity();
        ex1.title = "Quadratic";
        ex1.content = "x^2 + 2x + 1";
        ex1.user = user;
        ex1.published = true;
        exerciseRepository.persist(ex1);

        ExerciseEntity ex2 = new ExerciseEntity();
        ex2.title = "Linear";
        ex2.content = "x + 1";
        ex2.user = user;
        ex2.published = false;
        exerciseRepository.persist(ex2);

        List<ExerciseEntity> published = exerciseRepository.findPublished();
        Assertions.assertTrue(published.stream().anyMatch(e -> e.title.equals("Quadratic")));
        Assertions.assertFalse(published.stream().anyMatch(e -> e.title.equals("Linear")));

        List<ExerciseEntity> search = exerciseRepository.search("quad");
        Assertions.assertTrue(search.stream().anyMatch(e -> e.title.equals("Quadratic")));
    }
}
