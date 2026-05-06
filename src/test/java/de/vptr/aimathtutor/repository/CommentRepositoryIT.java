package de.vptr.aimathtutor.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link CommentRepository}.
 */
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

    private UserEntity createUser(final String suffix) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "CmtRank_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = "cmtuser_" + suffix;
        user.password = "pw";
        user.email = "cmtuser_" + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    private ExerciseEntity createExercise(final UserEntity user, final String title) {
        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = title;
        ex.content = "x + 1";
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);
        return ex;
    }

    @Test
    @TestTransaction
    public void testFindByExerciseIdWithRelations_eagerFetch() {
        final UserEntity user = this.createUser("eager");
        final ExerciseEntity ex = this.createExercise(user, "EagerExercise");

        final CommentEntity comment = new CommentEntity();
        comment.content = "Nice!";
        comment.exercise = ex;
        comment.user = user;
        this.commentRepository.persist(comment);

        final List<CommentEntity> comments = this.commentRepository.findByExerciseIdWithRelations(ex.id);
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
        final UserEntity user = this.createUser("status");
        final ExerciseEntity ex = this.createExercise(user, "StatusExercise");

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
        final UserEntity user = this.createUser("flag");
        final ExerciseEntity ex = this.createExercise(user, "FlagExercise");

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
        final UserEntity user = this.createUser("srch");
        final ExerciseEntity ex = this.createExercise(user, "SearchExercise");

        final CommentEntity comment = new CommentEntity();
        comment.content = "UniqueSearchableContent_XYZ";
        comment.exercise = ex;
        comment.user = user;
        this.commentRepository.persist(comment);

        final List<CommentEntity> result = this.commentRepository.search("UniqueSearchableContent_XYZ");
        Assertions.assertFalse(result.isEmpty(), "Search should find the comment by content");
        Assertions.assertTrue(result.stream().anyMatch(c -> "UniqueSearchableContent_XYZ".equals(c.content)));
    }
}
