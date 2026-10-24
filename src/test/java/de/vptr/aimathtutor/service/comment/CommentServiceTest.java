package de.vptr.aimathtutor.service.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.CommentFlagRepository;
import de.vptr.aimathtutor.repository.CommentRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
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

    // The mocked PermissionService accepts any credentials; the service's own checks are tested with it
    // (the guard itself is tested in PermissionServiceTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    private CommentService commentService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private CommentFlagRepository commentFlagRepository;

    @InjectMock
    private PermissionService permissionService;

    @InjectMock
    private CommentRateLimitService commentRateLimitService;

    @InjectMock
    private AuthService authService;

    @BeforeEach
    void setUpRateLimit() {
        doNothing().when(this.commentRateLimitService).checkRateLimit(anyLong());
        when(this.authService.getCurrentUserEntity()).thenReturn(this.userRepository.findByUsername("teacher"));
    }

    /** Makes {@code actor} the signed-in user and returns the service under test. */
    private CommentService as(final UserEntity actor) {
        doReturn(actor).when(this.authService).requireCurrentUser();
        return this.commentService;
    }

    private ExerciseViewDto createCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher);
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "ex_" + suffix;
        dto.content = "exercise content " + suffix;
        dto.published = true;
        dto.commentable = true;
        return this.exerciseService.createExercise(dto);
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
            this.as(student).createComment(dto);
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
            this.as(student).createComment(dto);
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
            this.as(student).createComment(dto);
        });
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should sanitize HTML tags from comment content")
    @TestTransaction
    void shouldSanitizeHtmlInComment() {
        final ExerciseViewDto exercise = this.createCommentableExercise();

        final var dto = new CommentDto();
        dto.content = "<script>alert(1)</script>safe text";
        dto.exercisePublicId = exercise.publicId;

        final CommentViewDto created = this.as(this.userRepository.findByUsername("student1")).createComment(dto);

        assertNotNull(created);
        assertNotNull(created.content);
        assertFalse(created.content.contains("<script>"), "Sanitizer should strip <script>, got: " + created.content);
        assertTrue(created.content.contains("safe text"));
        verify(this.permissionService).requireCommentAdd();
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should store the sessionId of the DTO on the created comment")
    @TestTransaction
    void shouldStoreSessionIdOnCreate() {
        final var dto = new CommentDto();
        dto.content = "comment with a session";
        dto.exercisePublicId = this.createCommentableExercise().publicId;
        dto.sessionId = "test-session-" + UUID.randomUUID();

        final CommentViewDto created = this.as(this.userRepository.findByUsername("student1")).createComment(dto);

        final var stored = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        assertEquals(dto.sessionId, stored.sessionId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should reject comment on non-commentable exercise")
    @TestTransaction
    void shouldRejectCommentOnNonCommentableExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "teacher fixture must exist");
        final var exDto = new ExerciseDto();
        exDto.title = "noncommentable_" + UUID.randomUUID().toString().substring(0, 8);
        exDto.content = "x";
        exDto.published = true;
        exDto.commentable = false;
        final var ex = this.exerciseService.createExercise(exDto);

        final var dto = new CommentDto();
        dto.content = "hi";
        dto.exercisePublicId = ex.publicId;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.as(this.userRepository.findByUsername("student1")).createComment(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
        verify(this.permissionService).requireCommentAdd();
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should list comments by exercise id")
    @TestTransaction
    void shouldListCommentsByExercise() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "comment one";
        dto.exercisePublicId = exercise.publicId;
        this.as(this.userRepository.findByUsername("student1")).createComment(dto);

        final var comments = this.commentService.findByExerciseId(CREDENTIALS, exercise.id);

        assertEquals(1, comments.size());
        assertEquals("comment one", comments.get(0).content);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should soft-delete comment as author")
    @TestTransaction
    void shouldSoftDeleteCommentAsAuthor() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "to delete";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(this.userRepository.findByUsername("student1")).createComment(dto);
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "student1 fixture must exist");

        this.as(student).deleteComment(created.publicId, true);

        assertTrue(this.commentRepository.findByPublicId(created.publicId).isPresent(),
                "Soft-deleted comment should still be findable");
        assertEquals(student.publicId,
                this.commentRepository.findByPublicId(created.publicId).orElseThrow().deletedBy.publicId);
        verify(this.permissionService).requireCommentAdd();
        verify(this.permissionService, never()).requireCommentDelete();
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should hide comment via moderation")
    @TestTransaction
    void shouldHideCommentViaModeration() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var dto = new CommentDto();
        dto.content = "needs hiding";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(this.userRepository.findByUsername("student1")).createComment(dto);
        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin);

        this.as(admin).moderateComment(created.publicId, "HIDE", "spam");

        final var hidden = this.commentService.findByStatus(CREDENTIALS, CommentStatus.HIDDEN);
        assertTrue(hidden.stream().anyMatch(c -> c.publicId.equals(created.publicId)));
        assertEquals(admin.publicId,
                this.commentRepository.findByPublicId(created.publicId).orElseThrow().moderator.publicId);
        verify(this.permissionService).requireCommentAdd();
        verify(this.permissionService).requireCommentEdit();
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("editComment updates content for the author")
    @TestTransaction
    void testEditComment_authorCanEdit() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "original content";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        final var editDto = new CommentDto();
        editDto.content = "edited content";
        final CommentViewDto edited = this.as(student).editComment(created.publicId, editDto);

        assertEquals("edited content", edited.content);

        final var persisted = this.commentRepository.findByPublicId(created.publicId);
        assertTrue(persisted.isPresent());
        assertEquals("edited content", persisted.get().content, "Edited content should be persisted in the database");
        verify(this.permissionService, never()).requireCommentEdit();
    }

    @SuppressWarnings("NullAway")
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
        final CommentViewDto created = this.as(student1).createComment(dto);

        doThrow(new WebApplicationException("Forbidden", Response.Status.FORBIDDEN)).when(this.permissionService)
                .requireCommentEdit();

        final var editDto = new CommentDto();
        editDto.content = "student2 sneaking in";

        final var ex = assertThrows(WebApplicationException.class,
                () -> this.as(student2).editComment(created.publicId, editDto));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());

        final var reloaded = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        assertEquals(created.content, reloaded.content, "Comment content should be unchanged after failed edit");
        verify(this.permissionService).requireCommentEdit();
    }

    @SuppressWarnings("NullAway")
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
            this.as(student).createComment(d);
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

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByUserId returns only comments by that user")
    @TestTransaction
    void testFindByUserId_filtered() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "my comment";
        dto.exercisePublicId = exercise.publicId;
        this.as(student).createComment(dto);

        final var results = this.commentService.findByUserId(CREDENTIALS, student.id);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> student.publicId.equals(c.authorPublicId)),
                "All comments should belong to student1");
    }

    @Test
    @DisplayName("searchComments returns empty list for blank query")
    @TestTransaction
    void testSearchComments_blank() {
        final var results = this.commentService.searchComments(CREDENTIALS, "");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Blank search should return empty list per service contract");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findFlaggedComments with minFlags=0 returns non-null list")
    @TestTransaction
    void testFindFlaggedComments_zeroMin() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "flaggable comment";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        final var entity = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        entity.flagsCount = 2;

        final var results = this.commentService.findFlaggedComments(CREDENTIALS, 0);
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(c -> created.publicId.equals(c.publicId)),
                "Flagged comment should appear in results");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByStatus VISIBLE returns visible comments")
    @TestTransaction
    void testFindByStatus_visible() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "visible comment";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        final var visible = this.commentService.findByStatus(CREDENTIALS, CommentStatus.VISIBLE);
        assertNotNull(visible);
        assertTrue(visible.stream().anyMatch(c -> c.publicId.equals(created.publicId)),
                "Newly created comment should have VISIBLE status");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("getAllComments returns non-null list that includes newly created comment")
    @TestTransaction
    void testGetAllComments() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "all comments test";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        final var all = this.commentService.getAllComments(CREDENTIALS);
        assertNotNull(all);
        assertTrue(all.stream().anyMatch(c -> c.publicId.equals(created.publicId)),
                "Newly created comment should appear in getAllComments");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("hard delete removes comment from repository")
    @TestTransaction
    void testHardDeleteComment() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "to hard delete";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        this.as(student).deleteComment(created.publicId, false);

        assertFalse(this.commentRepository.findByPublicId(created.publicId).isPresent(),
                "Hard-deleted comment should be removed from the repository");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findReplies returns replies to a parent comment")
    @TestTransaction
    void testFindReplies() {
        final ExerciseViewDto exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");

        final var parentDto = new CommentDto();
        parentDto.content = "parent comment";
        parentDto.exercisePublicId = exercise.publicId;
        final CommentViewDto parent = this.as(student).createComment(parentDto);

        final var replyDto = new CommentDto();
        replyDto.content = "reply comment";
        replyDto.exercisePublicId = exercise.publicId;
        replyDto.parentCommentPublicId = parent.publicId;
        final CommentViewDto reply = this.as(student).createComment(replyDto);

        final var replies = this.commentService.findReplies(parent.publicId);
        assertNotNull(replies);
        assertTrue(replies.stream().anyMatch(c -> c.publicId.equals(reply.publicId)),
                "Reply should appear in findReplies for the parent");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("createComment rejects unpublished exercise")
    @TestTransaction
    void testCreateComment_unpublishedExercise() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher);
        final var exDto = new ExerciseDto();
        exDto.title = "unpublished_" + UUID.randomUUID().toString().substring(0, 8);
        exDto.content = "x";
        exDto.published = false;
        exDto.commentable = true;
        final var ex = this.exerciseService.createExercise(exDto);

        final var dto = new CommentDto();
        dto.content = "test";
        dto.exercisePublicId = ex.publicId;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.as(this.userRepository.findByUsername("student1")).createComment(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("createComment rejects missing exercise by default")
    @TestTransaction
    void testCreateComment_missingExercisePublicId() {
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student);
        final var dto = new CommentDto();
        dto.content = "test";
        dto.exercisePublicId = null;

        assertThrows(ValidationException.class, () -> this.as(student).createComment(dto));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("createComment with non-existent parent publicId throws BAD_REQUEST")
    @TestTransaction
    void testCreateComment_nonexistentParent() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "reply to nowhere";
        dto.exercisePublicId = exercise.publicId;
        dto.parentCommentPublicId = "00000000000000000000000000";

        final var ex = assertThrows(WebApplicationException.class, () -> this.as(student).createComment(dto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("createComment with parent from different exercise throws BAD_REQUEST")
    @TestTransaction
    void testCreateComment_parentDifferentExercise() {
        final var exercise1 = this.createCommentableExercise();
        final var exercise2 = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");

        final var parentDto = new CommentDto();
        parentDto.content = "parent";
        parentDto.exercisePublicId = exercise1.publicId;
        final var parent = this.as(student).createComment(parentDto);

        final var replyDto = new CommentDto();
        replyDto.content = "reply";
        replyDto.exercisePublicId = exercise2.publicId;
        replyDto.parentCommentPublicId = parent.publicId;

        final var ex = assertThrows(WebApplicationException.class, () -> this.as(student).createComment(replyDto));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByDateRange returns empty list for null dates")
    @TestTransaction
    void testFindByDateRange_nullDates() {
        assertTrue(this.commentService.findByDateRange(CREDENTIALS, null, null).isEmpty());
    }

    @Test
    @DisplayName("findByDateRange returns empty list for invalid date strings")
    @TestTransaction
    void testFindByDateRange_invalidDates() {
        assertTrue(this.commentService.findByDateRange(CREDENTIALS, "not-a-date", "also-not-a-date").isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("flagComment calls through to flagging service")
    @TestTransaction
    void testFlagComment() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var otherStudent = this.userRepository.findByUsername("student2");
        assertNotNull(otherStudent, "student2 fixture must exist");
        final var dto = new CommentDto();
        dto.content = "flaggable";
        dto.exercisePublicId = exercise.publicId;
        final CommentViewDto created = this.as(student).createComment(dto);

        // Use a different user to flag (can't flag own comment)
        this.as(otherStudent).flagComment(created.publicId, "inappropriate");

        final var flagged = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        assertEquals(1, flagged.flagsCount);
        assertTrue(this.commentFlagRepository.hasUserFlaggedComment(flagged.id, otherStudent.id));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("searchComments(null) returns empty list")
    void testSearchComments_null() {
        assertTrue(this.commentService.searchComments(CREDENTIALS, null).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByDateRange returns results for valid date strings")
    @TestTransaction
    void testFindByDateRange_validDates() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "date range test";
        dto.exercisePublicId = exercise.publicId;
        this.as(student).createComment(dto);

        final String today = LocalDate.now(ZoneId.systemDefault()).toString();
        final String tomorrow = LocalDate.now(ZoneId.systemDefault()).plusDays(1).toString();
        final var results = this.commentService.findByDateRange(CREDENTIALS, today, tomorrow);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("listCommentsByExercise with non-null parentPublicId returns replies")
    @TestTransaction
    void testListCommentsByExercise_withParent() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");

        final var parentDto = new CommentDto();
        parentDto.content = "parent";
        parentDto.exercisePublicId = exercise.publicId;
        final var parent = this.as(student).createComment(parentDto);

        final var replyDto = new CommentDto();
        replyDto.content = "a reply";
        replyDto.exercisePublicId = exercise.publicId;
        replyDto.parentCommentPublicId = parent.publicId;
        this.as(student).createComment(replyDto);

        final var replies = this.commentService.listCommentsByExercise(exercise.id, 0, 10, parent.publicId);
        assertNotNull(replies);
        assertFalse(replies.isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("editComment and deleteComment throw NOT_FOUND for non-existent publicId")
    @TestTransaction
    void testEditAndDeleteComment_notFound() {
        final var student = this.userRepository.findByUsername("student1");
        final var editDto = new CommentDto();
        editDto.content = "edited content";
        final var editEx = assertThrows(WebApplicationException.class,
                () -> this.as(student).editComment("00000000000000000000000000", editDto));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), editEx.getResponse().getStatus());
        final var deleteEx = assertThrows(WebApplicationException.class,
                () -> this.as(student).deleteComment("00000000000000000000000000", false));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteEx.getResponse().getStatus());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("every comment write refuses an ended session and changes nothing")
    @TestTransaction
    void testEndedSession_refusesEveryWrite() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student1");
        final var dto = new CommentDto();
        dto.content = "comment to keep";
        dto.exercisePublicId = exercise.publicId;
        final var created = this.as(student).createComment(dto);

        final var editDto = new CommentDto();
        editDto.content = "new content";
        when(this.authService.requireCurrentUser())
                .thenThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE));
        final Executable[] writes = { () -> this.commentService.createComment(dto),
                () -> this.commentService.editComment(created.publicId, editDto),
                () -> this.commentService.deleteComment(created.publicId, true),
                () -> this.commentService.flagComment(created.publicId, "spam"),
                () -> this.commentService.moderateComment(created.publicId, "HIDE", "spam") };
        for (final Executable write : writes) {
            final var ex = assertThrows(ValidationException.class, write);
            assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        }

        final var reloaded = this.commentRepository.findByPublicId(created.publicId).orElseThrow();
        assertEquals("comment to keep", reloaded.content, "Comment content must be unchanged");
        assertEquals(CommentStatus.VISIBLE, reloaded.status, "Comment must be neither deleted nor hidden");
        assertEquals(0, reloaded.flagsCount, "Comment must not be flagged");
        assertEquals(1, this.commentService.findByExerciseId(CREDENTIALS, exercise.id).size(),
                "No second comment may be stored");
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("createComment persists the session user as author and rate-limits on that user")
    @TestTransaction
    void testCreateComment_authorIsSessionUser() {
        final var exercise = this.createCommentableExercise();
        final var student = this.userRepository.findByUsername("student2");
        final var dto = new CommentDto();
        dto.content = "authored by the session user";
        dto.exercisePublicId = exercise.publicId;

        final var created = this.as(student).createComment(dto);

        assertEquals(student.publicId,
                this.commentRepository.findByPublicId(created.publicId).orElseThrow().user.publicId);
        verify(this.commentRateLimitService).checkRateLimit(student.id);
    }

}
