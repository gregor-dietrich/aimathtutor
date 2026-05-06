package de.vptr.aimathtutor.service.ai;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.AiConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for building structured prompts for AI math tutoring and question
 * answering.
 */
@ApplicationScoped
public class PromptBuilderService {

    private static final Logger LOG = Logger.getLogger(PromptBuilderService.class);

    private static final int MAX_PROMPT_INPUT_LENGTH = 2000;

    @Inject
    AiConfigService aiConfigService;

    /**
     * Builds a prompt for answering student questions.
     *
     * @param question          the student's question
     * @param currentExpression the current math expression
     * @param initialExpression the original problem state
     * @param targetExpression  the target solution state
     * @param context           conversation context
     * @return the constructed prompt string
     */
    public String buildQuestionAnsweringPrompt(final String question, final String currentExpression,
            final String initialExpression, final String targetExpression,
            final ConversationContextDto context) {
        final var prompt = new StringBuilder();

        // Load dynamic prompt configuration
        final var prefix = this.getConfigString(AiConfigKeys.PROMPT_QUESTION_PREFIX,
                "You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.");
        final var postfix = this.getConfigString(AiConfigKeys.PROMPT_QUESTION_POSTFIX,
                "Provide a helpful, encouraging answer that:\n- Guides the student's thinking without solving it for them\n- Is concise (2-3 sentences max)\n- Relates to their current problem if possible\n- Uses clear, simple language\n- Encourages them to try the next step\n\nYour answer:");

        prompt.append(prefix);
        prompt.append("\n\n");

        // Add conversation context if available
        this.appendConversationContext(prompt, context);

        if (currentExpression != null && !currentExpression.isBlank()) {
            prompt.append("<current_problem_state>\n")
                    .append(this.sanitizePromptInput(currentExpression))
                    .append("\n</current_problem_state>\n");
        }
        if (initialExpression != null && !initialExpression.isBlank()) {
            prompt.append("<original_problem>\n")
                    .append(this.sanitizePromptInput(initialExpression))
                    .append("\n</original_problem>\n");
        }
        if (targetExpression != null && !targetExpression.isBlank()) {
            prompt.append("<target_solution>\n")
                    .append(this.sanitizePromptInput(targetExpression))
                    .append("\n</target_solution>\n");
        }

        prompt.append("\n<student_question>\n")
                .append(this.sanitizePromptInput(question))
                .append("\n</student_question>\n\n");
        prompt.append(postfix);

        final var promptString = prompt.toString();
        LOG.debugf("Sending QuestionAnsweringPrompt, length=%s",  promptString.length());

        return promptString;
    }

    /**
     * Builds a structured prompt for math tutoring with conversation context.
     *
     * @param event   the student's math action event
     * @param context conversation context
     * @return the constructed prompt string
     */
    public String buildMathTutoringPrompt(final GraspableEventDto event, final ConversationContextDto context) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        final var prompt = new StringBuilder();

        // Load dynamic prompt configuration
        final var prefix = this.getConfigString(AiConfigKeys.PROMPT_TUTORING_PREFIX,
                "You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student's action and provide brief, helpful feedback.");
        final var postfix = this.getConfigString(AiConfigKeys.PROMPT_TUTORING_POSTFIX,
                "Provide feedback in the following JSON format:\n{\n  \"type\": \"POSITIVE\" or \"CORRECTIVE\" or \"HINT\" or \"SUGGESTION\",\n  \"message\": \"Your brief, encouraging feedback (ONE sentence only)\",\n  \"hints\": [],\n  \"suggestedNextSteps\": [],\n  \"confidence\": 0.0 to 1.0\n}\n\nIMPORTANT Guidelines:\n- Keep message to ONE SHORT sentence (max 15 words)\n- Be encouraging but not overly enthusiastic\n- If the action is correct, give brief praise\n- If incorrect, point out the error gently\n- Only provide hints array if student made a mistake (max 1-2 hints)\n- Do NOT provide hints for correct actions\n- Leave suggestedNextSteps empty unless specifically needed\n- Be specific about what they did, not generic\n");

        prompt.append(prefix);
        prompt.append("\n\n<student_action>\n- Action Type: ");
        prompt.append(event.eventType != null ? this.sanitizePromptInput(event.eventType) : "unknown").append("\n");
        prompt.append("</student_action>\n");

        // Add conversation context if available
        this.appendMathTutoringContext(prompt, context);

        prompt.append("\n<current_action>\n");
        if (event.expressionBefore != null) {
            prompt.append("- Expression Before: ")
                    .append(this.sanitizePromptInput(event.expressionBefore)).append("\n");
        }

        if (event.expressionAfter != null) {
            prompt.append("- Expression After: ")
                    .append(this.sanitizePromptInput(event.expressionAfter)).append("\n");
        }

