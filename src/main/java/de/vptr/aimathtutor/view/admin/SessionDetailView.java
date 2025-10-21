package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIInteractionViewDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying detailed information about a specific student
 * session.
 * Shows session timeline, AI feedback, and actions taken.
 */
@Route(value = "admin/session/:sessionId", layout = AdminMainLayout.class)
@PageTitle("Session Details - AI Math Tutor")
public class SessionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(SessionDetailView.class);

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    private String sessionId;
    private StudentSessionViewDto session;

    public SessionDetailView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo("login");
            return;
        }

        // Extract session ID from route parameters
        this.sessionId = event.getRouteParameters().get("sessionId").orElse(null);

        if (this.sessionId == null) {
            event.forwardTo(StudentSessionsView.class);
            return;
        }

        this.buildUI();
        this.loadSessionDetails();
    }

    private void buildUI() {
        this.removeAll();

        // Back button
        final var backButton = new Button("← Back to Sessions", e -> {
            this.getUI().ifPresent(ui -> ui.navigate(StudentSessionsView.class));
        });
        backButton.getStyle().set("margin-bottom", "20px");
        this.add(backButton);

        // Title
        final var title = new H2("Session Details");
        this.add(title);

        // Placeholder for session info
        final var sessionInfoLayout = new VerticalLayout();
        sessionInfoLayout.setPadding(true);
        sessionInfoLayout.setSpacing(true);
        sessionInfoLayout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("background-color", "var(--lumo-contrast-5pct)");

        sessionInfoLayout.add(new Paragraph("Loading session information..."));
        this.add(sessionInfoLayout);

        // Interactions grid
        final var interactionsTitle = new H2("Interactions & Feedback");
        this.add(interactionsTitle);

        final var interactionsGrid = new Grid<>(AIInteractionViewDto.class, false);
        interactionsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        interactionsGrid.setSizeFull();
        interactionsGrid.setHeight("400px");

        // Configure columns
        interactionsGrid.addColumn(interaction -> interaction.timestamp)
                .setHeader("Time")
                .setFlexGrow(1);

        interactionsGrid.addColumn(interaction -> interaction.eventType)
                .setHeader("Event Type")
                .setFlexGrow(1);

        interactionsGrid.addColumn(interaction -> interaction.feedbackType)
                .setHeader("Feedback Type")
                .setFlexGrow(1);

        interactionsGrid
                .addColumn(interaction -> interaction.feedbackMessage != null ? interaction.feedbackMessage : "")
                .setHeader("Feedback")
                .setFlexGrow(2);

        this.add(interactionsGrid);
    }

    private void loadSessionDetails() {
        CompletableFuture.runAsync(() -> {
            try {
                this.session = this.analyticsService.getSessionBySessionId(this.sessionId);
                if (this.session == null) {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        NotificationUtil.showError("Session not found");
                        ui.navigate(StudentSessionsView.class);
                    }));
                    return;
                }

                final List<AIInteractionViewDto> interactions = this.analyticsService
                        .getAIInteractionsBySession(this.sessionId);

                this.getUI().ifPresent(ui -> ui.access(() -> {
                    this.updateSessionInfo();
                    this.updateInteractionsGrid(interactions);
                }));

            } catch (final Exception e) {
                LOG.error("Error loading session details", e);
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    NotificationUtil.showError("Failed to load session details");
                }));
            }
        });
    }

    private void updateSessionInfo() {
        if (this.session == null) {
            return;
        }

        // Find and update the session info layout
        final var sessionInfoLayout = new VerticalLayout();
        sessionInfoLayout.setPadding(true);
        sessionInfoLayout.setSpacing(true);
        sessionInfoLayout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("background-color", "var(--lumo-contrast-5pct)");

        sessionInfoLayout.add(
                new Paragraph("Session ID: " + this.session.sessionId),
                new Paragraph("Student: " + this.session.username),
                new Paragraph("Exercise: " + this.session.exerciseTitle),
                new Paragraph("Start Time: " + this.session.startTime),
                new Paragraph("End Time: " + (this.session.endTime != null ? this.session.endTime : "In progress")),
                new Paragraph("Duration: " + this.session.getFormattedDuration()),
                new Paragraph("Total Actions: " + this.session.actionsCount),
                new Paragraph("Correct Actions: " + this.session.correctActions),
                new Paragraph("Success Rate: " + this.session.getSuccessRatePercentage()),
                new Paragraph("Hints Used: " + this.session.hintsUsed),
                new Paragraph("Status: " + (this.session.completed ? "Completed ✓" : "In Progress")),
                new Paragraph(
                        "Final Expression: "
                                + (this.session.finalExpression != null ? this.session.finalExpression : "N/A")));

        // Replace the first session info layout (skip back button and title)
        final var children = this.getChildren().toList();
        if (children.size() > 2 && children.get(2) instanceof VerticalLayout) {
            this.replace(children.get(2), sessionInfoLayout);
        }
    }

    private void updateInteractionsGrid(final List<AIInteractionViewDto> interactions) {
        // Find and update the interactions grid
        this.getChildren()
                .filter(Grid.class::isInstance)
                .findFirst()
                .ifPresent(c -> {
                    @SuppressWarnings("unchecked")
                    final var grid = (Grid<AIInteractionViewDto>) c;
                    grid.setItems(interactions);
                });
    }
}
