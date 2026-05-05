package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
