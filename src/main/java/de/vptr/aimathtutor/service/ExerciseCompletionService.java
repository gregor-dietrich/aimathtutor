package de.vptr.aimathtutor.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.ExerciseViewDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service responsible for enriching exercise DTOs with user-specific completion
 * data.
 *
 * <p>Decoupled from {@link ExerciseService} to keep exercise CRUD separate from
 * analytics/read-model concerns.</p>
 */
@ApplicationScoped
public class ExerciseCompletionService {

    private static final Logger LOG = LoggerFactory.getLogger(ExerciseCompletionService.class);

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
    public ExerciseViewDto enrichWithCompletionData(final ExerciseViewDto dto) {
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

        } catch (final RuntimeException e) {
            // Log the error but don't fail - this ensures we don't break the exercise
            // loading functionality
            LOG.error("Error enriching exercise DTO with completion data for exercise ID: {}", dto.id, e);
        }

        return dto;
    }

    /**
     * Batch-enriches a list of ExerciseViewDtos with completion data for the
     * current user. Uses a single query to load all user sessions and avoid N+1
     * patterns.
     *
     * @param dtos The exercise DTOs to enrich
     * @return The enriched DTOs
     */
    public List<ExerciseViewDto> enrichListWithCompletionData(final List<ExerciseViewDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return dtos;
        }

        try {
            final Long currentUserId = this.authService.getUserId();
            if (currentUserId == null) {
                return dtos;
            }

            // Batch-load all sessions for this user grouped by exercise (single query)
            final var sessionsByExercise = this.analyticsService
                    .getSessionsByUserGroupedByExercise(currentUserId);

            for (final ExerciseViewDto dto : dtos) {
                final var userSessions = sessionsByExercise.getOrDefault(dto.publicId, List.of());
                final var completedSessions = userSessions.stream()
                        .filter(s -> Boolean.TRUE.equals(s.completed))
                        .toList();
                dto.userCompleted = !completedSessions.isEmpty();
                dto.userCompletionCount = completedSessions.size();
            }
        } catch (final RuntimeException e) {
            LOG.error("Error enriching exercise DTO list with completion data", e);
        }

        return dtos;
    }
}
