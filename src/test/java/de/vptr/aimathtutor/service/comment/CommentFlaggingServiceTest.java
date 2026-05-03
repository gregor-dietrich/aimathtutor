package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentFlaggingServiceTest {

    @Inject
    CommentFlaggingService flaggingService;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Test
    @DisplayName("Should throw NOT_FOUND for non-existent comment")
    @TestTransaction
    void shouldThrowNotFoundForNonExistentComment() {
        final UserEntity user = this.userRepository.findByUsername("admin");
        final var ex = assertThrows(WebApplicationException.class,
                () -> this.flaggingService.flagComment(99999L, user.id, "spam"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND for non-existent flagger")
    @TestTransaction
    void shouldThrowNotFoundForNonExistentFlagger() {
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, this.userRepository.findByUsername("admin"));

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.flaggingService.flagComment(comment.id, 99999L, "spam"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should prevent self-flagging")
    @TestTransaction
    void shouldPreventSelfFlagging() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.flaggingService.flagComment(comment.id, author.id, "spam"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should increment flags count on successful flag")
    @TestTransaction
    void shouldIncrementFlagsCount() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        final UserEntity flagger = this.userRepository.findByUsername("teacher");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        this.flaggingService.flagComment(comment.id, flagger.id, "inappropriate");
        assertEquals(1, comment.flagsCount);
    }

    @Test
    @DisplayName("Should auto-hide comment when flags reach threshold")
    @TestTransaction
    void shouldAutoHideWhenFlagsReachThreshold() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        final var comment = this.createComment(exercise, author);

        final Long[] flaggerIds = {
                this.userRepository.findByUsername("teacher").id,
                this.userRepository.findByUsername("student1").id,
                this.userRepository.findByUsername("student2").id
        };
        for (final Long flaggerId : flaggerIds) {
            this.flaggingService.flagComment(comment.id, flaggerId, "spam");
        }

        // Create additional flaggers to reach threshold
        final UserRankEntity studentRank = this.userRankRepository.findById(3L);
        for (int i = 0; i < AppConstants.COMMENT_AUTO_HIDE_THRESHOLD - flaggerIds.length; i++) {
            final UserEntity flagger = new UserEntity();
            flagger.username = "flagger" + i;
            flagger.password = "password";
            flagger.rank = studentRank;
            flagger.activated = true;
            flagger.banned = false;
            this.userRepository.persist(flagger);
            this.flaggingService.flagComment(comment.id, flagger.id, "spam");
        }

        assertEquals(AppConstants.COMMENT_AUTO_HIDE_THRESHOLD, comment.flagsCount);
        assertEquals("HIDDEN", comment.status);
    }

    private CommentEntity createComment(final ExerciseEntity exercise, final UserEntity user) {
        final var comment = new CommentEntity();
        comment.content = "Test comment";
        comment.exercise = exercise;
        comment.user = user;
        comment.status = "VISIBLE";
        this.commentRepository.persist(comment);
        return comment;
    }
}
