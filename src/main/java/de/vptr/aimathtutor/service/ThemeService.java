package de.vptr.aimathtutor.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for managing application theme preferences.
 * Handles theme selection, persistence, and application across the UI.
 */
@ApplicationScoped
public class ThemeService {

    /**
     * Enumeration of available application themes.
     * Supports light, dark, and system-default theme modes.
     */
    public enum Theme {
        LIGHT("Light", null),
        DARK("Dark", Lumo.DARK),
        SYSTEM("System", null);

        private final String displayName;
        private final String themeVariant;

        Theme(final String displayName, final String themeVariant) {
            this.displayName = displayName;
            this.themeVariant = themeVariant;
        }

        /**
         * Gets the user-friendly display name for this theme.
         *
         * @return the theme's display name
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Gets the Lumo theme variant string for this theme.
         *
         * @return the theme variant string, or null for automatic/default
         */
        public String getThemeVariant() {
            return this.themeVariant;
        }
    }

    private static final String THEME_SESSION_KEY = "selected_theme";

    /**
     * Retrieves the currently active theme for the session.
     *
     * @return the current {@link Theme}, defaults to SYSTEM if not set
     */
    public Theme getCurrentTheme() {
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            final var theme = (Theme) session.getAttribute(THEME_SESSION_KEY);
            return theme != null ? theme : Theme.SYSTEM;
        }
        return Theme.SYSTEM;
    }

    /**
     * Sets the theme for the current session and applies it to the UI.
     *
     * @param theme the {@link Theme} to set as current
     */
    public void setTheme(final Theme theme) {
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(THEME_SESSION_KEY, theme);
        }
        this.applyTheme(theme);
    }

    /**
     * Applies the specified theme to the current UI.
     * Handles theme application via Vaadin's UI theme list and JavaScript for
     * system-default themes.
     *
     * @param theme the {@link Theme} to apply
     */
    public void applyTheme(final Theme theme) {
        final var ui = UI.getCurrent();
        if (ui != null) {
            final var themeList = ui.getElement().getThemeList();

            switch (theme) {
                case DARK:
                    // Clear any document-level theme attributes and apply dark theme
                    ui.getPage().executeJs("document.documentElement.removeAttribute('theme');");
                    themeList.clear();
                    themeList.add(Lumo.DARK);
                    break;
                case LIGHT:
                    // Clear any document-level theme attributes and ensure light theme
                    ui.getPage().executeJs("document.documentElement.removeAttribute('theme');");
                    themeList.clear();
                    // Light theme is default, no need to add anything to themeList
                    break;
                case SYSTEM:
                    // Clear Vaadin theme list and use CSS media query to detect system preference
                    themeList.clear();
                    ui.getPage().executeJs(
                            "if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {"
                                    + "  document.documentElement.setAttribute('theme', 'dark');"
                                    + "} else {"
                                    + "  document.documentElement.removeAttribute('theme');"
                                    + "}");
                    break;
                default:
                    // Unknown theme - fall back to system and log
                    themeList.clear();
                    ui.getPage().executeJs("document.documentElement.removeAttribute('theme');");
                    break;
            }
        }
    }

    /**
     * Gets the next theme in the cycle (LIGHT → DARK → SYSTEM → LIGHT).
     *
     * @return the next {@link Theme} in the rotation
     */
    public Theme getNextTheme() {
        final var current = this.getCurrentTheme();
        return switch (current) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
    }
}
