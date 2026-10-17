package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.service.ThemeService;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;

/**
 * Cycles the signed-in user's colour mode (light, dark, follow system). The icon shows the current mode; a click saves
 * the next one for the account off the UI thread, then applies it to the page without a reload.
 */
public final class ThemeToggleButton extends Button {
    private final transient ThemeService themeService;
    private final transient AuthService authService;
    private boolean saveInProgress;

    /**
     * Constructs a ThemeToggleButton with the specified services.
     */
    public ThemeToggleButton(final ThemeService themeService, final AuthService authService) {
        // store as transient to avoid serializing non-serializable services
        this.themeService = themeService;
        this.authService = authService;
        this.addClickListener(e -> this.toggleColorMode());
        this.updateButton();
    }

    private void toggleColorMode() {
        if (this.saveInProgress) {
            return;
        }
        // Captured here on the UI thread: the supplier runs off it.
        final var credentials = this.authService.currentSessionCredentials();
        final var session = VaadinSession.getCurrent();
        final var ui = this.getUI().orElse(null);
        if (credentials == null || session == null || ui == null) {
            NotificationUtil.showError(AppConstants.SESSION_ENDED_MESSAGE);
            return;
        }
        final var current = this.themeService.current();
        final var next = current.colorMode().next();
        this.saveInProgress = true;
        this.setEnabled(false);

        AsyncDataLoader.load(() -> {
            this.themeService.saveForSession(session, credentials, null, next);
            return next;
        }, this, ignored -> {
            this.themeService.applyColorMode(ui);
            this.updateButton();
            this.finishSave();
        }, this::finishSave, "Could not save the colour mode");
    }

    private void finishSave() {
        this.saveInProgress = false;
        this.setEnabled(true);
    }

    private void updateButton() {
        final var colorMode = this.themeService.current().colorMode();
        final var icon = switch (colorMode) {
            case LIGHT -> LineAwesomeIcon.SUN_SOLID;
            case DARK -> LineAwesomeIcon.MOON_SOLID;
            case SYSTEM -> LineAwesomeIcon.DESKTOP_SOLID;
        };
        final var label = "Switch to " + colorMode.next().getDisplayName();
        this.setIcon(icon.create());
        this.setTooltipText(label);
        this.getElement().setAttribute("aria-label", label);
    }
}
