package de.vptr.aimathtutor.view.admin;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying student progress summaries.
 * Shows aggregate statistics for all students including completion rates,
 * success rates, and activity.
 */
@Route(value = "admin/progress", layout = AdminMainLayout.class)
@PageTitle("Student Progress - AI Math Tutor")
public class AdminProgressView extends AbstractAdminView {

    @Inject
    private transient AnalyticsService analyticsService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private Grid<StudentProgressSummaryDto> grid;
    private TextField searchField;
    private Button resetFiltersButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    /**
     * Constructs the AdminProgressView with full size and padding.
     */
    public AdminProgressView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Perform authentication check and construct the progress dashboard before
     * the view becomes visible.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.buildUi();
        this.loadProgressData();
    }

    private void buildUi() {
        this.removeAll();

        // Title
        final var title = new H2("Student Progress");
        this.add(title);

        // Search layout
        final var searchLayout = this.createSearchLayout();
        this.add(searchLayout);

        // Button layout
        final var buttonLayout = this.createButtonLayout();
        this.add(buttonLayout);

        // Create grid
        this.grid = new Grid<>(StudentProgressSummaryDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(progress -> progress.username)
                .setHeader("Student")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.totalSessions)
                .setHeader("Total Sessions")
                .setFlexGrow(0);

        this.grid.addColumn(progress -> progress.completedSessions)
                .setHeader("Completed")
                .setFlexGrow(0);

        this.grid.addColumn(
                (ValueProvider<StudentProgressSummaryDto, ?>) StudentProgressSummaryDto::getCompletionRatePercentage)
                .setHeader("Completion Rate")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.totalProblems)
                .setHeader("Total Problems")
                .setFlexGrow(0);

        this.grid.addColumn(
                (ValueProvider<StudentProgressSummaryDto, ?>) StudentProgressSummaryDto::getSuccessRatePercentage)
                .setHeader("Success Rate")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.hintsUsed)
                .setHeader("Hints Used")
                .setFlexGrow(0);

        this.grid.addColumn(
                (ValueProvider<StudentProgressSummaryDto, ?>) StudentProgressSummaryDto::getFormattedAverageActions)
                .setHeader("Avg Actions/Problem")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> this.dateTimeFormatter.formatDateTime(progress.lastActivity))
                .setHeader("Last Activity")
                .setWidth("180px").setFlexGrow(0);

        this.add(this.grid);
    }

    /**
     * Create the search layout including date filters and reset button.
     *
     * @return the constructed SearchLayout
     */
    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().isBlank()) {
                        this.loadProgressData();
                    }
                },
                ignored -> this.searchStudents(),
                "Search by username...",
                "Search Students");

        this.searchField = searchLayout.getTextfield();

        // Add date range filter for last activity
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
     * Create the button layout for the progress view.
     *
     * @return a horizontal layout containing action buttons
     */
    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignored -> this.loadProgressData());

        layout.add(refreshButton);
        return layout;
    }

    /**
     * Search for students by username and update the grid with results.
     * Pushes username filtering to the database.
     */
    private void searchStudents() {
        final String searchTerm = this.searchField.getValue();
        if (searchTerm == null || searchTerm.isBlank()) {
            this.loadProgressData();
            return;
        }

        AsyncDataLoader.load(
                () -> this.analyticsService.getUsersProgressSummaryByUsernameSearch(searchTerm),
                this,
                progress -> this.grid.setItems(progress),
                "An error occurred while searching students. Please try again.");
    }

    /**
     * Load aggregated progress data asynchronously and populate the grid.
     */
    private void loadProgressData() {
        AsyncDataLoader.load(
                () -> this.analyticsService.getAllUsersProgressSummary(),
                this,
                progressData -> {
                    this.grid.setItems(progressData);
                },
                "Failed to load progress data");
    }

    /**
     * Filter the progress data by the selected start and end date.
     * Pushes date range filtering to the database.
     */
    private void filterByDateRange() {
        final var startDate = this.startDatePicker.getValue();
        final var endDate = this.endDatePicker.getValue();

        if (startDate == null && endDate == null) {
            this.loadProgressData();
            return;
        }

        final var startDateTime = startDate != null ? startDate.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);
        final var endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.of(2099, 12, 31, 23, 59, 59);

        AsyncDataLoader.load(
                () -> this.analyticsService.getUsersProgressSummaryByDateRange(startDateTime, endDateTime),
                this,
                progress -> this.grid.setItems(progress),
                "An error occurred while filtering by date range. Please try again.");
    }

    private void resetFilters() {
        this.searchField.clear();
        this.startDatePicker.clear();
        this.endDatePicker.clear();
        this.loadProgressData();
    }
}
