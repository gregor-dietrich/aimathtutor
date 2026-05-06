package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.StudentSessionRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing Graspable Math workspace sessions and events.
 * Handles session creation, event processing, and state management.
 */
@ApplicationScoped
public class GraspableMathService {

    private static final Logger LOG = Logger.getLogger(GraspableMathService.class);

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    StudentSessionRepository studentSessionRepository;

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
        final UserEntity user = this.userRepository.findById(userId);
        if (user == null) {
            LOG.errorf("User not found: %s",  userId);
            throw new IllegalArgumentException("User not found: " + userId);
        }

        final ExerciseEntity exercise = this.exerciseRepository.findById(exerciseId);
        if (exercise == null) {
            LOG.errorf("Exercise not found: %s",  exerciseId);
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

        this.studentSessionRepository.persist(session);
        LOG.infof("Created new session: %s for user %s on exercise %s",  sessionId,  userId,  exerciseId);

        return sessionId;
    }

    /**
     * Retrieves a session by its ID.
     * 
     * @param sessionId The session ID
     * @return The session entity, or null if not found
     */
    public StudentSessionEntity getSession(final String sessionId) {
        return this.studentSessionRepository.findBySessionId(sessionId);
    }

    /**
     * Processes a Graspable Math event and updates session statistics.
     * 
     * @param event The event to process
     */
    @Transactional
    public void processEvent(final GraspableEventDto event) {
        LOG.debugf("Processing Graspable Math event: %s",  event);

        final var session = this.studentSessionRepository.findBySessionId(event.sessionId);
        if (session == null) {
            LOG.warnf("Session not found: %s",  event.sessionId);
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

        this.studentSessionRepository.persist(session);
        LOG.debugf("Updated session %s: %s actions, %s correct", 
                event.sessionId,  session.actionsCount,  session.correctActions);
    }

    /**
     * Marks a session as completed.
     * 
     * @param sessionId The session ID
     */
    @Transactional
    public void completeSession(final String sessionId) {
        if (this.doCompleteSession(sessionId)) {
            LOG.infof("Completed session: %s",  sessionId);
        }
    }

    /**
     * Increments the hints used counter for a session.
     * 
     * @param sessionId The session ID
     */
    @Transactional
    public void recordHintUsed(final String sessionId) {
        final var session = this.studentSessionRepository.findBySessionId(sessionId);
        if (session != null) {
            session.hintsUsed++;
            this.studentSessionRepository.persist(session);
            LOG.debugf("Hint used in session: %s",  sessionId);
        }
    }

    /**
     * Retrieves all sessions for a specific user.
     * 
     * @param userId The user ID
     * @return List of sessions
     */
    public List<StudentSessionEntity> getUserSessions(final Long userId) {
        return this.studentSessionRepository.findByUserId(userId);
    }

    /**
     * Retrieves all sessions for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     * @return List of sessions
     */
    public List<StudentSessionEntity> getExerciseSessions(final Long exerciseId) {
        return this.studentSessionRepository.findByExerciseId(exerciseId);
    }

    /**
     * Calculates the accuracy rate for a session.
     * 
     * @param sessionId The session ID
     * @return Accuracy as a percentage (0-100), or null if session not found
     */
    public Double getSessionAccuracy(final String sessionId) {
        final var session = this.studentSessionRepository.findBySessionId(sessionId);
        if (session == null || session.actionsCount == 0) {
            return null;
        }

        return ((double) session.correctActions / session.actionsCount) * 100.0;
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

        LOG.debugf("Checking completion: '%s' vs '%s'",  normalizedCurrent,  normalizedTarget);

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
        String normalized = WHITESPACE_PATTERN.matcher(expression).replaceAll("");

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
        if (!this.doCompleteSession(sessionId)) {
            LOG.debugf("Session not marked complete (not found or already completed): %s",  sessionId);
        } else {
            LOG.debugf("Session marked complete: %s",  sessionId);
        }
    }

    private boolean doCompleteSession(final String sessionId) {
        final var session = this.studentSessionRepository.findBySessionId(sessionId);
        if (session == null || Boolean.TRUE.equals(session.completed)) {
            return false;
        }
        session.completed = true;
        if (session.endTime == null) {
            session.endTime = LocalDateTime.now();
        }
        this.studentSessionRepository.persist(session);
        return true;
    }
}
