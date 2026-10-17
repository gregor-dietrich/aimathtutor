package de.vptr.aimathtutor.util;

/**
 * Theme-neutral styling vocabulary for Java code. The names are defined in {@code styles.css}; accent and status tokens
 * have no {@code --vaadin-*} equivalent.
 */
public final class AppStyles {

    private AppStyles() {
        // Utility class
    }

    /** Accent colour token. */
    public static final String ACCENT = "var(--app-accent)";

    /** Accent colour token for text. */
    public static final String ACCENT_TEXT = "var(--app-accent-text)";

    /** Soft (translucent) accent colour token. */
    public static final String ACCENT_SOFT = "var(--app-accent-soft)";

    /** Muted (half-translucent) accent colour token. */
    public static final String ACCENT_MUTED = "var(--app-accent-muted)";

    /** Success text colour token. */
    public static final String SUCCESS = "var(--app-success)";

    /** Error text colour token. */
    public static final String ERROR = "var(--app-error)";

    /** Warning text colour token. */
    public static final String WARNING = "var(--app-warning)";

    /** Card surface with gradient, border and shadow. */
    public static final String GLASS_CARD = "glass-card";

    /** Raises an element slightly on hover. */
    public static final String LIFT_ON_HOVER = "lift-on-hover";

    /** Accent gradient strip along the top edge of a card. */
    public static final String ACCENT_BAR = "accent-bar";

    /** Base box of a chat message bubble. */
    public static final String CHAT_BUBBLE = "chat-bubble";

    /** Chat bubble of the student. */
    public static final String CHAT_BUBBLE_USER = "chat-bubble-user";

    /** Chat bubble of the tutor. */
    public static final String CHAT_BUBBLE_TUTOR = "chat-bubble-tutor";

    /** Chat bubble of system notices. */
    public static final String CHAT_BUBBLE_SYSTEM = "chat-bubble-system";
}