        if (event.correct != null) {
            prompt.append("- Is Correct: ").append(event.correct).append("\n");
        }

        prompt.append("</current_action>\n\n");
        prompt.append(postfix);

        final var promptString = prompt.toString();
        LOG.debugf("Sending MathTutoringPrompt, length=%s",  promptString.length());

        return promptString;
    }

    /**
     * Sanitizes user input for prompt construction.
     * Truncates to a maximum length and escapes XML-like closing tags to
     * prevent prompt injection.
     *
     * @param input the raw user input
     * @return sanitized input safe for inclusion in prompts
     */
    public String sanitizePromptInput(final String input) {
        if (input == null) {
            return null;
        }
        String sanitized = input;
        if (sanitized.length() > MAX_PROMPT_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_PROMPT_INPUT_LENGTH) + "...[truncated]";
        }
        // Escape XML-like closing tags to prevent injection
        sanitized = sanitized.replace("</", "<\\/");
        return sanitized;
    }

    /**
     * Null-safe getter for string configuration values. Falls back to default if
     * aiConfigService is not injected or value is missing.
     */
    private String getConfigString(final String key, final String defaultValue) {
        if (this.aiConfigService == null) {
            LOG.debugf("AiConfigService not injected, using default for key=%s",  key);
            return defaultValue;
        }
        return this.aiConfigService.getConfigValue(key, defaultValue);
    }

    private void appendConversationContext(final StringBuilder prompt, final ConversationContextDto context) {
        if (context == null) {
            return;
        }
        final var recentActions = context.getRecentActions();
        if (!recentActions.isEmpty()) {
            prompt.append("<conversation_context>\nRecent student actions:\n");
            for (int i = 0; i < recentActions.size(); ++i) {
                final var action = recentActions.get(i);
                prompt.append(String.format("%d. %s: '%s' → '%s'%n",
                        i + 1,
                        this.sanitizePromptInput(action.eventType),
                        this.sanitizePromptInput(action.expressionBefore),
                        this.sanitizePromptInput(action.expressionAfter)));
            }
            prompt.append("</conversation_context>\n\n");
        }

        final var recentQuestions = context.getRecentQuestions();
        if (!recentQuestions.isEmpty()) {
            prompt.append("<recent_questions>\n");
            for (int i = 0; i < recentQuestions.size(); ++i) {
                final var q = recentQuestions.get(i);
                prompt.append(String.format("%d. \"%s\"%n", i + 1, this.sanitizePromptInput(q.message)));
            }
            prompt.append("</recent_questions>\n\n");
        }

        final var recentAiMessages = context.getRecentAiMessages();
        if (!recentAiMessages.isEmpty()) {
            prompt.append("<recent_responses>\n");
            for (int i = 0; i < recentAiMessages.size(); ++i) {
                final var msg = recentAiMessages.get(i);
                prompt.append(String.format("%d. \"%s\"%n", i + 1, this.sanitizePromptInput(msg.message)));
            }
            prompt.append("</recent_responses>\n\n");
        }
    }

    private void appendMathTutoringContext(final StringBuilder prompt, final ConversationContextDto context) {
        if (context == null) {
            return;
        }
        final var recentActionsTutoring = context.getRecentActions();
        if (!recentActionsTutoring.isEmpty()) {
            prompt.append("\n<recent_actions>\n");
            for (int i = 0; i < recentActionsTutoring.size(); ++i) {
                final var action = recentActionsTutoring.get(i);
                prompt.append(String.format("%d. %s: '%s' → '%s'%n",
                        i + 1,
                        this.sanitizePromptInput(action.eventType),
                        this.sanitizePromptInput(action.expressionBefore),
                        this.sanitizePromptInput(action.expressionAfter)));
            }
            prompt.append("</recent_actions>\n");
        }

        final var recentQuestionsTutoring = context.getRecentQuestions();
        if (!recentQuestionsTutoring.isEmpty()) {
            prompt.append("\n<recent_questions>\n");
            for (int i = 0; i < recentQuestionsTutoring.size(); ++i) {
                final var q = recentQuestionsTutoring.get(i);
                prompt.append(String.format("%d. \"%s\"%n", i + 1, this.sanitizePromptInput(q.message)));
            }
            prompt.append("</recent_questions>\n");
        }

        final var recentAiMessagesTutoring = context.getRecentAiMessages();
        if (!recentAiMessagesTutoring.isEmpty()) {
            prompt.append("\n<recent_feedback>\n");
            for (int i = 0; i < recentAiMessagesTutoring.size(); ++i) {
                final var msg = recentAiMessagesTutoring.get(i);
                prompt.append(String.format("%d. \"%s\"%n", i + 1, this.sanitizePromptInput(msg.message)));
            }
            prompt.append("</recent_feedback>\n");
        }
    }
}
