package de.vptr.aimathtutor.repository;

import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.CommentFlagEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/**
 * Integration tests for {@link CommentFlagRepository}.
 */
@QuarkusTest
public class CommentFlagRepositoryIT extends AbstractRepositoryIT {

    @Inject
    CommentFlagRepository commentFlagRepository;

    @Inject
    CommentRepository commentRepository;

    private CommentEntity createComment(final UserEntity user, final ExerciseEntity ex, final String content) {
        final CommentEntity comment = new CommentEntity();
        comment.content = content;
        comment.exercise = ex;
        comment.user = user;
        this.commentRepository.persist(comment);
        return comment;
    }

    private CommentFlagEntity createFlag(final CommentEntity comment, final UserEntity flagger) {
        final CommentFlagEntity flag = new CommentFlagEntity();
        flag.comment = comment;
        flag.flagger = flagger;
        this.commentFlagRepository.persist(flag);
        return flag;
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserEntity user = this.createUser("cfpub", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFPublicIdEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "CF comment");
        final CommentFlagEntity flag = this.createFlag(comment, user);

        final Optional<CommentFlagEntity> found =
                this.commentFlagRepository.findByPublicId(Objects.requireNonNull(flag.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(flag.publicId, found.get().publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.commentFlagRepository.findByPublicId(null).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_notFound() {
        Assertions.assertTrue(this.commentFlagRepository.findByPublicId("nonexistent").isEmpty());
    }

    @Test
    @TestTransaction
    public void testHasUserFlaggedComment_true() {
        final UserEntity user = this.createUser("cfhuf", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFHasFlagEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Flags comment");
        this.createFlag(comment, user);

        Assertions.assertTrue(this.commentFlagRepository.hasUserFlaggedComment(Objects.requireNonNull(comment.id),
                Objects.requireNonNull(user.id)));
    }

    @Test
    @TestTransaction
    public void testHasUserFlaggedComment_false() {
        final UserEntity flagger = this.createUser("cfhff", "cfuser_");
        final UserEntity other = this.createUser("cfhfo", "cfuser_");
        final ExerciseEntity ex = this.createExercise(flagger, "CFNoFlagEx", "x + 1");
        final CommentEntity comment = this.createComment(flagger, ex, "No flag comment");

        Assertions.assertFalse(this.commentFlagRepository.hasUserFlaggedComment(Objects.requireNonNull(comment.id),
                Objects.requireNonNull(other.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testHasUserFlaggedComment_nullParams() {
        Assertions.assertFalse(this.commentFlagRepository.hasUserFlaggedComment(null, 1L));
        Assertions.assertFalse(this.commentFlagRepository.hasUserFlaggedComment(1L, null));
        Assertions.assertFalse(this.commentFlagRepository.hasUserFlaggedComment(null, null));
    }

    @Test
    @TestTransaction
    public void testPersist_flag() {
        final UserEntity user = this.createUser("cfper", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFPersistEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Persist flag comment");

        final CommentFlagEntity flag = new CommentFlagEntity();
        flag.comment = comment;
        flag.flagger = user;
        this.commentFlagRepository.persist(flag);

        Assertions.assertNotNull(flag.id);
        Assertions.assertNotNull(flag.publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testPersist_null() {
        Assertions.assertDoesNotThrow(() -> this.commentFlagRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testCreateFlag_success() {
        final UserEntity user = this.createUser("cfcrs", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFCreateEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Create flag comment");

        final CommentFlagEntity flag = this.commentFlagRepository.createFlag(comment, user);
        Assertions.assertNotNull(flag);
        Assertions.assertNotNull(flag.id);
        Assertions.assertEquals(comment.id, Objects.requireNonNull(flag.comment).id);
        Assertions.assertEquals(user.id, Objects.requireNonNull(flag.flagger).id);
    }

    @Test
    @TestTransaction
    public void testCreateFlag_duplicateThrows() {
        final UserEntity user = this.createUser("cfcrd", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFDupEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Duplicate flag comment");
        this.commentFlagRepository.createFlag(comment, user);

        Assertions.assertThrows(WebApplicationException.class,
                () -> this.commentFlagRepository.createFlag(comment, user));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testCreateFlag_nullParamsThrows() {
        final UserEntity user = this.createUser("cfcrn", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFNullEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Null params flag");

        Assertions.assertThrows(WebApplicationException.class, () -> this.commentFlagRepository.createFlag(null, user));
        Assertions.assertThrows(WebApplicationException.class,
                () -> this.commentFlagRepository.createFlag(comment, null));
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final UserEntity user = this.createUser("cfido", "cfuser_");
        final ExerciseEntity ex = this.createExercise(user, "CFIdOptEx", "x + 1");
        final CommentEntity comment = this.createComment(user, ex, "Find by ID comment");
        final CommentFlagEntity flag = this.createFlag(comment, user);

        final Optional<CommentFlagEntity> found =
                this.commentFlagRepository.findByIdOptional(Objects.requireNonNull(flag.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(flag.id, found.get().id);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByIdOptional_null() {
        Assertions.assertTrue(this.commentFlagRepository.findByIdOptional(null).isEmpty());
    }
}
