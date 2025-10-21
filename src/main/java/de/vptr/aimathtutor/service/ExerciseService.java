package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ExerciseService {

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    /**
     * Enriches an ExerciseViewDto with completion data for the current user.
     * If the user is not authenticated, completion fields remain null.
     * 
     * @param dto The exercise DTO to enrich
     * @return The enriched DTO
     */
    private ExerciseViewDto enrichWithCompletionData(final ExerciseViewDto dto) {
        if (dto == null) {
            return dto;
        }

        try {
            final Long currentUserId = this.authService.getUserId();
            if (currentUserId == null) {
                // User not authenticated, leave completion data as null
                return dto;
            }

            // Get all completed sessions for this user on this exercise
            final var sessions = this.analyticsService.getSessionsByExercise(dto.id);
            final var userSessions = sessions.stream()
                    .filter(s -> currentUserId.equals(s.userId))
                    .toList();

            // Check if any session was completed
            final var completedSessions = userSessions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.completed))
                    .toList();

            dto.userCompleted = !completedSessions.isEmpty();
            dto.userCompletionCount = completedSessions.size();

        } catch (final Exception e) {
            // If there's any error enriching the DTO, just leave completion data as null
            // This ensures we don't break the exercise loading functionality
        }

        return dto;
    }

    public List<ExerciseViewDto> getAllExercises() {
        return ExerciseEntity.find("ORDER BY created DESC").list().stream()
                .map(entity -> new ExerciseViewDto((ExerciseEntity) entity))
                .toList();
    }

    public Optional<ExerciseViewDto> findById(final Long id) {
        return ExerciseEntity.findByIdOptional(id)
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto((ExerciseEntity) entity)));
    }

    public List<ExerciseViewDto> findPublishedExercises() {
        return ExerciseEntity.find("published = true ORDER BY created DESC").list().stream()
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto((ExerciseEntity) entity)))
                .toList();
    }

    public List<ExerciseViewDto> findByUserId(final Long userId) {
        return ExerciseEntity.find("user.id = ?1 ORDER BY created DESC", userId).list().stream()
                .map(entity -> new ExerciseViewDto((ExerciseEntity) entity))
                .toList();
    }

    public List<ExerciseViewDto> findByLessonId(final Long lessonId) {
        return ExerciseEntity.find("lesson.id = ?1 ORDER BY created DESC", lessonId).list().stream()
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto((ExerciseEntity) entity)))
                .toList();
    }

    public List<ExerciseViewDto> findGraspableMathExercises() {
        return ExerciseEntity.find("graspableEnabled = true AND published = true ORDER BY created DESC").list().stream()
                .map(entity -> new ExerciseViewDto((ExerciseEntity) entity))
                .toList();
    }

    public List<ExerciseViewDto> findGraspableMathExercisesByLesson(final Long lessonId) {
        return ExerciseEntity
                .find("graspableEnabled = true AND published = true AND lesson.id = ?1 ORDER BY created DESC",
                        lessonId)
                .list().stream()
                .map(entity -> new ExerciseViewDto((ExerciseEntity) entity))
                .toList();
    }

    @Transactional
    public ExerciseViewDto createExercise(final ExerciseDto exerciseDto) {
        if (exerciseDto.title == null || exerciseDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for creating an exercise");
        }
        if (exerciseDto.content == null || exerciseDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating an exercise");
        }
        if (exerciseDto.userId == null) {
            throw new ValidationException("User ID is required for creating an exercise");
        }

        final ExerciseEntity exercise = new ExerciseEntity();
        exercise.title = exerciseDto.title;
        exercise.content = exerciseDto.content;
        exercise.published = exerciseDto.published != null ? exerciseDto.published : false;
        exercise.commentable = exerciseDto.commentable != null ? exerciseDto.commentable : false;
        exercise.created = LocalDateTime.now();
        exercise.lastEdit = exercise.created;

        // Set Graspable Math fields
        exercise.graspableEnabled = exerciseDto.graspableEnabled != null ? exerciseDto.graspableEnabled : false;
        exercise.graspableInitialExpression = exerciseDto.graspableInitialExpression;
        exercise.graspableTargetExpression = exerciseDto.graspableTargetExpression;
        exercise.graspableAllowedOperations = exerciseDto.graspableAllowedOperations;
        exercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        exercise.graspableHints = exerciseDto.graspableHints;
        exercise.graspableConfig = exerciseDto.graspableConfig;

        // Set user - required for creation
        final UserEntity user = UserEntity.findById(exerciseDto.userId);
        if (user == null) {
            throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
        }
        exercise.user = user;

        // Set exercise if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = LessonEntity.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            exercise.lesson = lesson;
        }

        exercise.persist();
        return new ExerciseViewDto(exercise);
    }

    @Transactional
    public ExerciseViewDto updateExercise(final Long id, final ExerciseDto exerciseDto) {
        // Validate required fields for PUT
        if (exerciseDto.title == null || exerciseDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for updating an exercise");
        }
        if (exerciseDto.content == null || exerciseDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating an exercise");
        }

        final ExerciseEntity existingExercise = ExerciseEntity.findById(id);
        if (existingExercise == null) {
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingExercise.title = exerciseDto.title;
        existingExercise.content = exerciseDto.content;
        existingExercise.published = exerciseDto.published != null ? exerciseDto.published : false;
        existingExercise.commentable = exerciseDto.commentable != null ? exerciseDto.commentable : false;
        existingExercise.lastEdit = LocalDateTime.now();

        // Update Graspable Math fields
        existingExercise.graspableEnabled = exerciseDto.graspableEnabled != null ? exerciseDto.graspableEnabled : false;
        existingExercise.graspableInitialExpression = exerciseDto.graspableInitialExpression;
        existingExercise.graspableTargetExpression = exerciseDto.graspableTargetExpression;
        existingExercise.graspableAllowedOperations = exerciseDto.graspableAllowedOperations;
        existingExercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        existingExercise.graspableHints = exerciseDto.graspableHints;
        existingExercise.graspableConfig = exerciseDto.graspableConfig;

        // Set user if provided, otherwise keep existing user
        if (exerciseDto.userId != null) {
            final UserEntity user = UserEntity.findById(exerciseDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
            }
            existingExercise.user = user;
        }
        // Note: Do not set to null if userId is not provided - preserve existing user

        // Set lesson if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = LessonEntity.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            existingExercise.lesson = lesson;
        } else {
            existingExercise.lesson = null;
        }

        existingExercise.persist();
        return new ExerciseViewDto(existingExercise);
    }

    @Transactional
    public ExerciseViewDto patchExercise(final Long id, final ExerciseDto exerciseDto) {
        final ExerciseEntity existingExercise = ExerciseEntity.findById(id);
        if (existingExercise == null) {
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (exerciseDto.title != null && !exerciseDto.title.trim().isEmpty()) {
            existingExercise.title = exerciseDto.title;
        }
        if (exerciseDto.content != null && !exerciseDto.content.trim().isEmpty()) {
            existingExercise.content = exerciseDto.content;
        }
        if (exerciseDto.published != null) {
            existingExercise.published = exerciseDto.published;
        }
        if (exerciseDto.commentable != null) {
            existingExercise.commentable = exerciseDto.commentable;
        }

        // Update Graspable Math fields if provided
        if (exerciseDto.graspableEnabled != null) {
            existingExercise.graspableEnabled = exerciseDto.graspableEnabled;
        }
        if (exerciseDto.graspableInitialExpression != null) {
            existingExercise.graspableInitialExpression = exerciseDto.graspableInitialExpression;
        }
        if (exerciseDto.graspableTargetExpression != null) {
            existingExercise.graspableTargetExpression = exerciseDto.graspableTargetExpression;
        }
        if (exerciseDto.graspableAllowedOperations != null) {
            existingExercise.graspableAllowedOperations = exerciseDto.graspableAllowedOperations;
        }
        if (exerciseDto.graspableDifficulty != null) {
            existingExercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        }
        if (exerciseDto.graspableHints != null) {
            existingExercise.graspableHints = exerciseDto.graspableHints;
        }
        if (exerciseDto.graspableConfig != null) {
            existingExercise.graspableConfig = exerciseDto.graspableConfig;
        }

        // Set user if provided
        if (exerciseDto.userId != null) {
            final UserEntity user = UserEntity.findById(exerciseDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
            }
            existingExercise.user = user;
        }

        // Set lesson if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = LessonEntity.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            existingExercise.lesson = lesson;
        }

        existingExercise.lastEdit = LocalDateTime.now();
        existingExercise.persist();
        return new ExerciseViewDto(existingExercise);
    }

    @Transactional
    public boolean deleteExercise(final Long id) {
        return ExerciseEntity.deleteById(id);
    }

    public List<ExerciseViewDto> searchExercises(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllExercises();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<ExerciseEntity> exercises = ExerciseEntity.find(
                "LOWER(title) LIKE ?1 OR content LIKE ?1 OR LOWER(user.username) LIKE ?1 ORDER BY created DESC",
                searchTerm).list();
        return exercises.stream()
                .map(ExerciseViewDto::new)
                .toList();
    }

    public List<ExerciseViewDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllExercises();
        }

        try {
            final var start = LocalDate.parse(startDate);
            final var end = LocalDate.parse(endDate);

            final var startDateTime = start.atStartOfDay();
            final var endDateTime = end.atTime(LocalTime.MAX);

            final List<ExerciseEntity> exercises = ExerciseEntity
                    .find("created >= ?1 AND created <= ?2 ORDER BY created DESC", startDateTime, endDateTime).list();
            return exercises.stream()
                    .map(ExerciseViewDto::new)
                    .toList();
        } catch (final Exception e) {
            // If date parsing fails, return all exercises
            return this.getAllExercises();
        }
    }
}
