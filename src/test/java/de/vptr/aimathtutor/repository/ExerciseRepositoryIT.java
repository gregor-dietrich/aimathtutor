package de.vptr.aimathtutor.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
public class ExerciseRepositoryIT {

    @Inject
    ExerciseRepository exerciseRepository;
    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    public void testFindPublishedAndSearch() {
        UserEntity user = new UserEntity();
        user.username = "searchuser";
        user.password = "pw";
        user.salt = "salt";
        user.email = "search@example.com";
        user.activated = true;
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
