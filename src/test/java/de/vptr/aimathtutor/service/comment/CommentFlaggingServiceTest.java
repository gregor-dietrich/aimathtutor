package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestCommentFactory;
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
        assertNotNull(user, "Seeded admin user must exist");
        final var ex = assertThrows(WebApplicationException.class, () -> this.flaggingService
                .flagComment("00000000000000000000000000", Objects.requireNonNull(user.id), "spam"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should throw NOT_FOUND for non-existent flagger")
    @TestTransaction
    void shouldThrowNotFoundForNonExistentFlagger() {
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        assertNotNull(exercise, "Seeded exercise must exist");
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin, "Seeded admin user must exist");
        final var comment = TestCommentFactory.createComment(this.commentRepository, exercise, admin);

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.flaggingService.flagComment(Objects.requireNonNull(comment.publicId), 99_999L, "spam"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should prevent self-flagging")
    @TestTransaction
    void shouldPreventSelfFlagging() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        assertNotNull(author, "Seeded admin user must exist");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        assertNotNull(exercise, "Seeded exercise must exist");
        final var comment = TestCommentFactory.createComment(this.commentRepository, exercise, author);

        final var ex = assertThrows(WebApplicationException.class, () -> this.flaggingService
                .flagComment(Objects.requireNonNull(comment.publicId), Objects.requireNonNull(author.id), "spam"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should increment flags count on successful flag")
    @TestTransaction
    void shouldIncrementFlagsCount() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        final UserEntity flagger = this.userRepository.findByUsername("teacher");
        assertNotNull(author, "Seeded admin user must exist");
        assertNotNull(flagger, "Seeded teacher user must exist");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        assertNotNull(exercise, "Seeded exercise must exist");
        final var comment = TestCommentFactory.createComment(this.commentRepository, exercise, author);

        this.flaggingService.flagComment(Objects.requireNonNull(comment.publicId), Objects.requireNonNull(flagger.id),
                "inappropriate");
        assertEquals(1, comment.flagsCount);
    }

    @Test
    @DisplayName("Should auto-hide comment when flags reach threshold")
    @TestTransaction
    void shouldAutoHideWhenFlagsReachThreshold() {
        final UserEntity author = this.userRepository.findByUsername("admin");
        assertNotNull(author, "Seeded admin user must exist");
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);
        assertNotNull(exercise, "Seeded exercise must exist");
        final var comment = TestCommentFactory.createComment(this.commentRepository, exercise, author);

        final UserEntity teacher = this.userRepository.findByUsername("teacher");
        final UserEntity student1 = this.userRepository.findByUsername("student1");
        final UserEntity student2 = this.userRepository.findByUsername("student2");
        assertNotNull(teacher, "Seeded teacher user must exist");
        assertNotNull(student1, "Seeded student1 user must exist");
        assertNotNull(student2, "Seeded student2 user must exist");
        final Long[] flaggerIds = { Objects.requireNonNull(teacher.id), Objects.requireNonNull(student1.id),
                Objects.requireNonNull(student2.id) };
        for (final Long flaggerId : flaggerIds) {
            this.flaggingService.flagComment(Objects.requireNonNull(comment.publicId), flaggerId, "spam");
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
            this.flaggingService.flagComment(Objects.requireNonNull(comment.publicId),
                    Objects.requireNonNull(flagger.id), "spam");
        }

        assertEquals(AppConstants.COMMENT_AUTO_HIDE_THRESHOLD, comment.flagsCount);
        assertEquals(CommentStatus.HIDDEN, comment.status);
    }
}
