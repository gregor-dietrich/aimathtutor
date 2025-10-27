package de.vptr.aimathtutor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.LessonRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    LessonRepository lessonRepository;

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

            // Get completed sessions for this user on this exercise (single query)
            final var userSessions = this.analyticsService.getSessionsByUserAndExercise(currentUserId, dto.id);

            // Check if any session was completed
            final var completedSessions = userSessions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.completed))
                    .toList();

            dto.userCompleted = !completedSessions.isEmpty();
            dto.userCompletionCount = completedSessions.size();

        } catch (final Exception e) {
            // Log the error but don't fail - this ensures we don't break the exercise
            // loading functionality
            log.error("Error enriching exercise DTO with completion data for exercise ID: " + dto.id, e);
        }

        return dto;
    }

    public List<ExerciseViewDto> getAllExercises() {
        return this.exerciseRepository.findAllOrdered().stream()
                .map(ExerciseViewDto::new)
                .toList();
    }

    public Optional<ExerciseViewDto> findById(final Long id) {
        return this.exerciseRepository.findByIdOptional(id)
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto(entity)));
    }

    public List<ExerciseViewDto> findPublishedExercises() {
        return this.exerciseRepository.findPublished().stream()
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto(entity)))
                .toList();
    }

    public List<ExerciseViewDto> findByUserId(final Long userId) {
        return this.exerciseRepository.findByUserId(userId).stream()
                .map(ExerciseViewDto::new)
                .toList();
    }

    public List<ExerciseViewDto> findByLessonId(final Long lessonId) {
        return this.exerciseRepository.findByLessonId(lessonId).stream()
                .map(entity -> this.enrichWithCompletionData(new ExerciseViewDto(entity)))
                .toList();
    }

    public List<ExerciseViewDto> findGraspableMathExercises() {
        return this.exerciseRepository.findGraspableMathExercises().stream()
                .map(ExerciseViewDto::new)
                .toList();
    }

    public List<ExerciseViewDto> findGraspableMathExercisesByLesson(final Long lessonId) {
        return this.exerciseRepository.findGraspableMathExercisesByLesson(lessonId).stream()
                .map(ExerciseViewDto::new)
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

        // Validate Graspable Math: if enabled, target expression is required
        if (exerciseDto.graspableEnabled != null && exerciseDto.graspableEnabled) {
            if (exerciseDto.graspableTargetExpression == null
                    || exerciseDto.graspableTargetExpression.trim().isEmpty()) {
                throw new ValidationException(
                        "Graspable Math target expression is required when Graspable Math is enabled");
            }
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
        exercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        exercise.graspableHints = exerciseDto.graspableHints;

        // Set user - required for creation
        final UserEntity user = this.userRepository.findById(exerciseDto.userId);
        if (user == null) {
            throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
        }
        exercise.user = user;

        // Set exercise if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = this.lessonRepository.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            exercise.lesson = lesson;
        }

        this.exerciseRepository.persist(exercise);
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

        final ExerciseEntity existingExercise = this.exerciseRepository.findById(id);
        if (existingExercise == null) {
            throw new WebApplicationException("Exercise not found", Response.Status.NOT_FOUND);
        }

        // Validate Graspable Math: if enabled, target expression is required
        if (exerciseDto.graspableEnabled != null && exerciseDto.graspableEnabled) {
            if (exerciseDto.graspableTargetExpression == null
                    || exerciseDto.graspableTargetExpression.trim().isEmpty()) {
                throw new ValidationException(
                        "Graspable Math target expression is required when Graspable Math is enabled");
            }
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
        existingExercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        existingExercise.graspableHints = exerciseDto.graspableHints;

        // Set user if provided, otherwise keep existing user
        if (exerciseDto.userId != null) {
            final UserEntity user = this.userRepository.findById(exerciseDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
            }
            existingExercise.user = user;
        }
        // Note: Do not set to null if userId is not provided - preserve existing user

        // Set lesson if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = this.lessonRepository.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            existingExercise.lesson = lesson;
        } else {
            existingExercise.lesson = null;
        }

        this.exerciseRepository.persist(existingExercise);
        return new ExerciseViewDto(existingExercise);
    }

    @Transactional
    public ExerciseViewDto patchExercise(final Long id, final ExerciseDto exerciseDto) {
        final ExerciseEntity existingExercise = this.exerciseRepository.findById(id);
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
        if (exerciseDto.graspableDifficulty != null) {
            existingExercise.graspableDifficulty = exerciseDto.graspableDifficulty;
        }
        if (exerciseDto.graspableHints != null) {
            existingExercise.graspableHints = exerciseDto.graspableHints;
        }

        // Set user if provided
        if (exerciseDto.userId != null) {
            final UserEntity user = this.userRepository.findById(exerciseDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + exerciseDto.userId + " not found");
            }
            existingExercise.user = user;
        }

        // Set lesson if provided
        if (exerciseDto.lessonId != null) {
            final LessonEntity lesson = this.lessonRepository.findById(exerciseDto.lessonId);
            if (lesson == null) {
                throw new ValidationException("Lesson with ID " + exerciseDto.lessonId + " not found");
            }
            existingExercise.lesson = lesson;
        }

        existingExercise.lastEdit = LocalDateTime.now();
        this.exerciseRepository.persist(existingExercise);
        return new ExerciseViewDto(existingExercise);
    }

    @Transactional
    public boolean deleteExercise(final Long id) {
        return this.exerciseRepository.deleteById(id);
    }

    public List<ExerciseViewDto> searchExercises(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllExercises();
        }
        final List<ExerciseEntity> exercises = this.exerciseRepository.search(query);
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

            final List<ExerciseEntity> exercises = this.exerciseRepository.findByDateRange(startDateTime, endDateTime);
            return exercises.stream()
                    .map(ExerciseViewDto::new)
                    .toList();
        } catch (final Exception e) {
            // If date parsing fails, return all exercises
            return this.getAllExercises();
        }
    }
}
