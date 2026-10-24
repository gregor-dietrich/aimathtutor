package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link CommentRepository}.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
public class CommentRepositoryIT extends AbstractRepositoryIT {

    @Inject
    CommentRepository commentRepository;

    @Test
    @TestTransaction
    public void testFindByExerciseIdWithRelations_eagerFetch() {
        final UserEntity user = this.createUser("eager", "cmtuser_");
        final ExerciseEntity ex = this.createExercise(user, "EagerExercise", "x + 1");

        final CommentEntity comment = new CommentEntity();
        comment.content = "Nice!";
        comment.exercise = ex;
        comment.user = user;
        this.commentRepository.persist(comment);

        final List<CommentEntity> comments =
                this.commentRepository.findByExerciseIdWithRelations(Objects.requireNonNull(ex.id), 0, 10);
        Assertions.assertFalse(comments.isEmpty());
        final CommentEntity loaded = comments.get(0);
        Assertions.assertNotNull(loaded.user);
        Assertions.assertNotNull(loaded.exercise);
        Assertions.assertEquals("cmtuser_eager", loaded.user.username);
        Assertions.assertEquals("EagerExercise", loaded.exercise.title);
    }

    @Test
    @TestTransaction
    public void testFindByStatus_returnsOnlyVisible() {
        final UserEntity user = this.createUser("status", "cmtuser_");
        final ExerciseEntity ex = this.createExercise(user, "StatusExercise", "x + 1");

        final CommentEntity visible = new CommentEntity();
        visible.content = "Visible comment";
        visible.exercise = ex;
        visible.user = user;
        visible.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(visible);

        final CommentEntity hidden = new CommentEntity();
        hidden.content = "Hidden comment";
        hidden.exercise = ex;
        hidden.user = user;
        hidden.status = CommentStatus.HIDDEN;
        this.commentRepository.persist(hidden);

        final List<CommentEntity> result = this.commentRepository.findByStatus(CommentStatus.VISIBLE);
        Assertions.assertTrue(result.stream().allMatch(c -> CommentStatus.VISIBLE.equals(c.status)),
                "All returned comments should be VISIBLE");
        Assertions.assertTrue(result.stream().anyMatch(c -> "Visible comment".equals(c.content)));
        Assertions.assertFalse(result.stream().anyMatch(c -> "Hidden comment".equals(c.content)));
    }

    @Test
    @TestTransaction
    public void testFindFlaggedComments_returnsAboveThreshold() {
        final UserEntity user = this.createUser("flag", "cmtuser_");
        final ExerciseEntity ex = this.createExercise(user, "FlagExercise", "x + 1");

        final CommentEntity flagged = new CommentEntity();
        flagged.content = "Flagged comment";
        flagged.exercise = ex;
        flagged.user = user;
        flagged.flagsCount = 3;
        this.commentRepository.persist(flagged);

        final CommentEntity clean = new CommentEntity();
        clean.content = "Clean comment";
        clean.exercise = ex;
        clean.user = user;
        clean.flagsCount = 0;
        this.commentRepository.persist(clean);

        final List<CommentEntity> result = this.commentRepository.findFlaggedComments(2);
        Assertions.assertTrue(result.stream().anyMatch(c -> "Flagged comment".equals(c.content)),
                "Flagged comment with 3 flags should appear when threshold is 2");
        Assertions.assertFalse(result.stream().anyMatch(c -> "Clean comment".equals(c.content)),
                "Clean comment should not appear");
    }

