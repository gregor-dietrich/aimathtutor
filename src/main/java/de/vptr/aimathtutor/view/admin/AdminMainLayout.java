package de.vptr.aimathtutor.view.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;

import de.vptr.aimathtutor.component.AdminNavigationTabs;
import de.vptr.aimathtutor.component.button.LogoutButton;
import de.vptr.aimathtutor.component.button.ThemeToggleButton;
import de.vptr.aimathtutor.component.button.UserViewButton;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ThemeService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.view.LessonsView;
import de.vptr.aimathtutor.view.LoginView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.inject.Inject;

public class AdminMainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AdminMainLayout.class);

    private transient Button userViewButton;
    private transient Button logoutButton;
    private transient Tabs navigationTabs;

    @Inject
    private transient AuthService authService;

    @Inject
    private transient ThemeService themeService;

    @Inject
    private transient UserRankService userRankService;

    private transient HorizontalLayout topBar;
    private transient HorizontalLayout rightSide;
    private transient HorizontalLayout mainLayout;
    private transient VerticalLayout sidebar;
    private transient VerticalLayout contentArea;
    private transient boolean initialized = false;

    /**
     * Get the shared top bar for views that need to add additional components
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Top bar is intentionally exposed for child views to append components")
    public HorizontalLayout getTopBar() {
        return this.topBar;
    }

    @Override
    public void showRouterLayoutContent(final HasElement content) {
        if (this.contentArea != null) {
            this.contentArea.getElement().removeAllChildren();
            this.contentArea.getElement().appendChild(content.getElement());
        } else {
            // Fallback to default behavior if content area is not initialized
            this.getElement().appendChild(content.getElement());
        }
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.updateLogoutButtonVisibility();
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        // Apply current theme on every navigation
        this.themeService.applyTheme(this.themeService.getCurrentTheme());

        final var targetView = event.getNavigationTarget();

        LOG.trace("MainLayout.beforeEnter - Target: {}", targetView.getSimpleName());

        // Skip auth check for the login view
        if (targetView == LoginView.class) {
            LOG.trace("Navigating to login view, skipping authentication check");
            if (this.initialized) {
                this.removeButtonsFromTopBar(); // Hide logout button on login view
                this.hideNavigationTabs(); // Hide navigation tabs on login view
            }
            return;
        }

        // Check authentication for all other views
        if (!this.authService.isAuthenticated()) {
            LOG.trace("User not authenticated, redirecting to login");
            event.forwardTo(LoginView.class);
            return;
        }

        // Check admin permission for admin views
        final var userRank = this.userRankService.getCurrentUserRank();
        if (userRank == null || !userRank.canAdminView()) {
            LOG.trace("User does not have admin:view permission, redirecting to home");
            event.forwardTo("");
            return;
        }

        // Initialize layout only after all security checks pass
        if (!this.initialized) {
            this.initializeLayout();
            this.initialized = true;
        }

        this.addButtonsToTopBar();
        this.showNavigationTabs();
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
                        this.addButtonsToTopBar();
                        this.showNavigationTabs();
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
        this.createMainLayout();
    }

    private void createTopBar() {
        this.topBar = new HorizontalLayout();
        this.topBar.setWidthFull();
        this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.topBar.setPadding(true);

        // Create left side with title
        final var leftSide = new HorizontalLayout();
        final var title = new H1("AI Math Tutor - Admin Panel");
        title.getStyle().set("margin", "0");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        leftSide.add(title);

        // Create right side components container
        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);

        final var themeToggle = new ThemeToggleButton(this.themeService);
        this.rightSide.add(themeToggle);

        this.topBar.add(leftSide, this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    private void createMainLayout() {
        // Create the main horizontal layout that will contain sidebar and content
        this.mainLayout = new HorizontalLayout();
        this.mainLayout.setSizeFull();
        this.mainLayout.setPadding(false);
        this.mainLayout.setSpacing(false);

        // Create sidebar
        this.sidebar = new VerticalLayout();
        this.sidebar.setWidth("250px");
        this.sidebar.setHeightFull();
        this.sidebar.setPadding(true);
        this.sidebar.setSpacing(false);
        this.sidebar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        this.sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        // Create navigation tabs
        final var userRank = this.userRankService.getCurrentUserRank();
        this.navigationTabs = new AdminNavigationTabs(userRank);
        this.sidebar.add(this.navigationTabs);

        // Create content area where the actual views will be displayed
        this.contentArea = new VerticalLayout();
        this.contentArea.setSizeFull();
        this.contentArea.setPadding(false);
        this.contentArea.setSpacing(false);

        // Add sidebar and content area to main layout
        this.mainLayout.add(this.sidebar, this.contentArea);
        this.mainLayout.setFlexGrow(0, this.sidebar);
        this.mainLayout.setFlexGrow(1, this.contentArea);

        this.add(this.mainLayout);
    }

    private void showNavigationTabs() {
        if (this.sidebar != null && this.mainLayout != null) {
            this.sidebar.setVisible(true);
        }
    }

    private void hideNavigationTabs() {
        if (this.sidebar != null) {
            this.sidebar.setVisible(false);
        }
    }

    private void addButtonsToTopBar() {
        if (this.rightSide != null) {
            // Remove existing buttons if present
            if (this.logoutButton != null) {
                this.rightSide.remove(this.logoutButton);
            }
            if (this.userViewButton != null) {
                this.rightSide.remove(this.userViewButton);
            }

            // Create new logout button
            this.logoutButton = new LogoutButton(e -> {
                this.authService.logout();
                this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            });

            // Create new AdminView button
            this.userViewButton = new UserViewButton(e -> this.getUI().ifPresent(ui -> ui.navigate(LessonsView.class)));

            // Add buttons
            final var componentCount = this.rightSide.getComponentCount();
            if (componentCount > 0) {
                this.rightSide.addComponentAtIndex(componentCount - 1, this.logoutButton);
                this.rightSide.addComponentAtIndex(componentCount - 1, this.userViewButton);
            } else {
                this.rightSide.add(this.logoutButton);
                this.rightSide.add(this.userViewButton);
            }
        }
    }

    private void removeButtonsFromTopBar() {
        if (this.rightSide != null) {
            if (this.logoutButton != null) {
                this.rightSide.remove(this.logoutButton);
                this.logoutButton = null;
            }
            if (this.userViewButton != null) {
                this.rightSide.remove(this.userViewButton);
                this.userViewButton = null;
            }
        }
    }
}
