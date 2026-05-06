package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class ExerciseServiceTest {

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private LessonService lessonService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private EntityManager em;

    @InjectMock
    private PermissionService permissionService;

    private String teacherPublicId() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher user should exist");
        return teacher.publicId;
    }

    private ExerciseDto buildDto(final String userPublicId, final boolean published) {
        final var dto = new ExerciseDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.title = "Exercise " + suffix;
        dto.content = "Content for " + suffix;
        dto.userPublicId = userPublicId;
        dto.published = published;
        dto.commentable = false;
        return dto;
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null title")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullTitle() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = null;
        exerciseDto.content = "Content";
        exerciseDto.userPublicId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

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
        exerciseDto.userPublicId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

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
        exerciseDto.userPublicId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

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
        exerciseDto.userPublicId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating exercise with null userId")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingExerciseWithNullUserId() {
        final ExerciseDto exerciseDto = new ExerciseDto();
        exerciseDto.title = "Title";
        exerciseDto.content = "Content";
        exerciseDto.userPublicId = null;

        assertThrows(ValidationException.class, () -> {
            this.exerciseService.createExercise(exerciseDto);
        });
    }

    @Test
    @DisplayName("Should reject Graspable Math exercise without target expression")
    @Transactional
    void shouldRejectGraspableExerciseWithoutTarget() {
        final ExerciseDto dto = this.buildDto(this.teacherPublicId(), false);
        dto.graspableEnabled = Boolean.TRUE;
        dto.graspableTargetExpression = null;

        assertThrows(ValidationException.class, () -> this.exerciseService.createExercise(dto));
    }

    @Test
    @DisplayName("Should create exercise with valid data")
    @TestTransaction
    void shouldCreateExerciseWithValidData() {
        final ExerciseDto dto = this.buildDto(this.teacherPublicId(), true);

        final ExerciseViewDto created = this.exerciseService.createExercise(dto);

        assertNotNull(created.publicId);
        assertEquals(dto.title, created.title);
        assertEquals(dto.content, created.content);
        assertEquals(this.teacherPublicId(), created.userPublicId);
        assertTrue(created.published);
    }

    @Test
    @DisplayName("Should find exercise by id and route through completion enrichment")
    @TestTransaction
    void shouldFindExerciseById() {
        final ExerciseViewDto created = this.exerciseService
                .createExercise(this.buildDto(this.teacherPublicId(), true));

        final var found = this.exerciseService.findById(created.id);

        assertTrue(found.isPresent());
        assertEquals(created.publicId, found.get().publicId);
        assertEquals(created.title, found.get().title);
    }

    @Test
    @DisplayName("Should find only published exercises")
    @TestTransaction
    void shouldFindPublishedExercisesOnly() {
        final var teacherId = this.teacherPublicId();
        final ExerciseViewDto pub = this.exerciseService.createExercise(this.buildDto(teacherId, true));
        final ExerciseViewDto draft = this.exerciseService.createExercise(this.buildDto(teacherId, false));

        final var published = this.exerciseService.findPublishedExercises();

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

        final ExerciseDto dto = this.buildDto(this.teacherPublicId(), true);
        dto.lessonPublicId = lesson.publicId;

        final ExerciseViewDto created = this.exerciseService.createExercise(dto);

        assertEquals(lesson.publicId, created.lessonPublicId);
        final var lessonEntityForLookup = this.em.createQuery(
                "SELECT l FROM LessonEntity l WHERE l.publicId = :p", LessonEntity.class)
                .setParameter("p", lesson.publicId)
                .getSingleResult();
        final var exercises = this.exerciseService.findByLessonId(lessonEntityForLookup.id);
        assertEquals(1, exercises.size());
        assertEquals(created.publicId, exercises.get(0).publicId);
    }

    @Test
    @DisplayName("Should find exercises by user id")
    @TestTransaction
    void shouldFindExercisesByUserId() {
        final var teacher = this.userRepository.findByUsername("teacher");
        assertNotNull(teacher, "Seeded teacher user should exist");
        final ExerciseViewDto created = this.exerciseService.createExercise(this.buildDto(teacher.publicId, true));

        final var byUser = this.exerciseService.findByUserId(teacher.id);

        assertTrue(byUser.stream().anyMatch(e -> e.publicId.equals(created.publicId)));
    }

    @Test
    @DisplayName("Should delete exercise by id")
    @TestTransaction
    void shouldDeleteExercise() {
        final ExerciseViewDto created = this.exerciseService
                .createExercise(this.buildDto(this.teacherPublicId(), true));

        final boolean deleted = this.exerciseService.deleteExercise(created.publicId);

        assertTrue(deleted);
        assertTrue(this.exerciseService.findById(created.id).isEmpty());
    }

    @Test
    @DisplayName("updateExercise replaces all fields")
    @TestTransaction
    void testUpdateExercise_replacesFields() {
        final ExerciseViewDto created = this.exerciseService
                .createExercise(this.buildDto(this.teacherPublicId(), false));

        final ExerciseDto update = new ExerciseDto();
        update.title = "Updated Title";
        update.content = "Updated content";
        update.userPublicId = this.teacherPublicId();
        update.published = true;
        update.commentable = true;

        final ExerciseViewDto updated = this.exerciseService.updateExercise(created.publicId, update);

        assertEquals("Updated Title", updated.title);
        assertEquals("Updated content", updated.content);
        assertTrue(updated.published);
        assertTrue(updated.commentable);
    }

    @Test
    @DisplayName("patchExercise updates only the provided field")
    @TestTransaction
    void testPatchExercise_updatesProvidedField() {
        final ExerciseViewDto created = this.exerciseService
                .createExercise(this.buildDto(this.teacherPublicId(), false));

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
        this.exerciseService.createExercise(this.buildDto(this.teacherPublicId(), true));
        final var results = this.exerciseService.searchExercises("");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Blank query should return all exercises");
    }

    @Test
    @DisplayName("searchExercises with matching term returns results")
    @TestTransaction
    void testSearchExercises_match() {
        final ExerciseDto dto = this.buildDto(this.teacherPublicId(), true);
        final String uniqueTitle = "UniqueSearchableTitle_" + UUID.randomUUID().toString().substring(0, 8);
        dto.title = uniqueTitle;
        this.exerciseService.createExercise(dto);

        final var results = this.exerciseService.searchExercises(uniqueTitle);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> uniqueTitle.equals(e.title)));
    }

    @Test
    @DisplayName("searchExercises with non-matching term returns empty list")
    @TestTransaction
    void testSearchExercises_noMatch() {
        final var results = this.exerciseService.searchExercises("zzz_nonexistent_xyz_9999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findGraspableMathExercises returns only graspable exercises")
    @TestTransaction
    void testFindGraspableMathExercises() {
        final var results = this.exerciseService.findGraspableMathExercises();
        assertNotNull(results);
        assertTrue(results.stream().allMatch(e -> Boolean.TRUE.equals(e.graspableEnabled)),
                "All returned exercises should have graspableEnabled=true");
    }

    @Test
    @DisplayName("findPublishedExercisesByLessonMap returns a non-null map")
    @TestTransaction
    void testFindPublishedExercisesByLessonMap() {
        final var map = this.exerciseService.findPublishedExercisesByLessonMap();
        assertNotNull(map);
    }

    @Test
    @DisplayName("findByDateRange with null dates returns empty list")
    @TestTransaction
    void testFindByDateRange_nullDates() {
        final var results = this.exerciseService.findByDateRange(null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findByDateRange with today's range includes recently created exercise")
    @TestTransaction
    void testFindByDateRange_today() {
        this.exerciseService.createExercise(this.buildDto(this.teacherPublicId(), true));
        final String today = LocalDate.now().toString();
        final var results = this.exerciseService.findByDateRange(today, today);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Exercise created today should be in today's date range");
    }
}
