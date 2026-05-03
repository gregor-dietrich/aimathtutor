package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentRateLimitServiceTest {

    @Inject
    CommentRateLimitService rateLimitService;

    @Inject
    CommentRepository commentRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    EntityManager em;

    @Test
    @DisplayName("Should reject null user ID")
    void shouldRejectNullUserId() {
        final var ex = assertThrows(WebApplicationException.class,
                () -> this.rateLimitService.checkRateLimit(null));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should pass when user has no recent comments")
    @TestTransaction
    void shouldPassWhenNoRecentComments() {
        final UserEntity user = this.userRepository.findById(1L);
        assertDoesNotThrow(() -> this.rateLimitService.checkRateLimit(user.id));
    }

    @Test
    @DisplayName("Should reject when user posted within 5 seconds")
    @TestTransaction
    void shouldRejectWhenPostedWithinFiveSeconds() {
        final UserEntity user = this.userRepository.findById(1L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);

        final var comment = new CommentEntity();
        comment.content = "Test comment";
        comment.exercise = exercise;
        comment.user = user;
        comment.status = "VISIBLE";
        this.commentRepository.persist(comment);
        this.em.flush();

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.rateLimitService.checkRateLimit(user.id));
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should reject when user reached daily limit")
    @TestTransaction
    void shouldRejectWhenReachedDailyLimit() {
        final UserEntity user = this.userRepository.findById(1L);
        final ExerciseEntity exercise = this.exerciseRepository.findById(1L);

        for (int i = 0; i < 200; i++) {
            final var comment = new CommentEntity();
            comment.content = "Bulk comment " + i;
            comment.exercise = exercise;
            comment.user = user;
            comment.status = "VISIBLE";
            this.commentRepository.persist(comment);
        }
        this.em.flush();

        // Backdate all comments so the 5-second window does not trigger first
        this.em.createNativeQuery(
                "UPDATE comments SET created = CURRENT_TIMESTAMP - interval '10 seconds' WHERE user_id = ?1")
                .setParameter(1, user.id)
                .executeUpdate();
        this.em.flush();

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.rateLimitService.checkRateLimit(user.id));
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), ex.getResponse().getStatus());
    }
}
