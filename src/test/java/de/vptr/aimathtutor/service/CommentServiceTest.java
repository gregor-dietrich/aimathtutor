package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.InjectMock;
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

    @Inject
    private CommentRepository commentRepository;

    @InjectMock
    private PermissionService permissionService;

    private ExerciseViewDto createCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher);
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "ex_" + suffix;
        dto.content = "exercise content " + suffix;
        dto.userPublicId = teacher.publicId;
        dto.published = true;
        dto.commentable = true;
        return this.exerciseService.createExercise(dto);
    }

    private Long getCommentNumericId(final String publicId) {
        return this.commentRepository.findByPublicId(publicId)
                .map(c -> c.id)
                .orElseThrow(() -> new AssertionError("Comment not found: " + publicId));
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with null content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithNullContent() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");
        final var dto = new CommentDto();
        dto.content = null;
        dto.exercisePublicId = exercise.publicId;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(dto, student.id);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with empty content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithEmptyContent() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");
        final var dto = new CommentDto();
        dto.content = "";
        dto.exercisePublicId = exercise.publicId;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(dto, student.id);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating comment with whitespace content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingCommentWithWhitespaceContent() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");
        final var dto = new CommentDto();
        dto.content = "   ";
        dto.exercisePublicId = exercise.publicId;

        assertThrows(ValidationException.class, () -> {
            this.commentService.createComment(dto, student.id);
        });
    }

    @Test
    @DisplayName("Should sanitize HTML tags from comment content")
    @TestTransaction
    void shouldSanitizeHtmlInComment() {
        final ExerciseViewDto exercise = this.createCommentableExercise();

        final var dto = new CommentDto();
        dto.content = "<script>alert(1)</script>safe text";
        dto.exercisePublicId = exercise.publicId;

        final CommentViewDto created = this.commentService.createComment(dto,
                this.userRepository.findByUsername("student1").id);

        assertNotNull(created);
        assertNotNull(created.content);
        assertFalse(created.content.contains("<script>"),
                "Sanitizer should strip <script>, got: " + created.content);
        assertTrue(created.content.contains("safe text"));
        verify(this.permissionService).requireCommentAdd();
    }

    @Test
    @DisplayName("Should reject comment on non-commentable exercise")
    @TestTransaction
    void shouldRejectCommentOnNonCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "teacher fixture must exist");
        final var exDto = new ExerciseDto();
        exDto.title = "noncommentable_" + UUID.randomUUID().toString().substring(0, 8);
        exDto.content = "x";
        exDto.userPublicId = teacher.publicId;
        exDto.published = true;
        exDto.commentable = false;
        final var ex = this.exerciseService.createExercise(exDto);

        final var dto = new CommentDto();
        dto.content = "hi";
        dto.exercisePublicId = ex.publicId;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
        verify(this.permissionService).requireCommentAdd();
    }

    @Test
    @DisplayName("Should find comment by id after creation")
    @TestTransaction
    void shouldFindCommentById() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "hello world";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto,
                this.userRepository.findByUsername("student1").id);

        final var found = this.commentService.findById(this.getCommentNumericId(created.publicId));

        assertTrue(found.isPresent());
        assertEquals("hello world", found.get().content);
    }

    @Test
    @DisplayName("Should list comments by exercise id")
    @TestTransaction
    void shouldListCommentsByExercise() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "comment one";
        dto.exercisePublicId = exercise.publicId;
        this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id);

        final var comments = this.commentService.findByExerciseId(exercise.id);

        assertEquals(1, comments.size());
        assertEquals("comment one", comments.get(0).content);
    }

    @Test
    @DisplayName("Should soft-delete comment as author")
    @TestTransaction
    void shouldSoftDeleteCommentAsAuthor() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "to delete";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto,
                this.userRepository.findByUsername("student1").id);
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");

        this.commentService.deleteComment(created.publicId, student.id, true);

        final var found = this.commentService.findById(this.getCommentNumericId(created.publicId));
        assertTrue(found.isPresent(), "Soft-deleted comment should still be findable");
        verify(this.permissionService).requireCommentAdd();
        verify(this.permissionService, never()).requireCommentDelete();
    }

    @Test
    @DisplayName("Should hide comment via moderation")
    @TestTransaction
    void shouldHideCommentViaModeration() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "needs hiding";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto,
                this.userRepository.findByUsername("student1").id);
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin);

        this.commentService.moderateComment(created.publicId, "HIDE", admin.id, "spam");

        final var found = this.commentService.findById(this.getCommentNumericId(created.publicId));
        assertTrue(found.isPresent());
        final var hidden = this.commentService.findByStatus(CommentStatus.HIDDEN);
        assertTrue(hidden.stream().anyMatch(c -> c.publicId.equals(created.publicId)));
        verify(this.permissionService).requireCommentAdd();
        verify(this.permissionService).requireCommentEdit();
    }
}
