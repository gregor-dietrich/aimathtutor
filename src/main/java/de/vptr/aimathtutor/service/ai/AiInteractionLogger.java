package de.vptr.aimathtutor.service.ai;

import java.io.IOException;
import java.util.HashMap;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.AiInteractionRepository;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for logging AI tutor interactions to the database.
 */
@ApplicationScoped
public class AiInteractionLogger {

    private static final Logger LOG = Logger.getLogger(AiInteractionLogger.class);

    @Inject
    UserRepository userRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Inject
    AiInteractionRepository aiInteractionRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Logs an AI interaction to the database for analytics.
     *
     * @param event    the student's action
     * @param feedback the AI's feedback
     */
    @Transactional
    public void logInteraction(final GraspableEventDto event, final AiFeedbackDto feedback) {
        try {
            final var interaction = new AiInteractionEntity();
            interaction.sessionId = event.sessionId;

            // Look up user and exercise if IDs are provided
            if (event.studentId != null) {
                interaction.user = this.userRepository.findById(event.studentId);
            }
            if (event.exerciseId != null) {
                interaction.exercise = this.exerciseRepository.findById(event.exerciseId);
            }

            interaction.eventType = event.eventType != null ? event.eventType : "UNKNOWN";
            interaction.expressionBefore = event.expressionBefore;
            interaction.expressionAfter = event.expressionAfter;
            interaction.feedbackType = feedback.type != null ? feedback.type.toString() : "UNKNOWN";
            interaction.feedbackMessage = feedback.message;
            interaction.confidenceScore = feedback.confidence;
            interaction.actionCorrect = event.correct;
            try {
                final var contextMap = new HashMap<String, Object>();
                contextMap.put("eventType", event.eventType);
                contextMap.put("exprBeforeLen",
                        event.expressionBefore != null ? event.expressionBefore.length() : 0);
                contextMap.put("exprAfterLen",
                        event.expressionAfter != null ? event.expressionAfter.length() : 0);
                contextMap.put("actionCorrect", event.correct);
                interaction.conversationContext = this.objectMapper.writeValueAsString(contextMap);
            } catch (final IOException e) {
                LOG.error("Failed to serialize conversation context", e);
                interaction.conversationContext = null;
            }

            this.aiInteractionRepository.persist(interaction);
            LOG.debugf("Logged AI interaction: id=%s",  interaction.id);
        } catch (final RuntimeException e) {
            LOG.error("Failed to log AI interaction", e);
            // Don't fail the request if logging fails
        }
    }

    /**
     * Log a student question and AI answer as an interaction.
     * Used for recording conversational interactions in the SessionDetailsView.
     * Marked as @Transactional to ensure proper persistence in async contexts.
     *
     * @param sessionId       the session identifier
     * @param userId          the user ID
     * @param exerciseId      the exercise ID
     * @param studentQuestion the student's question
     * @param aiAnswer        the AI's answer
     */
    @Transactional
    public void logQuestionInteraction(final String sessionId, final Long userId, final Long exerciseId,
            final String studentQuestion, final String aiAnswer) {
        try {
            LOG.infof(
                    "Logging question interaction: sessionId=%s, userId=%s, exerciseId=%s, questionLen=%s, answerLen=%s", 
                    sessionId,  userId,  exerciseId, 
                    studentQuestion != null ? studentQuestion.length() : 0, 
                    aiAnswer != null ? aiAnswer.length() : 0);

            // Create TWO separate records: one for student question, one for AI answer
            // This ensures they appear as separate rows in the SessionDetailView grid

            String contextJson = null;
            try {
                final var contextMap = new HashMap<String, Object>();
                contextMap.put("questionLength", studentQuestion != null ? studentQuestion.length() : 0);
                contextMap.put("answerLength", aiAnswer != null ? aiAnswer.length() : 0);
                contextJson = this.objectMapper.writeValueAsString(contextMap);
            } catch (final IOException e) {
                LOG.error("Failed to serialize question context", e);
                contextJson = null;
            }

            // 1. Log the student question
            final var studentQuestionRecord = new AiInteractionEntity();
            studentQuestionRecord.sessionId = sessionId;
            studentQuestionRecord.eventType = "QUESTION";
            studentQuestionRecord.feedbackType = "QUESTION";
            studentQuestionRecord.studentMessage = studentQuestion;
            studentQuestionRecord.conversationContext = contextJson;

            UserEntity user = null;
            if (userId != null) {
                user = this.userRepository.findById(userId);
                if (user == null) {
                    LOG.warnf("User not found for logging question interaction: userId=%s",  userId);
                } else {
                    studentQuestionRecord.user = user;
                }
            }

            ExerciseEntity exercise = null;
            if (exerciseId != null) {
                exercise = this.exerciseRepository.findById(exerciseId);
                if (exercise == null) {
                    LOG.warnf("Exercise not found for logging question interaction: exerciseId=%s",  exerciseId);
                } else {
                    studentQuestionRecord.exercise = exercise;
                }
            }

            this.aiInteractionRepository.persist(studentQuestionRecord);
            LOG.infof("Successfully logged student question: id=%s, msgLen=%s", 
                    studentQuestionRecord.id, 
                    studentQuestionRecord.studentMessage != null ? studentQuestionRecord.studentMessage.length() : 0);

            // 2. Log the AI answer as a separate record
            final var aiAnswerRecord = new AiInteractionEntity();
            aiAnswerRecord.sessionId = sessionId;
            aiAnswerRecord.eventType = "QUESTION_ANSWER";
            aiAnswerRecord.feedbackType = "ANSWER";
            aiAnswerRecord.feedbackMessage = aiAnswer;
            aiAnswerRecord.conversationContext = contextJson;

            aiAnswerRecord.user = user;
            aiAnswerRecord.exercise = exercise;

            this.aiInteractionRepository.persist(aiAnswerRecord);
            LOG.infof("Successfully logged AI answer: id=%s, msgLen=%s", 
                    aiAnswerRecord.id, 
                    aiAnswerRecord.feedbackMessage != null ? aiAnswerRecord.feedbackMessage.length() : 0);
        } catch (final RuntimeException e) {
            LOG.error("Failed to log question interaction", e);
            // Don't fail the request if logging fails
        }
    }
}
