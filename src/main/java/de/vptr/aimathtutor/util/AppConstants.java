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

    /** Canvas height for the math workspace view. */
    public static final String CANVAS_HEIGHT_MATH = "80vh";

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
    public static final int EXERCISE_CONTENT_MAX_LENGTH = 50000;

    /** Maximum exercise expression length. */
    public static final int EXERCISE_EXPRESSION_MAX_LENGTH = 1000;

    /** Maximum exercise hints length. */
    public static final int EXERCISE_HINTS_MAX_LENGTH = 5000;

    /** Minimum lesson name length. */
    public static final int LESSON_NAME_MIN_LENGTH = 1;

    /** Maximum lesson name length. */
    public static final int LESSON_NAME_MAX_LENGTH = 255;
}
