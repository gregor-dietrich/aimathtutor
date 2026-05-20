package de.vptr.aimathtutor.view.admin;

import java.time.LocalTime;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.util.AdminSearchLayoutFactory;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying all student sessions with filtering and detail options. Shows session information including
 * student, exercise, duration, and completion status.
 */
@Route(value = "admin/sessions", layout = AdminMainLayout.class)
@PageTitle("Student Sessions - AI Math Tutor")
@SuppressWarnings("NullAway")
public class AdminSessionsView extends AbstractAdminView {

    @Inject
    private transient AnalyticsService analyticsService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private Grid<StudentSessionViewDto> grid;
    private TextField searchField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private HorizontalLayout buttonLayout;

    /**
     * Constructs the AdminSessionsView with full size and padding.
     */
    public AdminSessionsView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        super.onAttach(event);
        this.loadSessions();
    }

    @Override
    protected void buildUi() {
        this.removeAll();

        // Title
        final var title = new H2("Student Sessions");
        this.add(title);

        // Search layout
        final var searchLayout = this.createSearchLayout();
        this.add(searchLayout);

        // Button layout
        this.add(this.buttonLayout);

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
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(ignored -> UI.getCurrent().navigate("admin/session/" + session.sessionId));
            return usernameSpan;
        }).setHeader("Student").setFlexGrow(1);

        this.grid.addColumn(session -> session.exerciseTitle).setHeader("Exercise").setFlexGrow(1);

        this.grid.addColumn(session -> this.dateTimeFormatter.formatDateTime(session.startTime)).setHeader("Start Time")
                .setWidth("180px").setFlexGrow(0);

        this.grid.addColumn(StudentSessionViewDto::getFormattedDuration).setHeader("Duration").setFlexGrow(0);

        this.grid.addColumn(session -> session.completed ? "✓" : "✗").setHeader("Completed").setFlexGrow(0);

        this.add(this.grid);
    }

    /**
     * Create the search layout used to filter sessions by text and date.
     *
     * @return the search layout
     */
    private HorizontalLayout createSearchLayout() {
        final var components = new AdminSearchLayoutFactory.Components();
        final var searchLayout = AdminSearchLayoutFactory.create(this::loadSessions, this::searchSessions,
                "Search by student or exercise...", "Search Sessions", this::filterByDateRange, this::resetFilters,
                this::loadSessions, components);
        this.searchField = components.searchField;
        this.startDatePicker = components.startDatePicker;
        this.endDatePicker = components.endDatePicker;
        this.buttonLayout = components.buttonLayout;
        return searchLayout;
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

        AsyncDataLoader.load(() -> this.analyticsService.searchSessions(searchTerm), this,
                sessions -> this.grid.setItems(sessions),
                "An error occurred while searching sessions. Please try again.");
    }

    /**
     * Load all sessions asynchronously and populate the grid.
     */
    private void loadSessions() {
        AsyncDataLoader.load(() -> this.analyticsService.getAllSessions(), this, sessions -> {
            this.grid.setItems(sessions);
        }, "Failed to load sessions");
    }

    /**
     * Filter sessions by the selected start and end dates. Pushes date range filtering to the database.
     */
    private void filterByDateRange() {
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

        AsyncDataLoader.load(() -> this.analyticsService.getSessionsByDateRange(startDateTime, endDateTime), this,
                sessions -> this.grid.setItems(sessions),
                "An error occurred while filtering by date range. Please try again.");
    }

    private void resetFilters() {
        this.searchField.clear();
        this.startDatePicker.clear();
        this.endDatePicker.clear();
        this.loadSessions();
    }
}
