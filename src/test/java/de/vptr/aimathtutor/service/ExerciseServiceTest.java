package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class ExerciseServiceTest {

    // The mocked PermissionService accepts any credentials; the service's own checks are tested with it
    // (the guard itself is tested in PermissionServiceTest)
    private static final SessionCredentials CREDENTIALS =
            new SessionCredentials("admin-public-id", "stamp", "session-key");

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private LessonService lessonService;

    @InjectMock
    private PermissionService permissionService;

    @InjectMock
    private AuthService authService;

    @Inject
    private UserRepository userRepository;

    @SuppressWarnings("NullAway")
    @BeforeEach
    void setUpMocks() {
        Mockito.doNothing().when(this.permissionService).requireExerciseAdd();
        Mockito.doNothing().when(this.permissionService).requireExerciseEdit();
        Mockito.doNothing().when(this.permissionService).requireExerciseDelete();

        final var teacher = this.userRepository.findByUsername("teacher");
        Mockito.when(this.authService.getUserId()).thenReturn(teacher.id);
        Mockito.when(this.authService.getCurrentUserEntity()).thenReturn(teacher);
    }

    @SuppressWarnings("NullAway")
    private String teacherPublicId() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher user should exist");
        return teacher.publicId;
    }

    private ExerciseDto buildDto(final boolean published) {
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        return new ExerciseDto("Exercise " + suffix, "Content for " + suffix, null, published, false);
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null title")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullTitle() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = null;
        exerciseDto.content = "Content";

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with empty title")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithEmptyTitle() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "";
        exerciseDto.content = "Content";

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullContent() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "Title";
        exerciseDto.content = null;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with empty content")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithEmptyContent() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "Title";
        exerciseDto.content = "";

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise when not logged in")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWhenNotLoggedIn() {
        Mockito.when(this.authService.getCurrentUserEntity()).thenReturn(null);
        final ExerciseDto exerciseDto = new ExerciseDto("Title", "Content", null, true, true);

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should reject Graspable Math exercise without target expression")
    @Transactional
    void shouldRejectGraspableExerciseWithoutTarget() {
        final ExerciseDto dto = this.buildDto(false);
        dto.graspableEnabled = Boolean.TRUE;
        dto.graspableTargetExpression = null;

        assertThrows(ValidationException.class, () -> this.exerciseService.createExercise(dto));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should create exercise with valid data")
    @TestTransaction
    void shouldCreateExerciseWithValidData() {
        final ExerciseDto dto = this.buildDto(true);

        final ExerciseViewDto created = this.exerciseService.createExercise(dto);

        assertNotNull(created.publicId);
        assertEquals(dto.title, created.title);
        assertEquals(dto.content, created.content);
        assertEquals(this.teacherPublicId(), created.userPublicId);
        assertTrue(created.published);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should find only published exercises")
    @TestTransaction
    void shouldFindPublishedExercisesOnly() {
        final ExerciseViewDto pub = this.exerciseService.createExercise(this.buildDto(true));
        final ExerciseViewDto draft = this.exerciseService.createExercise(this.buildDto(false));

        final var published = this.exerciseService.findPublishedExercises(CREDENTIALS);

        assertTrue(published.stream().anyMatch(e -> e.publicId.equals(pub.publicId)));
        assertFalse(published.stream().anyMatch(e -> e.publicId.equals(draft.publicId)));
    }

    @Test
    @DisplayName("Should attach exercise to lesson")
    @TestTransaction
    void shouldAttachExerciseToLesson() {
        final var lessonEntity = new LessonEntity();
        lessonEntity.name = "lesson_" + UUID.randomUUID().toString().substring(0, 8);
        final LessonViewDto lesson = this.lessonService.createLesson(lessonEntity);

        final ExerciseDto dto = this.buildDto(true);
        dto.lessonPublicId = lesson.publicId;

        final ExerciseViewDto created = this.exerciseService.createExercise(dto);

        assertEquals(lesson.publicId, created.lessonPublicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should find exercises by user id")
    @TestTransaction
    void shouldFindExercisesByUserId() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher user should exist");
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(true));

        final var byUser = this.exerciseService.findByUserId(CREDENTIALS, teacher.id);

        assertTrue(byUser.stream().anyMatch(e -> e.publicId.equals(created.publicId)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Should delete exercise by id")
    @TestTransaction
    void shouldDeleteExercise() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(true));

        final boolean deleted = this.exerciseService.deleteExercise(created.publicId);

        assertTrue(deleted);
        assertTrue(this.exerciseService.findByPublicId(created.publicId).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateExercise replaces all fields")
    @TestTransaction
    void testUpdateExercise_replacesFields() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto update = new ExerciseDto();
        update.title = "Updated Title";
        update.content = "Updated content";
        update.published = true;
        update.commentable = true;

        final ExerciseViewDto updated = this.exerciseService.updateExercise(created.publicId, update);

        assertEquals("Updated Title", updated.title);
        assertEquals("Updated content", updated.content);
        assertTrue(updated.published);
        assertTrue(updated.commentable);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchExercise updates only the provided field")
    @TestTransaction
    void testPatchExercise_updatesProvidedField() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto patch = new ExerciseDto();
        patch.published = true;

        final ExerciseViewDto patched = this.exerciseService.patchExercise(created.publicId, patch);

        assertEquals(created.title, patched.title, "Title should be unchanged after patch");
        assertTrue(patched.published);
    }

    @Test
    @DisplayName("searchExercises with blank query returns all exercises")
    @TestTransaction
    void testSearchExercises_blank() {
        this.exerciseService.createExercise(this.buildDto(true));
        final var results = this.exerciseService.searchExercises(CREDENTIALS, "");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Blank query should return all exercises");
    }

    @Test
    @DisplayName("searchExercises with matching term returns results")
    @TestTransaction
    void testSearchExercises_match() {
        final ExerciseDto dto = this.buildDto(true);
        final String uniqueTitle = "UniqueSearchableTitle_" + UUID.randomUUID().toString().substring(0, 8);
        dto.title = uniqueTitle;
        this.exerciseService.createExercise(dto);

        final var results = this.exerciseService.searchExercises(CREDENTIALS, uniqueTitle);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> uniqueTitle.equals(e.title)));
    }

    @Test
    @DisplayName("searchExercises with non-matching term returns empty list")
    @TestTransaction
    void testSearchExercises_noMatch() {
        final var results = this.exerciseService.searchExercises(CREDENTIALS, "zzz_nonexistent_xyz_9999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findPublishedExercisesByLessonMap returns a non-null map")
    @TestTransaction
    void testFindPublishedExercisesByLessonMap() {
        final var map = this.exerciseService.findPublishedExercisesByLessonMap();
        assertNotNull(map);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByDateRange with null dates returns empty list")
    @TestTransaction
    void testFindByDateRange_nullDates() {
        final var results = this.exerciseService.findByDateRange(CREDENTIALS, null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByDateRange with today's range includes recently created exercise")
    @TestTransaction
    void testFindByDateRange_today() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(true));
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();
        final var results = this.exerciseService.findByDateRange(CREDENTIALS, today, today);
        assertNotNull(results);
        assertTrue(results.stream().anyMatch(e -> created.publicId.equals(e.publicId)),
                "Exercise created today should be in today's date range");
    }

    @Test
    @DisplayName("findByPublicId returns empty for unknown publicId")
    @TestTransaction
    void testFindByPublicId_notFound() {
        final var result = this.exerciseService.findByPublicId("00000000000000000000000000");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("createExercise throws ValidationException for unknown lessonPublicId")
    @TestTransaction
    void createExercise_lessonNotFound_throwsValidationException() {
        final ExerciseDto dto = this.buildDto(true);
        dto.lessonPublicId = "00000000000000000000nonexistent";
        assertThrows(ValidationException.class, () -> this.exerciseService.createExercise(dto));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateExercise throws ValidationException for graspable exercise without target expression")
    @TestTransaction
    void updateExercise_graspableWithoutTarget_throwsValidationException() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto update = new ExerciseDto();
        update.title = "Updated Title";
        update.content = "Updated content";
        update.graspableEnabled = Boolean.TRUE;
        update.graspableTargetExpression = null;

        assertThrows(ValidationException.class, () -> this.exerciseService.updateExercise(created.publicId, update));
    }

    @Test
    @DisplayName("patchExercise throws NOT_FOUND for unknown publicId")
    @TestTransaction
    void patchExercise_notFound_throwsNotFound() {
        final ExerciseDto patch = new ExerciseDto();
        patch.published = true;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.exerciseService.patchExercise("00000000000000000000nonexistent", patch));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("findByDateRange with invalid date format returns empty list")
    @TestTransaction
    void findByDateRange_invalidFormat_returnsEmpty() {
        final var results = this.exerciseService.findByDateRange(CREDENTIALS, "not-a-date", "also-not-a-date");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchExercise updates graspable fields when provided")
    @TestTransaction
    void patchExercise_updatesGraspableFields() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto patch = new ExerciseDto();
        patch.graspableEnabled = Boolean.TRUE;
        patch.graspableTargetExpression = "x = 5";
        patch.graspableInitialExpression = "x + 1 = 6";

        final ExerciseViewDto patched = this.exerciseService.patchExercise(created.publicId, patch);

        assertEquals(true, patched.graspableEnabled);
        assertEquals("x = 5", patched.graspableTargetExpression);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchExercise attaches lesson when lessonPublicId provided")
    @TestTransaction
    void patchExercise_attachesLesson() {
        final var lessonEntity = new LessonEntity();
        lessonEntity.name = "patch_lesson_" + UUID.randomUUID().toString().substring(0, 8);
        final LessonViewDto lesson = this.lessonService.createLesson(lessonEntity);
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(true));

        final ExerciseDto patch = new ExerciseDto();
        patch.lessonPublicId = lesson.publicId;
        final ExerciseViewDto patched = this.exerciseService.patchExercise(created.publicId, patch);

        assertEquals(lesson.publicId, patched.lessonPublicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchExercise updates title and content when provided as non-blank values")
    @TestTransaction
    void testPatchExercise_updatesTitleAndContent() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto patch = new ExerciseDto();
        patch.title = "Patched Title";
        patch.content = "Patched content text";

        final ExerciseViewDto patched = this.exerciseService.patchExercise(created.publicId, patch);

        assertEquals("Patched Title", patched.title);
        assertEquals("Patched content text", patched.content);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("findByDateRange with null endDate returns empty list")
    @TestTransaction
    void testFindByDateRange_nullEndDate_returnsEmpty() {
        final var results = this.exerciseService.findByDateRange(CREDENTIALS, "2024-01-01", null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("createExercise with blank graspableTargetExpression throws ValidationException")
    @TestTransaction
    void createExercise_graspableWithBlankTarget_throwsValidationException() {
        final ExerciseDto dto = this.buildDto(false);
        dto.graspableEnabled = Boolean.TRUE;
        dto.graspableTargetExpression = "   ";
        assertThrows(ValidationException.class, () -> this.exerciseService.createExercise(dto));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateExercise with valid lessonPublicId attaches the lesson")
    @TestTransaction
    void testUpdateExercise_withValidLesson_attachesLesson() {
        final var lessonEntity = new LessonEntity();
        lessonEntity.name = "upd_lesson_" + UUID.randomUUID().toString().substring(0, 8);
        final LessonViewDto lesson = this.lessonService.createLesson(lessonEntity);
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto update = new ExerciseDto();
        update.title = "Updated";
        update.content = "Updated content";
        update.lessonPublicId = lesson.publicId;
        final ExerciseViewDto updated = this.exerciseService.updateExercise(created.publicId, update);

        assertEquals(lesson.publicId, updated.lessonPublicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateExercise throws ValidationException for unknown lessonPublicId")
    @TestTransaction
    void testUpdateExercise_withInvalidLesson_throwsValidationException() {
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(false));

        final ExerciseDto update = new ExerciseDto();
        update.title = "Title";
        update.content = "Content";
        update.lessonPublicId = "00000000000000000000nonexistent";

        assertThrows(ValidationException.class, () -> this.exerciseService.updateExercise(created.publicId, update));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchExercise with blank lessonPublicId keeps the existing lesson (clearIfMissing=false)")
    @TestTransaction
    void testPatchExercise_withBlankLessonPublicId_keepsExistingLesson() {
        final var lessonEntity = new LessonEntity();
        lessonEntity.name = "pk_lesson_" + UUID.randomUUID().toString().substring(0, 8);
        final LessonViewDto lesson = this.lessonService.createLesson(lessonEntity);

        final ExerciseDto dto = this.buildDto(true);
        dto.lessonPublicId = lesson.publicId;
        final ExerciseViewDto created = this.exerciseService.createExercise(dto);

        final ExerciseDto patch = new ExerciseDto();
        patch.lessonPublicId = "";
        final ExerciseViewDto patched = this.exerciseService.patchExercise(created.publicId, patch);

        assertEquals(lesson.publicId, patched.lessonPublicId);
    }

}
