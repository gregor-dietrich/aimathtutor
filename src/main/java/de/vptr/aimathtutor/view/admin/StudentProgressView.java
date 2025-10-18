package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for displaying student progress summaries.
 * Shows aggregate statistics for all students including completion rates, success rates, and activity.
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

        this.grid.addColumn(progress -> progress.getCompletionRatePercentage())
                .setHeader("Completion Rate")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.totalProblems)
                .setHeader("Total Problems")
                .setFlexGrow(0);

        this.grid.addColumn(progress -> progress.getSuccessRatePercentage())
                .setHeader("Success Rate")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.hintsUsed)
                .setHeader("Hints Used")
                .setFlexGrow(0);

        this.grid.addColumn(progress -> progress.getFormattedAverageActions())
                .setHeader("Avg Actions/Problem")
                .setFlexGrow(1);

        this.grid.addColumn(progress -> progress.lastActivity)
                .setHeader("Last Activity")
                .setFlexGrow(1);

        this.add(this.grid);
    }

    private void loadProgressData() {
        CompletableFuture.runAsync(() -> {
            try {
                final List<StudentProgressSummaryDto> progressData = this.analyticsService
                        .getAllUsersProgressSummary();
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    this.grid.setItems(progressData);
                }));
            } catch (final Exception e) {
                LOG.error("Error loading progress data", e);
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    NotificationUtil.showError("Failed to load progress data");
                }));
            }
        });
    }
}
