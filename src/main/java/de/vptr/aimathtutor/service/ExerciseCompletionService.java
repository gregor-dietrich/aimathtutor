package de.vptr.aimathtutor.service;

import java.util.List;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.service.security.AuthService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service responsible for enriching exercise DTOs with user-specific completion data.
 *
 * <p>
 * Decoupled from {@link ExerciseService} to keep exercise CRUD separate from analytics/read-model concerns.
 * </p>
 */
@ApplicationScoped
public class ExerciseCompletionService {

    private static final Logger LOG = Logger.getLogger(ExerciseCompletionService.class);

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    /**
     * Enriches an ExerciseViewDto with completion data for the current user. If the user is not authenticated,
     * completion fields remain null.
     *
     * @param dto
     *            The exercise DTO to enrich
     * @return The enriched DTO
     */
    public ExerciseViewDto enrichWithCompletionData(final ExerciseViewDto dto) {
        return this.enrichWithCompletionData(dto, this.authService.getUserId());
    }

    /**
     * Enriches an ExerciseViewDto with completion data for the given user. Prefer this overload from background threads
     * (e.g. {@code AsyncDataLoader} suppliers): resolve the user id on the UI thread first, because
     * {@code VaadinSession} — and therefore {@code AuthService.getUserId()} — is unavailable off the UI thread.
     *
     * @param dto
     *            The exercise DTO to enrich
     * @param userId
     *            The id of the user whose completion data should be applied; null leaves it unset
     * @return The enriched DTO
     */
    public ExerciseViewDto enrichWithCompletionData(final ExerciseViewDto dto, @Nullable final Long userId) {
        if (dto == null) {
            return dto;
        }

        try {
            if (userId == null) {
                // No user resolved, leave completion data as null
                return dto;
            }

            // Get completed sessions for this user on this exercise (single query)
            final var userSessions = this.analyticsService.getSessionsByUserAndExercise(userId, dto.id);

            // Check if any session was completed
            final var completedSessions = userSessions.stream().filter(s -> Boolean.TRUE.equals(s.completed)).toList();

            dto.userCompleted = !completedSessions.isEmpty();
            dto.userCompletionCount = completedSessions.size();

        } catch (final RuntimeException e) {
            // Log the error but don't fail - this ensures we don't break the exercise
            // loading functionality
            LOG.errorf(e, "Error enriching exercise DTO with completion data for exercise ID: %s", dto.id);
        }

        return dto;
    }

    /**
     * Batch-enriches a list of ExerciseViewDtos with completion data for the current user. Uses a single query to load
     * all user sessions and avoid N+1 patterns.
     *
     * @param dtos
     *            The exercise DTOs to enrich
     * @return The enriched DTOs
     */
    public List<ExerciseViewDto> enrichListWithCompletionData(final List<ExerciseViewDto> dtos) {
        return this.enrichListWithCompletionData(dtos, this.authService.getUserId());
    }

    /**
     * Batch-enriches a list of ExerciseViewDtos with completion data for the given user. Prefer this overload from
     * background threads (e.g. {@code AsyncDataLoader} suppliers): resolve the user id on the UI thread first, because
     * {@code VaadinSession} — and therefore {@code AuthService.getUserId()} — is unavailable off the UI thread.
     *
     * @param dtos
     *            The exercise DTOs to enrich
     * @param userId
     *            The id of the user whose completion data should be applied; null leaves it unset
     * @return The enriched DTOs
     */
    public List<ExerciseViewDto> enrichListWithCompletionData(final List<ExerciseViewDto> dtos,
            @Nullable final Long userId) {
        if (dtos == null || dtos.isEmpty()) {
            return dtos;
        }

        try {
            if (userId == null) {
                return dtos;
            }

            // Batch-load all sessions for this user grouped by exercise (single query)
            final var sessionsByExercise = this.analyticsService.getSessionsByUserGroupedByExercise(userId);

            for (final ExerciseViewDto dto : dtos) {
                final var userSessions = sessionsByExercise.getOrDefault(dto.publicId, List.of());
                final var completedSessions =
                        userSessions.stream().filter(s -> Boolean.TRUE.equals(s.completed)).toList();
                dto.userCompleted = !completedSessions.isEmpty();
                dto.userCompletionCount = completedSessions.size();
            }
        } catch (final RuntimeException e) {
            LOG.error("Error enriching exercise DTO list with completion data", e);
        }

        return dtos;
    }
}
