package de.vptr.aimathtutor.view.admin;

import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.GlassAccentBar;
import de.vptr.aimathtutor.component.dashboard.DashboardKpiCard;
import de.vptr.aimathtutor.dto.AiInteractionViewDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying detailed information about a specific student session.
 */
@Route(value = "admin/session/:sessionId", layout = AdminMainLayout.class)
@PageTitle("Session Details - AI Math Tutor")
@SuppressWarnings("NullAway")
public class AdminSessionView extends AbstractAdminView {

    @Inject
    private transient AnalyticsService analyticsService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private transient String sessionId;
    private transient StudentSessionViewDto session;

    private transient Span headerSubtitle;
    private transient Span statusBadge;

    private transient DashboardKpiCard actionsCard;
    private transient DashboardKpiCard correctActionsCard;
    private transient DashboardKpiCard successRateCard;
    private transient DashboardKpiCard hintsCard;

    private transient DashboardKpiCard studentCard;
    private transient DashboardKpiCard exerciseCard;
    private transient DashboardKpiCard startTimeCard;
    private transient DashboardKpiCard endTimeCard;

    private transient DashboardKpiCard finalExpressionCard;

    private transient Grid<AiInteractionViewDto> interactionsGrid;

    /**
     * Constructs the AdminSessionView with scrollable full-width layout.
     */
    public AdminSessionView() {
        this.setWidthFull();
        this.setPadding(true);
        this.setSpacing(true);
        this.getStyle().set("overflow-y", "auto");
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.sessionId = event.getRouteParameters().get("sessionId").orElse(null);

        if (this.sessionId == null) {
            event.forwardTo(AdminSessionsView.class);
            return;
        }

        this.buildUi();
        this.loadSessionDetails();
    }

    private void buildUi() {
        this.removeAll();

        // Header
        final var title = new H2("Session Details");
        title.getStyle().set("margin-top", "0").set("margin-bottom", "4px").set("font-size", "22px")
                .set("font-weight", "700").set("color", "var(--lumo-header-text-color)").set("letter-spacing", "-0.3px")
                .set("display", "inline");

        this.statusBadge = new Span("Loading");
        this.statusBadge.getElement().getThemeList().add("badge");
        this.statusBadge.getElement().getThemeList().add("contrast");

        final var titleRow = new Div(title, this.statusBadge);
        titleRow.getStyle().set("display", "flex").set("align-items", "center").set("gap", "12px").set("margin-bottom",
                "4px");
        this.add(titleRow);

        this.headerSubtitle = new Span("Loading session information...");
        this.headerSubtitle.getStyle().set("font-size", "13px").set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "8px").set("display", "block");
        this.add(this.headerSubtitle);

        // Row 1: stats KPI cards
        this.actionsCard = new DashboardKpiCard("Total Actions", "—");
        this.correctActionsCard = new DashboardKpiCard("Correct Actions", "—");
        this.successRateCard = new DashboardKpiCard("Success Rate", "—");
        this.hintsCard = new DashboardKpiCard("Hints Used", "—");

        final var statsRow = new Div(this.actionsCard, this.correctActionsCard, this.successRateCard, this.hintsCard);
        statsRow.setWidthFull();
        statsRow.getStyle().set("display", "grid").set("grid-template-columns", "repeat(4, 1fr)").set("gap", "14px");
        this.add(statsRow);

        // Row 2: session context KPI cards
        this.studentCard = new DashboardKpiCard("Student", "—");
        this.exerciseCard = new DashboardKpiCard("Exercise", "—");
        this.startTimeCard = new DashboardKpiCard("Start Time", "—");
        this.endTimeCard = new DashboardKpiCard("End Time", "—");

        final var contextRow = new Div(this.studentCard, this.exerciseCard, this.startTimeCard, this.endTimeCard);
        contextRow.setWidthFull();
        contextRow.getStyle().set("display", "grid").set("grid-template-columns", "repeat(4, 1fr)").set("gap", "14px");
        this.add(contextRow);

        // Row 3: final expression (single full-width card)
        this.finalExpressionCard = new DashboardKpiCard("Final Expression", "—");
        final var expressionRow = new Div(this.finalExpressionCard);
        expressionRow.setWidthFull();
        this.add(expressionRow);

