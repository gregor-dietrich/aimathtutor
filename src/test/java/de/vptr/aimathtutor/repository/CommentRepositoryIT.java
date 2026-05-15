package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
@SuppressWarnings({ "NullAway", "PMD.TooManyMethods" })
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
                this.commentRepository.findByExerciseIdWithRelations(Objects.requireNonNull(ex.id));
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
    public void testFindByIdOptional_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByIdOptional(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_notFoundReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByIdOptional(-999L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByPublicIdWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByIdOptionalWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByIdOptionalWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByExerciseIdWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findByUserIdWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindBySessionIdWithRelations_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findBySessionIdWithRelations(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindReplies_nullReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findReplies(null).isEmpty());
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

    @Test
    @TestTransaction
    public void testFindRecentCommentsWithRelations_zeroLimitReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findRecentCommentsWithRelations(0).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindRecentCommentsWithRelations_negativeLimitReturnsEmpty() {
        Assertions.assertTrue(this.commentRepository.findRecentCommentsWithRelations(-1).isEmpty());
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
    public void testFindAllOrdered_returnsComments() {
        this.createComment("fall");
        Assertions.assertFalse(this.commentRepository.findAllOrdered().isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindAllOrderedWithRelations_returnsComments() {
        this.createComment("fallwr");
        Assertions.assertFalse(this.commentRepository.findAllOrderedWithRelations().isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final CommentEntity c = this.createComment("fido");
        final var found = this.commentRepository.findByIdOptional(Objects.requireNonNull(c.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(c.content, found.get().content);
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
    public void testFindByPublicIdWithRelations_found() {
        final CommentEntity c = this.createComment("fpubwr");
        final var found = this.commentRepository.findByPublicIdWithRelations(Objects.requireNonNull(c.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(c.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindByIdOptionalWithRelations_found() {
        final CommentEntity c = this.createComment("fidowr");
        final var found = this.commentRepository.findByIdOptionalWithRelations(Objects.requireNonNull(c.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(c.id, found.get().id);
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
    public void testFindByExerciseId_found() {
        final CommentEntity c = this.createComment("fex");
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(c.exercise).id);
        final List<CommentEntity> result = this.commentRepository.findByExerciseId(exId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> Objects.equals(r.id, c.id)));
    }

    @Test
    @TestTransaction
    public void testFindByUserId_found() {
        final CommentEntity c = this.createComment("fuid");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final List<CommentEntity> result = this.commentRepository.findByUserId(userId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> Objects.equals(r.id, c.id)));
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
    public void testFindRecentComments_returnsComments() {
        this.createComment("frecent");
        Assertions.assertFalse(this.commentRepository.findRecentComments(10).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindRecentCommentsWithRelations_positiveLimit() {
        this.createComment("frecentwr");
        Assertions.assertFalse(this.commentRepository.findRecentCommentsWithRelations(10).isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingComment() {
        final CommentEntity c = this.createComment("delid");
        final Long id = Objects.requireNonNull(c.id);
        Assertions.assertTrue(this.commentRepository.deleteById(id));
        Assertions.assertTrue(this.commentRepository.findByIdOptional(id).isEmpty());
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
    public void testFindBySessionId_found() {
        final String sessionId = "sess-cr-" + UUID.randomUUID();
        final UserEntity user = this.createUser("fsess", "cruser_");
        final ExerciseEntity ex = this.createExercise(user, "SessEx", "x+1");
        final CommentEntity c = new CommentEntity();
        c.content = "Session comment";
        c.exercise = ex;
        c.user = user;
        c.sessionId = sessionId;
        this.commentRepository.persist(c);

        final List<CommentEntity> result = this.commentRepository.findBySessionId(sessionId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> sessionId.equals(r.sessionId)));
    }

    @Test
    @TestTransaction
    public void testFindBySessionIdWithRelations_found() {
        final String sessionId = "sess-crwr-" + UUID.randomUUID();
        final UserEntity user = this.createUser("fsesswr", "cruser_");
        final ExerciseEntity ex = this.createExercise(user, "SessWrEx", "x+1");
        final CommentEntity c = new CommentEntity();
        c.content = "Session WR comment";
        c.exercise = ex;
        c.user = user;
        c.sessionId = sessionId;
        this.commentRepository.persist(c);

        final List<CommentEntity> result = this.commentRepository.findBySessionIdWithRelations(sessionId);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindReplies_found() {
        final CommentEntity parent = this.createComment("rplypar");
        final UserEntity user = Objects.requireNonNull(parent.user);
        final ExerciseEntity ex = Objects.requireNonNull(parent.exercise);

        final CommentEntity reply = new CommentEntity();
        reply.content = "Reply content";
        reply.exercise = ex;
        reply.user = user;
        reply.parentComment = parent;
        reply.status = CommentStatus.VISIBLE;
        this.commentRepository.persist(reply);

        final List<CommentEntity> replies = this.commentRepository.findReplies(Objects.requireNonNull(parent.publicId));
        Assertions.assertFalse(replies.isEmpty());
        Assertions.assertTrue(replies.stream().anyMatch(r -> "Reply content".equals(r.content)));
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
    public void testCountByUserSince_returnsCount() {
        final CommentEntity c = this.createComment("cntsince");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final long count =
                this.commentRepository.countByUserSince(userId, LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
        Assertions.assertTrue(count >= 0);
    }

    @Test
    @TestTransaction
    public void testCountByUserInLastDays_returnsCount() {
        final CommentEntity c = this.createComment("cntinterval");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(c.user).id);
        final long count = this.commentRepository.countByUserInLastDays(userId, 1);
        Assertions.assertTrue(count >= 0);
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
