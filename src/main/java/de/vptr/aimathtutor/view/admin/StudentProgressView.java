package de.vptr.aimathtutor.view.admin;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying student progress summaries.
 * Shows aggregate statistics for all students including completion rates,
 * success rates, and activity.
 */
@Route(value = "admin/progress", layout = AdminMainLayout.class)
@PageTitle("Student Progress - AI Math Tutor")
public class StudentProgressView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(StudentProgressView.class);

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    private Grid<StudentProgressSummaryDto> grid;
    private TextField searchField;

    public StudentProgressView() {
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

        this.buildUI();
        this.loadProgressData();
    }

    private void buildUI() {
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

        this.grid.addColumn(progress -> progress.lastActivity)
                .setHeader("Last Activity")
                .setFlexGrow(1);

        this.add(this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadProgressData();
                    }
                },
                e -> this.searchStudents(),
                "Search by username...",
                "Search Students");

        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(e -> this.loadProgressData());

        layout.add(refreshButton);
        return layout;
    }

    private void searchStudents() {
        final String searchTerm = this.searchField.getValue();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            this.loadProgressData();
            return;
        }

        try {
            final var allProgress = this.analyticsService.getAllUsersProgressSummary();
            final var filtered = allProgress.stream()
                    .filter(p -> p.username != null && p.username.toLowerCase().contains(searchTerm.toLowerCase()))
                    .toList();
            this.grid.setItems(filtered);
        } catch (final Exception e) {
            LOG.error("Error searching students", e);
            NotificationUtil.showError("Error searching students: " + e.getMessage());
        }
    }

    private void loadProgressData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.analyticsService.getAllUsersProgressSummary();
            } catch (final Exception e) {
                LOG.error("Error loading progress data", e);
                throw new RuntimeException("Failed to load progress data", e);
            }
        }).whenComplete((progressData, throwable) -> {
            this.getUI().ifPresent(ui -> ui.access(() -> {
                if (throwable != null) {
                    LOG.error("Error loading progress data: {}", throwable.getMessage(), throwable);
                    NotificationUtil.showError("Failed to load progress data");
                } else {
                    this.grid.setItems(progressData);
                    this.grid.getDataProvider().refreshAll();
                }
            }));
        });
    }
}