    @Test
    @TestTransaction
    public void testSearch_returnsMatchingComments() {
        final UserEntity user = this.createUser("srch", "cmtuser_");
        final ExerciseEntity ex = this.createExercise(user, "SearchExercise", "x + 1");

        final CommentEntity comment = new CommentEntity();
        comment.content = "UniqueSearchableContent_XYZ";
        comment.exercise = ex;
        comment.user = user;
        this.commentRepository.persist(comment);

        final List<CommentEntity> result = this.commentRepository.search("UniqueSearchableContent_XYZ");
        Assertions.assertFalse(result.isEmpty(), "Search should find the comment by content");
        Assertions.assertTrue(result.stream().anyMatch(c -> "UniqueSearchableContent_XYZ".equals(c.content)));
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByExerciseIdWithRelations(null, 0, 10).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByUserIdWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindRepliesWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findRepliesWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindRepliesPaged_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findRepliesPaged(null, 0, 10).isEmpty());
    }

    @Test
    @TestTransaction
    public void testPersist_nullReturnsNull() {
        Assertions.assertNull(this.commentRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDeleteById_notFoundReturnsFalse() {
        Assertions.assertFalse(this.commentRepository.deleteById(-999L));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_notFoundReturnsFalse() {
        Assertions.assertFalse(this.commentRepository.deleteByPublicId("00000000000000000000000000"));
    }

    private CommentEntity createComment(final String suffix) {
        final UserEntity user = this.createUser(suffix, "cruser_");
        final ExerciseEntity ex = this.createExercise(user, "Ex_" + suffix, "x+1");
        final CommentEntity c = new CommentEntity();
        c.content = "Content_" + suffix;
        c.exercise = ex;
        c.user = user;
        this.commentRepository.persist(c);
        return c;
    }

    @Test
    @TestTransaction
    public void testFindAllOrderedWithRelations_returnsComments() {
        this.createComment("fallwr");
        Assertions.assertFalse(this.commentRepository.findAllOrderedWithRelations(0, 10).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final CommentEntity c = this.createComment("fpub");
        final var found = this.commentRepository.findByPublicId(Objects.requireNonNull(c.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(c.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final CommentEntity c = this.createComment("fid");
        final CommentEntity found = this.commentRepository.findById(Objects.requireNonNull(c.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(c.content, found.content);
    }

    @Test
    @TestTransaction
    public void testFindByUserIdWithRelations_found() {
        final CommentEntity c = this.createComment("fuidwr");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final List<CommentEntity> result = this.commentRepository.findByUserIdWithRelations(userId);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingComment() {
        final CommentEntity c = this.createComment("delid");
        final Long id = Objects.requireNonNull(c.id);
        Assertions.assertTrue(this.commentRepository.deleteById(id));
        Assertions.assertNull(this.commentRepository.findById(id));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existingComment() {
        final CommentEntity c = this.createComment("delpub");
        final String publicId = Objects.requireNonNull(c.publicId);
        Assertions.assertTrue(this.commentRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.commentRepository.findByPublicId(publicId).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindRepliesWithRelations_found() {
        final CommentEntity parent = this.createComment("rplywrpar");
        final UserEntity user = Objects.requireNonNull(parent.user);
        final ExerciseEntity ex = Objects.requireNonNull(parent.exercise);

        final CommentEntity reply = new CommentEntity();
        reply.content = "Reply WR content";
        reply.exercise = ex;
        reply.user = user;
        reply.parentComment = parent;
        reply.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(reply);

        final List<CommentEntity> replies =
                this.commentRepository.findRepliesWithRelations(Objects.requireNonNull(parent.publicId));
        Assertions.assertFalse(replies.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindTopLevelByExercise_returnsTopLevelComments() {
        final CommentEntity c = this.createComment("toplvl");
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(c.exercise).id);
        final List<CommentEntity> result = this.commentRepository.findTopLevelByExercise(exId, 0, 10);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().allMatch(r -> r.parentComment == null));
    }

    @Test
    @TestTransaction
    public void testFindRepliesPaged_found() {
        final CommentEntity parent = this.createComment("rplypagedpar");
        final UserEntity user = Objects.requireNonNull(parent.user);
        final ExerciseEntity ex = Objects.requireNonNull(parent.exercise);

        final CommentEntity reply = new CommentEntity();
        reply.content = "Paged reply";
        reply.exercise = ex;
        reply.user = user;
        reply.parentComment = parent;
        reply.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(reply);

        final List<CommentEntity> replies =
                this.commentRepository.findRepliesPaged(Objects.requireNonNull(parent.publicId), 0, 10);
        Assertions.assertFalse(replies.isEmpty());
    }

    @Test
    @TestTransaction
    public void testCountByUserInLastDays_returnsCount() {
        final CommentEntity c = this.createComment("cntinterval");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final long count = this.commentRepository.countByUserInLastDays(userId, 1);
        Assertions.assertTrue(count >= 1, "expected at least one comment for the created user");
    }

    @Test
    @TestTransaction
    public void testCountByUserInLastSeconds_returnsCount() {
        final CommentEntity c = this.createComment("cntsec");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final long count = this.commentRepository.countByUserInLastSeconds(userId, 60);
        Assertions.assertTrue(count >= 1, "expected at least one comment for the created user in the last 60 seconds");
    }

    @Test
    public void testCountByUserInLastSeconds_throwsOnInvalidWindow() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.commentRepository.countByUserInLastSeconds(1L, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.commentRepository.countByUserInLastSeconds(1L, -1));
    }

    @Test
    public void testCountByUserInLastDays_throwsOnInvalidWindow() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.commentRepository.countByUserInLastDays(1L, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.commentRepository.countByUserInLastDays(1L, -1));
    }

    @Test
    @TestTransaction
    public void testFindByDateRange_returnsComments() {
        this.createComment("daterange");
        final LocalDateTime start = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
        final LocalDateTime end = LocalDateTime.now(ZoneId.systemDefault()).plusDays(1);
        final List<CommentEntity> result = this.commentRepository.findByDateRange(start, end);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testPersist_validComment() {
        final UserEntity user = this.createUser("perval", "cruser_");
        final ExerciseEntity ex = this.createExercise(user, "PersistEx", "y+2");
        final CommentEntity c = new CommentEntity();
        c.content = "Persisted comment";
        c.exercise = ex;
        c.user = user;
        final CommentEntity result = this.commentRepository.persist(c);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.id);
        Assertions.assertNotNull(result.publicId);
    }
}
