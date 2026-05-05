package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class CommentServiceTest {

    @Inject
    private CommentService commentService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private UserRepository userRepository;

    private ExerciseViewDto createCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher);
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "ex_" + suffix;
        dto.content = "exercise content " + suffix;
        dto.userId = teacher.id;
        dto.published = true;
        dto.commentable = true;
        return this.exerciseService.createExercise(dto);
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with null content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithNullContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = null;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with empty content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithEmptyContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with whitespace content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithWhitespaceContent() {
        final CommentEntity comment = new CommentEntity();
        comment.content = "   ";

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(comment, "testuser");
        });
    }

    @Test
    @DisplayName("Should sanitize HTML tags from comment content")
    @TestTransaction
    void shouldSanitizeHtmlInComment() {
        final ExerciseViewDto exercise = this.createCommentableExercise();

        final CommentEntity comment = new CommentEntity();
        comment.content = "<script>alert(1)</script>safe text";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = exercise.id;
        comment.exercise = exerciseRef;

        final CommentViewDto created = this.commentService.createComment(comment, "student1");

        assertNotNull(created);
        assertNotNull(created.content);
        assertFalse(created.content.contains("<script>"),
                "Sanitizer should strip <script>, got: " + created.content);
        assertTrue(created.content.contains("safe text"));
    }

    @Test
    @DisplayName("Should reject comment on non-commentable exercise")
    @TestTransaction
    void shouldRejectCommentOnNonCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "teacher fixture must exist");
        final var dto = new ExerciseDto();
        dto.title = "noncommentable_" + UUID.randomUUID().toString().substring(0, 8);
        dto.content = "x";
        dto.userId = teacher.id;
        dto.published = true;
        dto.commentable = false;
        final var ex = this.exerciseService.createExercise(dto);

        final CommentEntity comment = new CommentEntity();
        comment.content = "hi";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = ex.id;
        comment.exercise = exerciseRef;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.commentService.createComment(comment, "student1"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should find comment by id after creation")
    @TestTransaction
    void shouldFindCommentById() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final CommentEntity comment = new CommentEntity();
        comment.content = "hello world";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = exercise.id;
        comment.exercise = exerciseRef;
        final CommentViewDto created = this.commentService.createComment(comment, "student1");

        final var found = this.commentService.findById(created.id);

        assertTrue(found.isPresent());
        assertEquals("hello world", found.get().content);
    }

    @Test
    @DisplayName("Should list comments by exercise id")
    @TestTransaction
    void shouldListCommentsByExercise() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final CommentEntity comment = new CommentEntity();
        comment.content = "comment one";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = exercise.id;
        comment.exercise = exerciseRef;
        this.commentService.createComment(comment, "student1");

        final var comments = this.commentService.findByExerciseId(exercise.id);

        assertEquals(1, comments.size());
        assertEquals("comment one", comments.get(0).content);
    }

    @Test
    @DisplayName("Should soft-delete comment as author")
    @TestTransaction
    void shouldSoftDeleteCommentAsAuthor() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final CommentEntity comment = new CommentEntity();
        comment.content = "to delete";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = exercise.id;
        comment.exercise = exerciseRef;
        final CommentViewDto created = this.commentService.createComment(comment, "student1");
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");

        this.commentService.deleteComment(created.id, student.id, true);

        final var found = this.commentService.findById(created.id);
        assertTrue(found.isPresent(), "Soft-deleted comment should still be findable");
    }

    @Test
    @DisplayName("Should hide comment via moderation")
    @TestTransaction
    void shouldHideCommentViaModeration() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final CommentEntity comment = new CommentEntity();
        comment.content = "needs hiding";
        final var exerciseRef = new ExerciseEntity();
        exerciseRef.id = exercise.id;
        comment.exercise = exerciseRef;
        final CommentViewDto created = this.commentService.createComment(comment, "student1");
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin);

        this.commentService.moderateComment(created.id, "HIDE", admin.id, "spam");

        final var found = this.commentService.findById(created.id);
        assertTrue(found.isPresent());
        final var hidden = this.commentService.findByStatus(CommentStatus.HIDDEN);
        assertTrue(hidden.stream().anyMatch(c -> c.id.equals(created.id)));
    }
}
