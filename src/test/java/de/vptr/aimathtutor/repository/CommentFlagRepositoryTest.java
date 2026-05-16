package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.CommentFlagEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
@SuppressWarnings("NullAway")
class CommentFlagRepositoryTest {

    @Inject
    CommentFlagRepository commentFlagRepository;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Test
    @DisplayName("Null and not-found handling for repository methods")
    @TestTransaction
    void testNullAndNotFoundHandling() {
        assertTrue(this.commentFlagRepository.findByPublicId(null).isEmpty());
        assertTrue(this.commentFlagRepository.findByPublicId("not-found-public-id").isEmpty());
        assertFalse(this.commentFlagRepository.hasUserFlaggedComment(null, 1L));
        assertFalse(this.commentFlagRepository.hasUserFlaggedComment(1L, null));
        assertTrue(this.commentFlagRepository.findByIdOptional(null).isEmpty());
        this.commentFlagRepository.persist(null);
        assertTrue(this.commentFlagRepository.findByIdOptional(null).isEmpty());
    }

    @Test
    @DisplayName("createFlag should throw when comment or flagger is null")
    @TestTransaction
    void testCreateFlag_nullArgs_throws() {
        final var user = this.userRepository.findByUsername("student1");
        assertThrows(WebApplicationException.class, () -> this.commentFlagRepository.createFlag(null, user));
        assertThrows(WebApplicationException.class,
                () -> this.commentFlagRepository.createFlag(new CommentEntity(), null));
    }

    @Test
    @DisplayName("createFlag should create and findByPublicId should find it")
    @TestTransaction
    void testCreateFlag_success() {
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var student1 = this.userRepository.findByUsername("student1");
        final var student2 = this.userRepository.findByUsername("student2");

        final var comment = new CommentEntity();
        comment.content = "Flaggable comment";
        comment.exercise = exercise;
        comment.user = student1;
        comment.publicId = UlidUtil.generate();
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        assertFalse(this.commentFlagRepository.hasUserFlaggedComment(comment.id, student2.id));

        final CommentFlagEntity flag = this.commentFlagRepository.createFlag(comment, student2);
        assertNotNull(flag);
        assertNotNull(flag.publicId);

        assertTrue(this.commentFlagRepository.hasUserFlaggedComment(comment.id, student2.id));
        assertTrue(this.commentFlagRepository.findByPublicId(flag.publicId).isPresent());
        assertTrue(this.commentFlagRepository.findByIdOptional(flag.id).isPresent());
    }

    @Test
    @DisplayName("createFlag should throw when user already flagged the comment")
    @TestTransaction
    void testCreateFlag_alreadyFlagged_throws() {
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var student1 = this.userRepository.findByUsername("student1");
        final var student2 = this.userRepository.findByUsername("student2");

        final var comment = new CommentEntity();
        comment.content = "Double flag target";
        comment.exercise = exercise;
        comment.user = student1;
        comment.publicId = UlidUtil.generate();
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        this.commentFlagRepository.createFlag(comment, student2);

        assertThrows(WebApplicationException.class, () -> this.commentFlagRepository.createFlag(comment, student2));
    }
}
