package de.vptr.aimathtutor.view;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;

import de.vptr.aimathtutor.component.NavigationTabs;
import de.vptr.aimathtutor.component.TopBar;
import de.vptr.aimathtutor.component.button.AdminViewButton;
import de.vptr.aimathtutor.component.button.LogoutButton;
import de.vptr.aimathtutor.component.button.SettingsViewButton;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ThemeService;
import de.vptr.aimathtutor.service.UserRankService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.inject.Inject;

/**
 * Main application layout wrapping views with navigation and header.
 */
public class MainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(MainLayout.class);

    private Button adminViewButton;
    private Button settingsButton;
    private Button logoutButton;
    private Tabs navigationTabs;

    @Inject
    private transient AuthService authService;

    @Inject
    private transient ThemeService themeService;

    @Inject
    private transient UserRankService userRankService;

    private TopBar topBar;
    private boolean initialized = false;

    /**
     * Get the shared top bar for views that need to add additional components
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Top bar is intended to be shared and extended by child views")
    public HorizontalLayout getTopBar() {
        return this.topBar;
    }

    /**
     * Attaches event listener when layout is added to the UI tree.
     * Updates logout button visibility based on authentication state.
     *
     * @param attachEvent the attach event containing lifecycle information
     */
    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.updateLogoutButtonVisibility();
    }

    /**
     * Called before navigation occurs. Initializes layout on first entry, applies
     * theme,
     * checks authentication, and shows/hides navigation tabs based on target view.
     *
     * @param event the before enter navigation event
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.initialized) {
            this.initializeLayout();
            this.initialized = true;
        }

        // Apply current theme on every navigation
        this.themeService.applyTheme(this.themeService.getCurrentTheme());

        final var targetView = event.getNavigationTarget();

        LOG.tracef("MainLayout.beforeEnter - Target: %s",  targetView.getSimpleName());

        // No backend health check needed since we're using direct database access
        LOG.trace("User authenticated, building UI");

        // Skip auth check for the login view
        if (targetView == LoginView.class) {
            LOG.trace("Navigating to login view, skipping authentication check");
            this.removeButtonsFromTopBar(); // Hide logout button on login view
            this.hideNavigationTabs(); // Hide navigation tabs on login view
            return;
        }

        // Check authentication for all other views
        if (!this.authService.isAuthenticated()) {
            LOG.trace("User not authenticated, redirecting to login");
            event.forwardTo(LoginView.class);
            return;
        }

        // User is authenticated - show logout button and navigation tabs
        this.addButtonsToTopBar();
        this.showNavigationTabs();

        LOG.tracef("All checks passed for %s",  targetView.getSimpleName());
    }

    private void updateLogoutButtonVisibility() {
        if (this.authService.isAuthenticated()) {
            // Check current route to determine if logout button should be shown
            this.getUI().ifPresent(ui -> {
                final var location = ui.getInternals().getActiveViewLocation();
                if (location != null) {
                    final var path = location.getPath();
                    // Don't show logout button on login or error views
                    if (!"login".equals(path) && !"backend-error".equals(path)) {
                        // Skip adding buttons if this is an admin route (AdminMainLayout will handle
                        // it)
                        if (!path.startsWith("admin")) {
                            this.addButtonsToTopBar();
                            this.showNavigationTabs();
                        }
                    } else {
                        this.removeButtonsFromTopBar();
                        this.hideNavigationTabs();
                    }
                }
            });
        } else {
            this.removeButtonsFromTopBar();
            this.hideNavigationTabs();
        }
    }

    private void initializeLayout() {
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);

        this.navigationTabs = new NavigationTabs();
        this.topBar = new TopBar(this.themeService);
        this.addComponentAsFirst(this.topBar);
    }

    private void showNavigationTabs() {
        if (this.navigationTabs != null && this.topBar != null) {
            this.topBar.setLeftContent(this.navigationTabs);
        }
    }

    private void hideNavigationTabs() {
        if (this.topBar != null) {
            this.topBar.setLeftContent(null);
        }
    }

    private void addButtonsToTopBar() {
        // Avoid adding twice
        if (this.logoutButton != null
                && this.topBar.getRightSide().getChildren().anyMatch(c -> c == this.logoutButton)) {
            return;
        }

        // Only create admin button if user has admin:view permission
        final var userRank = this.userRankService.getCurrentUserRank();
        if (userRank != null && userRank.canAdminView()) {
            this.adminViewButton = new AdminViewButton(
                    e -> this.getUI().ifPresent(ui -> ui.navigate("admin/dashboard")));
        } else {
            this.adminViewButton = null;
        }

        this.settingsButton = new SettingsViewButton(e -> {
            final var currentLocation = UI.getCurrent().getInternals().getActiveViewLocation();
            if (currentLocation == null || !"settings".equals(currentLocation.getPath())) {
                this.getUI().ifPresent(ui -> ui.navigate(UserSettingsView.class));
            }
        });

        this.logoutButton = new LogoutButton(e -> {
            this.authService.logout();
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        this.topBar.addRightSideButtons(this.adminViewButton, this.settingsButton, this.logoutButton);
    }

    private void removeButtonsFromTopBar() {
        if (this.topBar == null) {
            return;
        }
        this.topBar.removeFromRightSide(this.adminViewButton, this.settingsButton, this.logoutButton);
        this.adminViewButton = null;
        this.settingsButton = null;
        this.logoutButton = null;
    }
}
