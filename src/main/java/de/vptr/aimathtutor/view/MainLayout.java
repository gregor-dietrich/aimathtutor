package de.vptr.aimathtutor.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;

import de.vptr.aimathtutor.component.NavigationTabs;
import de.vptr.aimathtutor.component.button.AdminViewButton;
import de.vptr.aimathtutor.component.button.LogoutButton;
import de.vptr.aimathtutor.component.button.ThemeToggleButton;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ThemeService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.view.admin.AdminHomeView;
import jakarta.inject.Inject;

public class MainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(MainLayout.class);

    private Button adminViewButton;
    private Button settingsButton;
    private Button logoutButton;
    private Tabs navigationTabs;

    @Inject
    AuthService authService;

    @Inject
    ThemeService themeService;

    @Inject
    UserRankService userRankService;

    private HorizontalLayout topBar;
    private HorizontalLayout rightSide;
    private boolean initialized = false;

    /**
     * Get the shared top bar for views that need to add additional components
     */
    public HorizontalLayout getTopBar() {
        return this.topBar;
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.updateLogoutButtonVisibility();
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.initialized) {
            this.initializeLayout();
            this.initialized = true;
        }

        // Apply current theme on every navigation
        this.themeService.applyTheme(this.themeService.getCurrentTheme());

        final var targetView = event.getNavigationTarget();

        LOG.trace("MainLayout.beforeEnter - Target: {}", targetView.getSimpleName());

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

        LOG.trace("All checks passed for {}", targetView.getSimpleName());
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

        this.createTopBar();
    }

    private void createTopBar() {
        this.topBar = new HorizontalLayout();
        this.topBar.setWidthFull();
        this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.topBar.setPadding(true);

        // Create navigation menu
        this.navigationTabs = new NavigationTabs();

        // Create right side components container
        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);

        final var themeToggle = new ThemeToggleButton(this.themeService);
        this.rightSide.add(themeToggle);

        this.topBar.add(this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    private void showNavigationTabs() {
        if (this.navigationTabs != null && this.topBar != null) {
            if (!this.topBar.getChildren().anyMatch(component -> component == this.navigationTabs)) {
                this.topBar.removeAll();
                this.topBar.add(this.navigationTabs, this.rightSide);
                this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
            }
        }
    }

    private void hideNavigationTabs() {
        if (this.navigationTabs != null && this.topBar != null) {
            this.topBar.removeAll();
            this.topBar.add(this.rightSide);
            this.topBar.setJustifyContentMode(JustifyContentMode.END);
        }
    }

    private void addButtonsToTopBar() {
        // Check if buttons already exist, if so don't add them again
        if (this.logoutButton != null && this.rightSide.getChildren().anyMatch(c -> c == this.logoutButton)) {
            return;
        }

        // Only create admin button if user is authenticated and has admin:view
        // permission
        final var userRank = this.userRankService.getCurrentUserRank();
        if (userRank != null && userRank.canAdminView()) {
            this.adminViewButton = new AdminViewButton(
                    e -> this.getUI().ifPresent(ui -> ui.navigate(AdminHomeView.class)));
        } else {
            this.adminViewButton = null;
        }

        // Settings button
        this.settingsButton = new Button("Settings");
        this.settingsButton.addClickListener(e -> this.getUI().ifPresent(ui -> ui.navigate(UserSettingsView.class)));

        this.logoutButton = new LogoutButton(e -> {
            this.authService.logout();
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        // Add buttons
        final var componentCount = this.rightSide.getComponentCount();
        if (componentCount > 0) {
            this.rightSide.addComponentAtIndex(componentCount - 1, this.logoutButton);
            this.rightSide.addComponentAtIndex(componentCount - 1, this.settingsButton);
            if (this.adminViewButton != null) {
                this.rightSide.addComponentAtIndex(componentCount - 1, this.adminViewButton);
            }
        } else {
            if (this.adminViewButton != null) {
                this.rightSide.add(this.adminViewButton);
            }
            this.rightSide.add(this.settingsButton);
            this.rightSide.add(this.logoutButton);
        }
    }

    private void removeButtonsFromTopBar() {
        if (this.rightSide != null) {
            if (this.logoutButton != null) {
                this.rightSide.remove(this.logoutButton);
                this.logoutButton = null;
            }
            if (this.settingsButton != null) {
                this.rightSide.remove(this.settingsButton);
                this.settingsButton = null;
            }
            if (this.adminViewButton != null) {
                this.rightSide.remove(this.adminViewButton);
                this.adminViewButton = null;
            }
        }
    }
}
