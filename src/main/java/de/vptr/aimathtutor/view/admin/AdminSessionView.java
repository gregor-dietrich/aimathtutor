package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIInteractionViewDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying detailed information about a specific student
 * session.
 * Shows session timeline, AI feedback, and actions taken.
 */
@Route(value = "admin/session/:sessionId", layout = AdminMainLayout.class)
@PageTitle("Session Details - AI Math Tutor")
public class AdminSessionView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AdminSessionView.class);

    @Inject
    private transient AuthService authService;

    @Inject
    private transient AnalyticsService analyticsService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private transient String sessionId;
    private transient StudentSessionViewDto session;
    private transient VerticalLayout sessionInfoLayout;
    private transient Grid<AIInteractionViewDto> interactionsGrid;

    public AdminSessionView() {
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
            event.forwardTo(AdminSessionsView.class);
            return;
        }

        this.buildUI();
        this.loadSessionDetails();
    }

    private void buildUI() {
        this.removeAll();

        // Back button
        final var backButton = new Button("← Back to Sessions", e -> {
            this.getUI().ifPresent(ui -> ui.navigate(AdminSessionsView.class));
        });
        backButton.getStyle().set("margin-bottom", "20px");
        this.add(backButton);

        // Title
        final var title = new H2("Session Details");
        this.add(title);

        // Placeholder for session info - will be populated with cards
        this.sessionInfoLayout = new VerticalLayout();
        this.sessionInfoLayout.setPadding(false);
        this.sessionInfoLayout.setSpacing(true);
        this.sessionInfoLayout.add(new Paragraph("Loading session information..."));
        this.add(this.sessionInfoLayout);

        // Interactions grid
        final var interactionsTitle = new H2("Interactions & Feedback");
        this.add(interactionsTitle);

        this.interactionsGrid = new Grid<>(AIInteractionViewDto.class, false);
        this.interactionsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.interactionsGrid.setSizeFull();
        this.interactionsGrid.setHeight("400px");

        // Configure columns to show conversational interaction
        this.interactionsGrid.addColumn(interaction -> this.dateTimeFormatter.formatDateTime(interaction.timestamp))
                .setHeader("Time")
                .setFlexGrow(0)
                .setWidth("150px");

        this.interactionsGrid.addColumn(interaction -> {
            // Identify if this is a student message or AI response
            if (interaction.studentMessage != null && !interaction.studentMessage.isEmpty()) {
                return "Student";
            } else if (interaction.feedbackMessage != null && !interaction.feedbackMessage.isEmpty()) {
                return "AI Tutor";
            }
            return "Event";
        }).setHeader("Source")
                .setFlexGrow(0)
                .setWidth("100px");

        // Show message content (student message or AI feedback)
        this.interactionsGrid.addColumn(interaction -> {
            if (interaction.studentMessage != null && !interaction.studentMessage.isEmpty()) {
                return interaction.studentMessage;
            } else if (interaction.feedbackMessage != null && !interaction.feedbackMessage.isEmpty()) {
                return interaction.feedbackMessage;
            } else if (interaction.expressionBefore != null && interaction.expressionAfter != null) {
                return interaction.expressionBefore + " → " + interaction.expressionAfter;
            }
            return "";
        }).setHeader("Message")
                .setFlexGrow(2);

        // Show feedback type if available
        this.interactionsGrid.addColumn(interaction -> interaction.feedbackType != null ? interaction.feedbackType : "")
                .setHeader("Feedback Type")
                .setFlexGrow(0)
                .setWidth("100px");

        this.add(this.interactionsGrid);
    }

    private void loadSessionDetails() {
        CompletableFuture.runAsync(() -> {
            try {
                this.session = this.analyticsService.getSessionBySessionId(this.sessionId);
                if (this.session == null) {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        NotificationUtil.showError("Session not found");
                        ui.navigate(AdminSessionsView.class);
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
        if (this.session == null || this.sessionInfoLayout == null) {
            return;
        }

        // Clear existing content and populate with session data in card format
        this.sessionInfoLayout.removeAll();

        // Main info card
        final var mainCard = new VerticalLayout();
        mainCard.setPadding(true);
        mainCard.setSpacing(true);
        mainCard.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("background-color", "var(--lumo-contrast-5pct)");

        // Header with student and exercise info
        final var header = new H3(this.session.username + " - " + this.session.exerciseTitle);
        mainCard.add(header);

        // Session status badge
        final var statusSpan = new Span(this.session.completed ? "✓ Completed" : "◌ Not Completed");
        statusSpan.getElement().getThemeList().add("badge");
        if (this.session.completed) {
            statusSpan.getElement().getThemeList().add("success");
        } else {
            statusSpan.getElement().getThemeList().add("contrast");
        }
        mainCard.add(statusSpan);

        // Info grid
        final var infoGrid = new VerticalLayout();
        infoGrid.setSpacing(false);
        infoGrid.setPadding(false);

        infoGrid.add(new Paragraph("Session ID: " + this.session.sessionId));
        infoGrid.add(new Paragraph("Start Time: " + this.dateTimeFormatter.formatDateTime(this.session.startTime)));
        infoGrid.add(new Paragraph(
                "End Time: "
                        + (this.session.endTime != null ? this.dateTimeFormatter.formatDateTime(this.session.endTime)
                                : "Not yet completed")));
        if (this.session.getFormattedDuration() != null) {
            infoGrid.add(new Paragraph("Duration: " + this.session.getFormattedDuration()));
        } else {
            infoGrid.add(new Paragraph("Duration: Not available"));
        }

        mainCard.add(infoGrid);

        // Performance metrics section
        final var metricsCard = new VerticalLayout();
        metricsCard.setPadding(true);
        metricsCard.setSpacing(true);
        metricsCard.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("background-color", "var(--lumo-contrast-5pct)");

        metricsCard.add(new H3("Performance Metrics"));

        final var metricsGrid = new VerticalLayout();
        metricsGrid.setSpacing(false);
        metricsGrid.setPadding(false);

        metricsGrid.add(new Paragraph("Total Actions: " + this.session.actionsCount));
        metricsGrid.add(new Paragraph("Correct Actions: " + this.session.correctActions));
        metricsGrid.add(new Paragraph("Success Rate: " + this.session.getSuccessRatePercentage()));
        metricsGrid.add(new Paragraph("Hints Used: " + this.session.hintsUsed));
        metricsGrid.add(new Paragraph(
                "Final Expression: "
                        + (this.session.finalExpression != null ? this.session.finalExpression : "N/A")));

        metricsCard.add(metricsGrid);

        this.sessionInfoLayout.add(mainCard, metricsCard);
    }

    private void updateInteractionsGrid(final List<AIInteractionViewDto> interactions) {
        // Direct update using stored grid reference
        if (this.interactionsGrid != null) {
            this.interactionsGrid.setItems(interactions);
        }
    }
}
