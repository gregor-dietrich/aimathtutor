package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import de.vptr.aimathtutor.service.comment.CommentRateLimitService;
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

    @InjectMock
    private CommentRateLimitService commentRateLimitService;

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
        return this.commentRepository.findByPublicId(publicId).map(c -> c.id)
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

        final CommentViewDto created =
                this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id);

        assertNotNull(created);
        assertNotNull(created.content);
        assertFalse(created.content.contains("<script>"), "Sanitizer should strip <script>, got: " + created.content);
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
        final CommentViewDto created =
                this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id);

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
        final CommentViewDto created =
                this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id);
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
        final CommentViewDto created =
                this.commentService.createComment(dto, this.userRepository.findByUsername("student1").id);
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

    @Test
    @DisplayName("editComment updates content for the author")
    @TestTransaction
    void testEditComment_authorCanEdit() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "original content";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        final var editDto = new CommentDto();
        editDto.content = "edited content";
        final CommentViewDto edited = this.commentService.editComment(created.publicId, editDto, student.id);

        assertEquals("edited content", edited.content);

        final var persisted = this.commentRepository.findByPublicId(created.publicId);
        assertTrue(persisted.isPresent());
        assertEquals("edited content", persisted.get().content, "Edited content should be persisted in the database");
        verify(this.permissionService, never()).requireCommentEdit();
    }

    @Test
    @DisplayName("editComment calls requireCommentEdit for non-author")
    @TestTransaction
    void testEditComment_nonAuthorRequiresPermission() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student1 = this.userRepository.findByUsername("student1");
        final var student2 = this.userRepository.findByUsername("student2");
        final var dto = new CommentDto();
        dto.content = "student1's comment";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student1.id);

        doThrow(new WebApplicationException("Forbidden", Response.Status.FORBIDDEN)).when(this.permissionService)
                .requireCommentEdit();

        final var editDto = new CommentDto();
        editDto.content = "student2 sneaking in";

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.commentService.editComment(created.publicId, editDto, student2.id));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());

        final var reloaded = this.commentService.findById(this.getCommentNumericId(created.publicId));
        assertTrue(reloaded.isPresent());
        assertEquals(created.content, reloaded.get().content, "Comment content should be unchanged after failed edit");
        verify(this.permissionService).requireCommentEdit();
    }

    @Test
    @DisplayName("listCommentsByExercise returns paged results")
    @TestTransaction
    void testListCommentsByExercise_paged() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");

        for (int i = 0; i < 3; i++) {
            final var d = new CommentDto();
            d.content = "comment " + i;
            d.exercisePublicId = exercise.publicId;
            this.commentService.createComment(d, student.id);
        }

        final var page1 = this.commentService.listCommentsByExercise(exercise.id, 0, 2, null);
        assertNotNull(page1);
        assertEquals(2, page1.size(), "First page should have exactly 2 comments");

        final var page2 = this.commentService.listCommentsByExercise(exercise.id, 1, 2, null);
        assertNotNull(page2);
        assertEquals(1, page2.size(), "Second page should have the remaining 1 comment");

        for (final CommentViewDto c1 : page1) {
            for (final CommentViewDto c2 : page2) {
                assertFalse(c1.publicId.equals(c2.publicId), "Pages should contain distinct comments");
            }
        }
    }

    @Test
    @DisplayName("findRecentComments returns at most limit number of comments")
    @TestTransaction
    void testFindRecentComments_limitRespected() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        for (int i = 0; i < 5; i++) {
            final var d = new CommentDto();
            d.content = "recent " + i;
            d.exercisePublicId = exercise.publicId;
            this.commentService.createComment(d, student.id);
        }

        final var recent = this.commentService.findRecentComments(3);
        assertNotNull(recent);
        assertEquals(3, recent.size(), "findRecentComments(3) should return exactly 3 comments");
        assertEquals("recent 4", recent.get(0).content, "Most recent comment should be first");
        assertEquals("recent 3", recent.get(1).content, "Second most recent should be second");
        assertEquals("recent 2", recent.get(2).content, "Third most recent should be third");
    }

    @Test
    @DisplayName("findByUserId returns only comments by that user")
    @TestTransaction
    void testFindByUserId_filtered() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "my comment";
        dto.exercisePublicId = exercise.publicId;
        this.commentService.createComment(dto, student.id);

        final var results = this.commentService.findByUserId(student.id);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> student.publicId.equals(c.authorPublicId)),
                "All comments should belong to student1");
    }

    @Test
    @DisplayName("searchComments returns empty list for blank query")
    @TestTransaction
    void testSearchComments_blank() {
        final var results = this.commentService.searchComments("");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Blank search should return empty list per service contract");
    }

    @Test
    @DisplayName("findFlaggedComments with minFlags=0 returns non-null list")
    @TestTransaction
    void testFindFlaggedComments_zeroMin() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "flaggable comment";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        final var entity = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        entity.flagsCount = 2;

        final var results = this.commentService.findFlaggedComments(0);
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(c -> created.publicId.equals(c.publicId)),
                "Flagged comment should appear in results");
    }

    @Test
    @DisplayName("findByStatus VISIBLE returns visible comments")
    @TestTransaction
    void testFindByStatus_visible() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "visible comment";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        final var visible = this.commentService.findByStatus(CommentStatus.VISIBLE);
        assertNotNull(visible);
        assertTrue(visible.stream().anyMatch(c -> c.publicId.equals(created.publicId)),
                "Newly created comment should have VISIBLE status");
    }

    @Test
    @DisplayName("getAllComments returns non-null list that includes newly created comment")
    @TestTransaction
    void testGetAllComments() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "all comments test";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        final var all = this.commentService.getAllComments();
        assertNotNull(all);
        assertTrue(all.stream().anyMatch(c -> c.publicId.equals(created.publicId)),
                "Newly created comment should appear in getAllComments");
    }

    @Test
    @DisplayName("findByPublicId returns empty for unknown publicId")
    @TestTransaction
    void testFindByPublicId_notFound() {
        final var result = this.commentService.findByPublicId("00000000000000000000000000");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("hard delete removes comment from repository")
    @TestTransaction
    void testHardDeleteComment() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "to hard delete";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        this.commentService.deleteComment(created.publicId, student.id, false);

        assertFalse(this.commentRepository.findByPublicId(created.publicId).isPresent(),
                "Hard-deleted comment should be removed from the repository");
    }

    @Test
    @DisplayName("findReplies returns replies to a parent comment")
    @TestTransaction
    void testFindReplies() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");

        final var parentDto = new CommentDto();
        parentDto.content = "parent comment";
        parentDto.exercisePublicId = exercise.publicId;
        final CommentViewDto parent = this.commentService.createComment(parentDto, student.id);

        final var replyDto = new CommentDto();
        replyDto.content = "reply comment";
        replyDto.exercisePublicId = exercise.publicId;
        replyDto.parentCommentPublicId = parent.publicId;
        final CommentViewDto reply = this.commentService.createComment(replyDto, student.id);

        final var replies = this.commentService.findReplies(parent.publicId);
        assertNotNull(replies);
        assertTrue(replies.stream().anyMatch(c -> c.publicId.equals(reply.publicId)),
                "Reply should appear in findReplies for the parent");
    }

    @Test
    @DisplayName("listCommentsBySession returns comments with the given sessionId")
    @TestTransaction
    void testListCommentsBySession() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final String sessionId = "test-session-" + UUID.randomUUID().toString().substring(0, 8);

        final var dto = new CommentDto();
        dto.content = "session comment";
        dto.exercisePublicId = exercise.publicId;
        dto.sessionId = sessionId;
        final CommentViewDto created = this.commentService.createComment(dto, student.id);

        final var results = this.commentService.listCommentsBySession(sessionId);
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(c -> c.publicId.equals(created.publicId)),
                "Comment with matching sessionId should be returned");
    }
}
