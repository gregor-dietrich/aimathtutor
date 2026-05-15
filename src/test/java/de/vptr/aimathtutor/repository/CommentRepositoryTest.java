package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class CommentRepositoryTest {

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Test
    @DisplayName("findByPublicIdWithRelations should load relations")
    @TestTransaction
    void testFindByPublicIdWithRelations() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        final var comment = new CommentEntity();
        comment.content = "Test content";
        comment.user = user;
        comment.exercise = exercise;
        comment.publicId = UlidUtil.generate();
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        final var found = this.commentRepository.findByPublicIdWithRelations(comment.publicId);
        assertTrue(found.isPresent());
        assertNotNull(found.get().user);
        assertNotNull(found.get().exercise);
        assertEquals("student1", found.get().user.username);
    }

    @Test
    @DisplayName("countByUserInLastSeconds should count correctly")
    @TestTransaction
    void testCountByUserInLastSeconds() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        final long preCount = this.commentRepository.countByUserInLastSeconds(user.id, 60);

        final var comment = new CommentEntity();
        comment.content = "Timed content";
        comment.user = user;
        comment.exercise = exercise;
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        final long postCount = this.commentRepository.countByUserInLastSeconds(user.id, 60);
        assertEquals(preCount + 1, postCount);
    }

    @Test
    @DisplayName("countByUserInLastDays should count correctly")
    @TestTransaction
    void testCountByUserInLastDays() {
        final var user = this.userRepository.findByUsername("student1");
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        final long preCount = this.commentRepository.countByUserInLastDays(user.id, 1);

        final var comment = new CommentEntity();
        comment.content = "Daily content";
        comment.user = user;
        comment.exercise = exercise;
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        final long postCount = this.commentRepository.countByUserInLastDays(user.id, 1);
        assertEquals(preCount + 1, postCount);
    }

    @Test
    @DisplayName("findTopLevelByExercise should filter replies and status")
    @TestTransaction
    void testFindTopLevelByExercise() {
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);

        final var parent = new CommentEntity();
        parent.content = "Parent";
        parent.exercise = exercise;
        parent.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(parent);

        final var reply = new CommentEntity();
        reply.content = "Reply";
        reply.exercise = exercise;
        reply.parentComment = parent;
        reply.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(reply);

        final var hidden = new CommentEntity();
        hidden.content = "Hidden";
        hidden.exercise = exercise;
        hidden.status = CommentStatus.HIDDEN;
        this.commentRepository.persist(hidden);

        this.commentRepository.flush();

        final var topLevel = this.commentRepository.findTopLevelByExercise(exercise.id, 0, 10);
        assertTrue(topLevel.stream().anyMatch(c -> "Parent".equals(c.content)));
        assertFalse(topLevel.stream().anyMatch(c -> "Reply".equals(c.content)));
        assertFalse(topLevel.stream().anyMatch(c -> "Hidden".equals(c.content)));
    }

    @Test
    @DisplayName("deleteByPublicId should remove the comment")
    @TestTransaction
    void testDeleteByPublicId() {
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        final var comment = new CommentEntity();
        comment.content = "To be deleted";
        comment.exercise = exercise;
        comment.publicId = UlidUtil.generate();
        this.commentRepository.persist(comment);
        this.commentRepository.flush();

        assertTrue(this.commentRepository.findByPublicId(comment.publicId).isPresent());
        assertTrue(this.commentRepository.deleteByPublicId(comment.publicId));
        assertFalse(this.commentRepository.findByPublicId(comment.publicId).isPresent());
    }

    @Test
    @DisplayName("Null and not-found handling for repository methods")
    @TestTransaction
    void testNullAndNotFoundHandling() {
        assertTrue(this.commentRepository.findByIdOptional(null).isEmpty());
        assertFalse(this.commentRepository.deleteByPublicId("non-existent-public-id"));
    }

    @Test
    @DisplayName("findRecentCommentsWithRelations should respect limit")
    @TestTransaction
    void testFindRecentCommentsWithRelations() {
        final var exercise = this.exerciseRepository.findAllOrdered().get(0);
        for (int i = 0; i < 5; i++) {
            final var c = new CommentEntity();
            c.content = "Recent " + i;
            c.exercise = exercise;
            this.commentRepository.persist(c);
        }
        this.commentRepository.flush();

        final var recent = this.commentRepository.findRecentCommentsWithRelations(3);
        assertEquals(3, recent.size());
    }
}
