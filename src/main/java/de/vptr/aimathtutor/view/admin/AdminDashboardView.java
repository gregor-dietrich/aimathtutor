package de.vptr.aimathtutor.view.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin dashboard for displaying high-level analytics and overview statistics.
 * Shows total sessions, active students, completed sessions, and recent
 * activity.
 */
@Route(value = "admin/dashboard", layout = AdminMainLayout.class)
@PageTitle("Admin Dashboard - AI Math Tutor")
public class AdminDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AdminDashboardView.class);

    @Inject
    private transient AuthService authService;

    @Inject
    private transient AnalyticsService analyticsService;

    // Map to store references to stat card value spans for efficient updates
    private final transient Map<String, Span> statCardValues = new HashMap<>();

    public AdminDashboardView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
        // transient statCardValues is initialized inline; no further action required
    }

    /**
     * TODO: Document beforeEnter().
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo("login");
            return;
        }

        this.buildUi();
        this.loadDashboardData();
    }

    private void buildUi() {
        this.removeAll();
        this.statCardValues.clear();

        // Title
        final var title = new H2("Dashboard Overview");
        this.add(title);

        // Statistics cards container
        final var cardsContainer = new HorizontalLayout();
        cardsContainer.setSpacing(true);
        cardsContainer.setWidthFull();

        // Add placeholder cards that will be updated with data
        final var totalSessionsCard = this.createStatCard("Total Sessions", "Loading...");
        final var completedSessionsCard = this.createStatCard("Completed Sessions", "Loading...");
        final var activeStudentsCard = this.createStatCard("Active Students (Last 7 Days)", "Loading...");
        final var todaySessionsCard = this.createStatCard("Today's Sessions", "Loading...");

        cardsContainer.add(totalSessionsCard, completedSessionsCard, activeStudentsCard, todaySessionsCard);
        cardsContainer.setFlexGrow(1, totalSessionsCard, completedSessionsCard, activeStudentsCard, todaySessionsCard);

        this.add(cardsContainer);
    }

    private void loadDashboardData() {
        CompletableFuture.runAsync(() -> {
            try {
                final var totalSessions = this.analyticsService.getTotalSessionsCount();
                final var completedSessions = this.analyticsService.getCompletedSessionsCount();
                final var activeStudents = this.analyticsService.getActiveStudentsCount();
                final var todaySessions = this.analyticsService.getTodaySessionsCount();

                this.getUI().ifPresent(ui -> ui.access(() -> {
                    this.updateStatCard("Total Sessions", String.valueOf(totalSessions));
                    this.updateStatCard("Completed Sessions", String.valueOf(completedSessions));
                    this.updateStatCard("Active Students (Last 7 Days)", String.valueOf(activeStudents));
                    this.updateStatCard("Today's Sessions", String.valueOf(todaySessions));
                }));

            } catch (final Exception e) {
                LOG.error("Error loading dashboard data", e);
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    NotificationUtil.showError("Failed to load dashboard data");
                }));
            }
        });
    }

    private VerticalLayout createStatCard(final String title, final String value) {
        final var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("background-color", "var(--lumo-contrast-5pct)");

        final var titleLabel = new Span(title);
        titleLabel.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("font-weight", "500");

        final var valueLabel = new Span(value);
        valueLabel.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)");

        // Store reference to value label for efficient updates
        this.statCardValues.put(title, valueLabel);

        card.add(titleLabel, valueLabel);
        return card;
    }

    private void updateStatCard(final String title, final String value) {
        // Direct lookup in map for O(1) access instead of tree traversal
        final var valueLabel = this.statCardValues.get(title);
        if (valueLabel != null) {
            valueLabel.setText(value);
        }
    }

    /**
     * Ensure transient fields are initialized after deserialization.
     */
    private void readObject(final java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Reinitialize transient runtime map used for UI updates
        if (this.statCardValues == null) {
            // It's safe to reassign a new HashMap here because this field is only used at
            // runtime
            final Map<String, Span> m = new HashMap<>();
            // assign to backing field via reflection isn't necessary since field is
            // non-final
            // Use a straightforward approach to set it
            try {
                final java.lang.reflect.Field f = AdminDashboardView.class.getDeclaredField("statCardValues");
                f.setAccessible(true);
                f.set(this, m);
            } catch (final Exception e) {
                // fallback: best-effort initialization
            }
        }
    }
}
