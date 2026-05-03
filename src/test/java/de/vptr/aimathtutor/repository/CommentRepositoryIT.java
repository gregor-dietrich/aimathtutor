package de.vptr.aimathtutor.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.quarkus.test.TestTransaction;

@QuarkusTest
public class CommentRepositoryIT {

    @Inject
    CommentRepository commentRepository;
    @Inject
    ExerciseRepository exerciseRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserRankRepository userRankRepository;

    @Test
    @TestTransaction
    public void testFindByExerciseIdWithRelations_eagerFetch() {
        // Setup: create user, exercise, comment
        UserEntity user = new UserEntity();
        user.username = "testuser";
        user.password = "pw";
        user.email = "test@example.com";
        user.activated = true;
        user.rank = this.userRankRepository.findById(1L);
        userRepository.persist(user);

        ExerciseEntity ex = new ExerciseEntity();
        ex.title = "Test Exercise";
        ex.content = "x + 1";
        ex.user = user;
        ex.published = true;
        exerciseRepository.persist(ex);

        CommentEntity comment = new CommentEntity();
        comment.content = "Nice!";
        comment.exercise = ex;
        comment.user = user;
        commentRepository.persist(comment);

        List<CommentEntity> comments = commentRepository.findByExerciseIdWithRelations(ex.id);
        Assertions.assertFalse(comments.isEmpty());
        CommentEntity loaded = comments.get(0);
        Assertions.assertNotNull(loaded.user);
        Assertions.assertNotNull(loaded.exercise);
        Assertions.assertEquals("testuser", loaded.user.username);
        Assertions.assertEquals("Test Exercise", loaded.exercise.title);
    }
}
