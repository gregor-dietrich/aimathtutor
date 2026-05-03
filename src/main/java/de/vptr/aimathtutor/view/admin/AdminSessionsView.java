package de.vptr.aimathtutor.view.admin;

import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying all student sessions with filtering and detail
 * options.
 * Shows session information including student, exercise, duration, and
 * completion status.
 */
@Route(value = "admin/sessions", layout = AdminMainLayout.class)
@PageTitle("Student Sessions - AI Math Tutor")
public class AdminSessionsView extends AbstractAdminView {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSessionsView.class);
    @Inject
    private transient AnalyticsService analyticsService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private Grid<StudentSessionViewDto> grid;
    private TextField searchField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Button resetFiltersButton;

    /**
     * Constructs the AdminSessionsView with full size and padding.
     */
    public AdminSessionsView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Ensure authentication and prepare session listing before entering the
     * view.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.buildUi();
        this.loadSessions();
    }

    private void buildUi() {
        this.removeAll();

        // Title
        final var title = new H2("Student Sessions");
        this.add(title);

        // Search layout
        final var searchLayout = this.createSearchLayout();
        this.add(searchLayout);

        // Button layout
        final var buttonLayout = this.createButtonLayout();
        this.add(buttonLayout);

        // Create grid
        this.grid = new Grid<>(StudentSessionViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        // Make the username column clickable like a hyperlink
        this.grid.addComponentColumn(session -> {
            final var usernameSpan = new Span(session.username);
            usernameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            usernameSpan.getStyle().set("cursor", "pointer");
            usernameSpan.getStyle().set("width", "100%");
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(ignored -> UI.getCurrent().navigate("admin/session/" + session.sessionId));
            return usernameSpan;
        }).setHeader("Student")
                .setFlexGrow(1);

        this.grid.addColumn(session -> session.exerciseTitle)
                .setHeader("Exercise")
                .setFlexGrow(1);

        this.grid.addColumn(session -> this.dateTimeFormatter.formatDateTime(session.startTime))
                .setHeader("Start Time")
                .setWidth("180px").setFlexGrow(0);

        this.grid.addColumn(StudentSessionViewDto::getFormattedDuration)
                .setHeader("Duration")
                .setFlexGrow(0);

        this.grid.addColumn(session -> session.actionsCount)
                .setHeader("Actions")
                .setFlexGrow(0);

        this.grid.addColumn(StudentSessionViewDto::getSuccessRatePercentage)
                .setHeader("Success Rate")
                .setFlexGrow(1);

        this.grid.addColumn(session -> session.hintsUsed)
                .setHeader("Hints Used")
                .setFlexGrow(0);

        this.grid.addColumn(session -> session.completed ? "✓" : "✗")
                .setHeader("Completed")
                .setFlexGrow(0);

        this.add(this.grid);
    }

    /**
     * Create the search layout used to filter sessions by text and date.
     *
     * @return the search layout
     */
    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().isBlank()) {
                        this.loadSessions();
                    }
                },
                ignored -> this.searchSessions(),
                "Search by student or exercise...",
                "Search Sessions");

        this.searchField = searchLayout.getTextfield();

        // Date range filter for session start time
        final var dateFilterLayout = new DateFilterLayout(ignored -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // Add reset filters button
        this.resetFiltersButton = new Button("Reset Filters", ignored -> this.resetFilters());
        this.resetFiltersButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        searchLayout.add(dateFilterLayout, this.resetFiltersButton);
        return searchLayout;
    }

    /**
     * Create the layout that contains action buttons for the sessions view.
     *
     * @return a horizontal layout with action buttons
     */
    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignored -> this.loadSessions());

        layout.add(refreshButton);
        return layout;
    }

    /**
     * Search for sessions by the current search term and update the grid.
     */
    private void searchSessions() {
        final String searchTerm = this.searchField.getValue();
        if (searchTerm == null || searchTerm.isBlank()) {
            this.loadSessions();
            return;
        }

        try {
            final var sessions = this.analyticsService.searchSessions(searchTerm);
            this.grid.setItems(sessions);
        } catch (final Exception e) {
            LOG.error("Error searching sessions", e);
            NotificationUtil.showError("An error occurred while searching sessions. Please try again.");
        }
    }

    /**
     * Load all sessions asynchronously and populate the grid.
     */
    private void loadSessions() {
        AsyncDataLoader.load(
                () -> this.analyticsService.getAllSessions(),
                this,
                sessions -> {
                    this.grid.setItems(sessions);
                },
                "Failed to load sessions");
    }

    /**
     * Filter sessions by the selected start and end dates.
     * Pushes date range filtering to the database.
     */
    private void filterByDateRange() {
        try {
            final var startDate = this.startDatePicker.getValue();
            final var endDate = this.endDatePicker.getValue();

            if (startDate == null && endDate == null) {
                this.loadSessions();
                return;
            }

            final var startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            final var endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

            if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
                NotificationUtil.showError("Start date must be before or equal to end date.");
                return;
            }

            final var sessions = this.analyticsService.getSessionsByDateRange(startDateTime, endDateTime);
            this.grid.setItems(sessions);
        } catch (final Exception e) {
            LOG.error("Error filtering by date range", e);
            NotificationUtil.showError("An error occurred while filtering by date range. Please try again.");
        }
    }

    private void resetFilters() {
        this.searchField.clear();
        this.startDatePicker.clear();
        this.endDatePicker.clear();
        this.loadSessions();
    }
}
