package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.service.AuthService;
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
public class StudentSessionsView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(StudentSessionsView.class);

    @Inject
    AuthService authService;

    @Inject
    AnalyticsService analyticsService;

    private Grid<StudentSessionViewDto> grid;

    public StudentSessionsView() {
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
        this.loadSessions();
    }

    private void buildUI() {
        this.removeAll();

        // Title
        final var title = new H2("Student Sessions");
        this.add(title);

        // Create grid
        this.grid = new Grid<>(StudentSessionViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(session -> session.username)
                .setHeader("Student")
                .setFlexGrow(1);

        this.grid.addColumn(session -> session.exerciseTitle)
                .setHeader("Exercise")
                .setFlexGrow(1);

        this.grid.addColumn(session -> session.startTime)
                .setHeader("Start Time")
                .setFlexGrow(1);

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

        // Add click listener to view session details
        this.grid.asSingleSelect().addValueChangeListener(event -> {
            final var selectedSession = event.getValue();
            if (selectedSession != null) {
                UI.getCurrent().navigate("admin/session/" + selectedSession.sessionId);
            }
        });

        this.add(this.grid);
    }

    private void loadSessions() {
        CompletableFuture.runAsync(() -> {
            try {
                final List<StudentSessionViewDto> sessions = this.analyticsService.getAllSessions();
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    this.grid.setItems(sessions);
                }));
            } catch (final Exception e) {
                LOG.error("Error loading sessions", e);
                this.getUI().ifPresent(ui -> ui.access(() -> {
                    NotificationUtil.showError("Failed to load sessions");
                }));
            }
        });
    }
}
