package de.vptr.aimathtutor.util;

/**
 * Application-wide constants to eliminate magic values.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class
    }

    /** Async timeout for admin view data loading in seconds. */
    public static final int ADMIN_ASYNC_TIMEOUT_SECONDS = 30;

    /** Standard grid column width for ID columns. */
    public static final String GRID_ID_WIDTH = "80px";

    /** Standard grid column width for action button columns. */
    public static final String GRID_ACTION_WIDTH = "150px";

    /** Standard grid column width for name/title columns. */
    public static final String GRID_NAME_WIDTH = "200px";

    /** Maximum retries for external AI service calls. */
    public static final int RETRY_MAX_RETRIES = 3;

    /** Delay between retries in milliseconds. */
    public static final int RETRY_DELAY_MS = 1000;

    /** Jitter added to retry delays in milliseconds. */
    public static final int RETRY_JITTER_MS = 200;

    /** Number of flags before a comment is automatically hidden. */
    public static final int COMMENT_AUTO_HIDE_THRESHOLD = 5;

    /** Notification duration for success messages in milliseconds. */
    public static final int NOTIFICATION_DURATION_SUCCESS_MS = 3000;

    /** Notification duration for error messages in milliseconds. */
    public static final int NOTIFICATION_DURATION_ERROR_MS = 5000;

    /** Notification duration for info messages in milliseconds. */
    public static final int NOTIFICATION_DURATION_INFO_MS = 4000;

    /** Notification duration for warning messages in milliseconds. */
    public static final int NOTIFICATION_DURATION_WARNING_MS = 5000;

    /** Canvas height for the exercise workspace view. */
    public static final String CANVAS_HEIGHT_WORKSPACE = "77vh";

    /** Default user avatar emoji. */
    public static final String AVATAR_DEFAULT_USER = "🧒";

    /** Default tutor avatar emoji. */
    public static final String AVATAR_DEFAULT_TUTOR = "🧑‍🏫";

    /** Default AI avatar emoji. */
    public static final String AVATAR_DEFAULT_AI = "🤖";

    /** Minimum user name length. */
    public static final int USER_USERNAME_MIN_LENGTH = 3;

    /** Maximum user name length. */
    public static final int USER_USERNAME_MAX_LENGTH = 50;

    /** Minimum password length. */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /** Maximum password length. */
    public static final int PASSWORD_MAX_LENGTH = 100;

    /** Minimum userrank name length. */
    public static final int USERRANK_NAME_MIN_LENGTH = 1;

    /** Maximum userrank name length. */
    public static final int USERRANK_NAME_MAX_LENGTH = 100;

    /** Minimum comment content length. */
    public static final int COMMENT_CONTENT_MIN_LENGTH = 1;

    /** Maximum comment content length. */
    public static final int COMMENT_CONTENT_MAX_LENGTH = 1000;

    /** Minimum exercise name length. */
    public static final int EXERCISE_TITLE_MIN_LENGTH = 1;

    /** Maximum exercise name length. */
    public static final int EXERCISE_TITLE_MAX_LENGTH = 255;

    /** Minimum exercise content length. */
    public static final int EXERCISE_CONTENT_MIN_LENGTH = 1;

    /** Maximum exercise content length. */
    public static final int EXERCISE_CONTENT_MAX_LENGTH = 50_000;

    /** Maximum exercise expression length. */
    public static final int EXERCISE_EXPRESSION_MAX_LENGTH = 1000;

    /** Maximum exercise hints length. */
    public static final int EXERCISE_HINTS_MAX_LENGTH = 5000;

    /** Minimum lesson name length. */
    public static final int LESSON_NAME_MIN_LENGTH = 1;

    /** Maximum lesson name length. */
    public static final int LESSON_NAME_MAX_LENGTH = 255;

    // IP addresses blocked from external AI provider URL configuration
    public static final String BLOCKED_HOST_LOCALHOST = "localhost";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String BLOCKED_HOST_LOOPBACK_IPV4 = "127.0.0.1";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String BLOCKED_HOST_LOOPBACK_IPV6 = "::1";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String BLOCKED_HOST_LOOPBACK_IPV6_EXPANDED = "0:0:0:0:0:0:0:1";

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String BLOCKED_HOST_ANY = "0.0.0.0";

    /** Message shown when an exercise is solved correctly. */
    public static final String EXERCISE_SOLVED_MESSAGE = "🎉 Congratulations! You've solved the problem correctly!";

    /** Vaadin session attribute key for the authenticated username. */
    public static final String SESSION_KEY_USERNAME = "authenticated.username";

    // Keep these in sync with the corresponding rows in sql/init.sql.
    // Assigned in a static initializer (not as constant-expression field
    // initializers) so the literals are NOT inlined into every referencing
    // class; that inlining triggers SpotBugs HSC_HUGE_SHARED_STRING_CONSTANT.

    /** Default prefix prompt for AI question answering. */
    public static final String PROMPT_QUESTION_ANSWERING_PREFIX;

    /** Default postfix prompt for AI question answering. */
    public static final String PROMPT_QUESTION_ANSWERING_POSTFIX;

    /** Default prefix prompt for AI math tutoring. */
    public static final String PROMPT_MATH_TUTORING_PREFIX;

    /** Default postfix prompt for AI math tutoring. */
    public static final String PROMPT_MATH_TUTORING_POSTFIX;

    static {
        PROMPT_QUESTION_ANSWERING_PREFIX = "You are a helpful AI math tutor. "
                + "A student is working on an algebra problem and has asked you a question.";

        PROMPT_QUESTION_ANSWERING_POSTFIX = """
                Provide a helpful, encouraging answer that:
                - Guides the student's thinking without solving it for them
                - Is concise (2-3 sentences max)
                - Relates to their current problem if possible
                - Uses clear, simple language
                - Encourages them to try the next step
                - Writes mathematical expressions in LaTeX: wrap inline math in single $...$ \
                and display math in $$...$$; do not use other math notations

                Your answer:""";

        PROMPT_MATH_TUTORING_PREFIX = "You are an encouraging but concise AI math tutor helping a student learn "
                + "algebra. Analyze the student's action and provide brief, helpful feedback.";

        PROMPT_MATH_TUTORING_POSTFIX = """
                Provide feedback in the following JSON format:
                {
                  "type": "POSITIVE" or "CORRECTIVE" or "HINT" or "SUGGESTION",
                  "message": "Your brief, encouraging feedback (ONE sentence only)",
                  "hints": [],
                  "suggestedNextSteps": [],
                  "confidence": 0.0 to 1.0
                }

                IMPORTANT Guidelines:
                - Keep message to ONE SHORT sentence (max 15 words)
                - Be encouraging but not overly enthusiastic
                - If the action is correct, give brief praise
                - If incorrect, point out the error gently
                - Only provide hints array if student made a mistake (max 1-2 hints)
                - Do NOT provide hints for correct actions
                - Leave suggestedNextSteps empty unless specifically needed
                - Be specific about what they did, not generic
                - In the "message" field, write mathematical expressions in LaTeX: wrap inline \
                math in single $...$ and display math in $$...$$""";
    }
}