        this.add(this.buildGridCard());
    }

    private VerticalLayout buildGridCard() {
        final var card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidthFull();
        applyGlassCardStyle(card);

        final var accentBar = buildAccentBar();
        final var cardTitle = new Span("Interactions & Feedback");
        cardTitle.getStyle().set("font-size", "13px").set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)").set("letter-spacing", "0.3px")
                .set("padding", "12px 16px 4px").set("display", "block");

        this.interactionsGrid = new Grid<>(AiInteractionViewDto.class, false);
        this.interactionsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.interactionsGrid.setWidthFull();
        this.interactionsGrid.getStyle().set("padding", "0 8px 8px");

        this.interactionsGrid.addColumn(interaction -> this.dateTimeFormatter.formatDateTime(interaction.created))
                .setHeader("Time").setFlexGrow(0).setWidth(AppConstants.GRID_ACTION_WIDTH);

        this.interactionsGrid.addColumn(interaction -> {
            if (interaction.studentMessage != null && !interaction.studentMessage.isEmpty()) {
                return "Student";
            } else if (interaction.feedbackMessage != null && !interaction.feedbackMessage.isEmpty()) {
                return "AI Tutor";
            }
            return "Event";
        }).setHeader("Source").setFlexGrow(0).setWidth("100px");

        this.interactionsGrid.addColumn(interaction -> {
            if (interaction.studentMessage != null && !interaction.studentMessage.isEmpty()) {
                return interaction.studentMessage;
            } else if (interaction.feedbackMessage != null && !interaction.feedbackMessage.isEmpty()) {
                return interaction.feedbackMessage;
            } else if (interaction.expressionBefore != null && interaction.expressionAfter != null) {
                return interaction.expressionBefore + " → " + interaction.expressionAfter;
            }
            return "";
        }).setHeader("Message").setFlexGrow(2);

        this.interactionsGrid.addColumn(interaction -> interaction.feedbackType != null ? interaction.feedbackType : "")
                .setHeader("Feedback Type").setFlexGrow(0).setWidth("100px");

        card.add(accentBar, cardTitle, this.interactionsGrid);
        GlassAccentBar.addHoverEffect(card);
        return card;
    }

    private static void applyGlassCardStyle(final VerticalLayout card) {
        final String bg = "linear-gradient(135deg, var(--lumo-base-color),"
                + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
        card.getStyle().set("background", bg).set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                .set("transition", "all 0.25s cubic-bezier(0.4, 0, 0.2, 1)").set("overflow", "hidden");
    }

    private static Div buildAccentBar() {
        final var bar = new Div();
        bar.getStyle().set("height", "3px").set("width", "100%")
                .set("background",
                        "linear-gradient(90deg, var(--lumo-primary-color),"
                                + " var(--lumo-primary-color-50pct), var(--lumo-primary-color-10pct))")
                .set("flex-shrink", "0");
        return bar;
    }

    private void loadSessionDetails() {
        AsyncDataLoader.load(() -> {
            this.session = this.analyticsService.getSessionBySessionId(this.sessionId);
            if (this.session == null) {
                return null;
            }
            return this.analyticsService.getAiInteractionsBySession(this.sessionId);
        }, this, interactions -> {
            if (interactions == null) {
                NotificationUtil.showError("Session not found");
                this.getUI().ifPresent(ui -> ui.navigate(AdminSessionsView.class));
                return;
            }
            this.updateSessionInfo();
            this.updateInteractionsGrid(interactions);
        }, "Failed to load session details");
    }

    private void updateSessionInfo() {
        if (this.session == null) {
            return;
        }

        final var duration = this.session.getFormattedDuration() != null ? this.session.getFormattedDuration() : "N/A";
        this.headerSubtitle.setText("(" + duration + ") Session ID: " + this.session.sessionId);

        this.statusBadge.setText(this.session.completed ? "Completed" : "In Progress");
        this.statusBadge.getElement().getThemeList().clear();
        this.statusBadge.getElement().getThemeList().add("badge");
        this.statusBadge.getElement().getThemeList().add(this.session.completed ? "success" : "contrast");

        this.actionsCard.setValue(String.valueOf(this.session.actionsCount));
        this.correctActionsCard.setValue(String.valueOf(this.session.correctActions));
        this.successRateCard.setValue(this.session.getSuccessRatePercentage());
        this.hintsCard.setValue(String.valueOf(this.session.hintsUsed));

        this.studentCard.setValue(this.session.username != null ? this.session.username : "N/A");
        this.exerciseCard.setValue(this.session.exerciseTitle != null ? this.session.exerciseTitle : "N/A");
        this.startTimeCard.setValue(
                this.session.startTime != null ? this.dateTimeFormatter.formatDateTime(this.session.startTime) : "N/A");
        this.endTimeCard.setValue(this.session.endTime != null
                ? this.dateTimeFormatter.formatDateTime(this.session.endTime) : "In Progress");
        this.finalExpressionCard.setValue(this.session.finalExpression != null ? this.session.finalExpression : "—");
    }

    private void updateInteractionsGrid(final List<AiInteractionViewDto> interactions) {
        if (this.interactionsGrid != null) {
            this.interactionsGrid.setItems(interactions);
        }
    }
}
