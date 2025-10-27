package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.button.Button;

import de.vptr.aimathtutor.service.ThemeService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * TODO: Class documentation.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Service reference is transient and intentionally stored for runtime behavior; not serialized")
public class ThemeToggleButton extends Button {

    private static final long serialVersionUID = 1L;
    private final transient ThemeService themeService;

    public ThemeToggleButton(final ThemeService themeService) {
        // store as transient to avoid serializing non-serializable service
        this.themeService = themeService;
        this.addClickListener(e -> this.toggleTheme());
        this.updateButton();
    }

    private void toggleTheme() {
        final var nextTheme = this.themeService.getNextTheme();
        this.themeService.setTheme(nextTheme);
        this.updateButton();
    }

    private void updateButton() {
        final var currentTheme = this.themeService.getCurrentTheme();
        switch (currentTheme) {
            case LIGHT:
                this.setIcon(LineAwesomeIcon.SUN_SOLID.create());
                this.setTooltipText("Switch to Dark Theme");
                break;
            case DARK:
                this.setIcon(LineAwesomeIcon.MOON_SOLID.create());
                this.setTooltipText("Switch to System Theme");
                break;
            case SYSTEM:
                this.setIcon(LineAwesomeIcon.DESKTOP_SOLID.create());
                this.setTooltipText("Switch to Light Theme");
                break;
        }
    }
}
