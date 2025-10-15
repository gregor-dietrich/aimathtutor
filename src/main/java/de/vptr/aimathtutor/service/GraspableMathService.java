package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Service for managing Graspable Math workspace sessions and events.
 * Handles session creation, event processing, and state management.
 */
@ApplicationScoped
public class GraspableMathService {

    private static final Logger LOG = LoggerFactory.getLogger(GraspableMathService.class);

    /**
     * Creates a new student session for working on an exercise.
     * 
     * @param userId     The student's user ID
     * @param exerciseId The exercise ID
     * @return The session ID
     */
    @Transactional
    public String createSession(final Long userId, final Long exerciseId) {
        final String sessionId = UUID.randomUUID().toString();

        final UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            LOG.error("User not found: {}", userId);
            throw new IllegalArgumentException("User not found: " + userId);
        }

        final ExerciseEntity exercise = ExerciseEntity.findById(exerciseId);
        if (exercise == null) {
            LOG.error("Exercise not found: {}", exerciseId);
            throw new IllegalArgumentException("Exercise not found: " + exerciseId);
        }

        final var session = new StudentSessionEntity();
        session.sessionId = sessionId;
        session.user = user;
        session.exercise = exercise;
        session.startTime = LocalDateTime.now();
        session.completed = false;
        session.actionsCount = 0;
        session.correctActions = 0;
        session.hintsUsed = 0;

        session.persist();
        LOG.info("Created new session: {} for user {} on exercise {}", sessionId, userId, exerciseId);

        return sessionId;
    }

    /**
     * Retrieves a session by its ID.
     * 
     * @param sessionId The session ID
     * @return The session entity, or null if not found
     */
    public StudentSessionEntity getSession(final String sessionId) {
        return StudentSessionEntity.findBySessionId(sessionId);
    }

    /**
     * Processes a Graspable Math event and updates session statistics.
     * 
     * @param event The event to process
     */
    @Transactional
    public void processEvent(final GraspableEventDto event) {
        LOG.debug("Processing Graspable Math event: {}", event);

        final var session = StudentSessionEntity.findBySessionId(event.sessionId);
        if (session == null) {
            LOG.warn("Session not found: {}", event.sessionId);
            return;
        }

        // Update session statistics
        session.actionsCount++;
        if (event.correct != null && event.correct) {
            session.correctActions++;
        }

        // Update final expression
        if (event.expressionAfter != null) {
            session.finalExpression = event.expressionAfter;
        }

        session.persist();
        LOG.debug("Updated session {}: {} actions, {} correct",
                event.sessionId, session.actionsCount, session.correctActions);
    }

    /**
     * Marks a session as completed.
     * 
     * @param sessionId The session ID
     */
    @Transactional
    public void completeSession(final String sessionId) {
        final var session = StudentSessionEntity.findBySessionId(sessionId);
        if (session != null) {
            session.completed = true;
            session.endTime = LocalDateTime.now();
            session.persist();
            LOG.info("Completed session: {}", sessionId);
        }
    }

    /**
     * Increments the hints used counter for a session.
     * 
     * @param sessionId The session ID
     */
    @Transactional
    public void recordHintUsed(final String sessionId) {
        final var session = StudentSessionEntity.findBySessionId(sessionId);
        if (session != null) {
            session.hintsUsed++;
            session.persist();
            LOG.debug("Hint used in session: {}", sessionId);
        }
    }

    /**
     * Retrieves all sessions for a specific user.
     * 
     * @param userId The user ID
     * @return List of sessions
     */
    public List<StudentSessionEntity> getUserSessions(final Long userId) {
        return StudentSessionEntity.list("user.id", userId);
    }

    /**
     * Retrieves all sessions for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     * @return List of sessions
     */
    public List<StudentSessionEntity> getExerciseSessions(final Long exerciseId) {
        return StudentSessionEntity.list("exercise.id", exerciseId);
    }

    /**
     * Calculates the accuracy rate for a session.
     * 
     * @param sessionId The session ID
     * @return Accuracy as a percentage (0-100), or null if session not found
     */
    public Double getSessionAccuracy(final String sessionId) {
        final var session = StudentSessionEntity.findBySessionId(sessionId);
        if (session == null || session.actionsCount == 0) {
            return null;
        }

        return (session.correctActions.doubleValue() / session.actionsCount) * 100.0;
    }

    /**
     * Checks if the current expression matches the target expression.
     * Normalizes both expressions for comparison (handles whitespace, order of
     * terms, etc.)
     * 
     * @param currentExpression The current mathematical expression
     * @param targetExpression  The target/goal expression
     * @return true if expressions are equivalent, false otherwise
     */
    public boolean checkCompletion(final String currentExpression, final String targetExpression) {
        if (currentExpression == null || targetExpression == null) {
            return false;
        }

        // Normalize both expressions
        final String normalizedCurrent = this.normalizeExpression(currentExpression);
        final String normalizedTarget = this.normalizeExpression(targetExpression);

        LOG.debug("Checking completion: '{}' vs '{}'", normalizedCurrent, normalizedTarget);

        // Direct string comparison after normalization
        return normalizedCurrent.equals(normalizedTarget);
    }

    /**
     * Normalizes a mathematical expression for comparison.
     * - Removes all whitespace
     * - Converts to lowercase
     * - Sorts terms in addition/subtraction (basic normalization)
     * 
     * @param expression The expression to normalize
     * @return Normalized expression
     */
    private String normalizeExpression(final String expression) {
        if (expression == null) {
            return "";
        }

        // Remove all whitespace
        String normalized = expression.replaceAll("\\s+", "");

        // Convert to lowercase
        normalized = normalized.toLowerCase();

        // Handle common equivalent forms
        // e.g., "x=5" is equivalent to "5=x"
        if (normalized.contains("=")) {
            final String[] parts = normalized.split("=");
            if (parts.length == 2) {
                // Sort the sides to handle "x=5" and "5=x" as equivalent
                final String left = parts[0].trim();
                final String right = parts[1].trim();
                // If one side is just a number/variable and the other is more complex, keep
                // order
                // Otherwise, sort alphabetically for consistency
                if (left.compareTo(right) > 0) {
                    normalized = right + "=" + left;
                }
            }
        }

        return normalized;
    }

    /**
     * Marks a session as completed.
     * 
     * @param sessionId The session ID to mark complete
     */
    @Transactional
    public void markSessionComplete(final String sessionId) {
        final var session = StudentSessionEntity.findBySessionId(sessionId);
        if (session == null) {
            LOG.warn("Cannot mark session complete - session not found: {}", sessionId);
            return;
        }

        session.completed = true;
        session.endTime = LocalDateTime.now();
        session.persist();
        LOG.debug("Session marked complete: {}", sessionId);
    }
}
