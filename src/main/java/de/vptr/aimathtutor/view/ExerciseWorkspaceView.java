package de.vptr.aimathtutor.view;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.AITutorService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.GraspableMathService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * ExerciseWorkspaceView - Student-facing view for working on specific exercises
 * with integrated Graspable Math workspace and AI tutor feedback.
 */
@Route(value = "exercise/:exerciseId", layout = MainLayout.class)
@PageTitle("Exercise Workspace")
@JavaScript("https://graspablemath.com/shared/libs/gmath/gm-inject.js")
public class ExerciseWorkspaceView extends HorizontalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(ExerciseWorkspaceView.class);

    @Inject
    ExerciseService exerciseService;

    @Inject
    AITutorService aiTutorService;

    @Inject
    GraspableMathService graspableMathService;

    @Inject
    AuthService authService;

    @Inject
    ObjectMapper objectMapper;

    private Long exerciseId;
    private ExerciseViewDto exercise;
    private String currentSessionId;
    private int hintCount = 0;

    // UI Components
    private Div canvasContainer;
    private VerticalLayout feedbackPanel;
    private Div feedbackContent;
    private VerticalLayout hintsPanel;
    private Button requestHintButton;
    private Button backButton;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Extract exerciseId from route parameters
        final var params = event.getRouteParameters();
        final var exerciseIdParam = params.get("exerciseId");

        if (exerciseIdParam.isEmpty()) {
            NotificationUtil.showError("Exercise ID is required");
            event.rerouteTo(HomeView.class);
            return;
        }

        try {
            this.exerciseId = Long.parseLong(exerciseIdParam.get());
        } catch (final NumberFormatException e) {
            NotificationUtil.showError("Invalid exercise ID");
            event.rerouteTo(HomeView.class);
            return;
        }

        // Load exercise from database
        final Optional<ExerciseViewDto> exerciseOpt = this.exerciseService.findById(this.exerciseId);
        if (exerciseOpt.isEmpty()) {
            NotificationUtil.showError("Exercise not found");
            event.rerouteTo(HomeView.class);
            return;
        }

        this.exercise = exerciseOpt.get();

        // Check if exercise is published (students can only see published exercises)
        if (!Boolean.TRUE.equals(this.exercise.published)) {
            NotificationUtil.showError("This exercise is not available");
            event.rerouteTo(HomeView.class);
            return;
        }

        // Check if Graspable Math is enabled for this exercise
        if (!Boolean.TRUE.equals(this.exercise.graspableEnabled)) {
            NotificationUtil.showError("This exercise does not have Graspable Math enabled");
            event.rerouteTo(HomeView.class);
            return;
        }

        // Initialize the view
        initializeView();
    }

    private void initializeView() {
        this.setSizeFull();
        this.setSpacing(false);
        this.setPadding(false);

        // Create session for this exercise
        try {
            final Long userId = this.authService.getUserId();
            this.currentSessionId = this.graspableMathService.createSession(userId, this.exerciseId);
        } catch (final Exception e) {
            LOG.error("Failed to create session", e);
            this.currentSessionId = "session-" + System.currentTimeMillis();
        }

        // Left side: Exercise content and Graspable Math canvas (70%)
        final var leftPanel = new VerticalLayout();
        leftPanel.setSizeFull();
        leftPanel.setSpacing(true);
        leftPanel.setPadding(true);
        leftPanel.getStyle().set("width", "70%");

        // Exercise header
        final var header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);

        final var titleLayout = new HorizontalLayout();
        titleLayout.setWidthFull();
        titleLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        titleLayout.setAlignItems(Alignment.CENTER);

        final var title = new H2(this.exercise.title);
        this.backButton = new Button("← Back to Exercises", e -> {
            UI.getCurrent().navigate(HomeView.class);
        });
        this.backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        titleLayout.add(title, this.backButton);
        header.add(titleLayout);

        // Exercise content/instructions
        if (this.exercise.content != null && !this.exercise.content.trim().isEmpty()) {
            final var contentDiv = new Div();
            contentDiv.getStyle()
                    .set("padding", "1rem")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("margin-bottom", "1rem");
            contentDiv.add(new Html("<div>" + this.exercise.content + "</div>"));
            header.add(contentDiv);
        }

        // Difficulty badge
        if (this.exercise.graspableDifficulty != null) {
            final var badge = new Span("Difficulty: " + this.exercise.graspableDifficulty);
            badge.getElement().getThemeList().add("badge");
            switch (this.exercise.graspableDifficulty.toLowerCase()) {
                case "beginner":
                    badge.getElement().getThemeList().add("success");
                    break;
                case "intermediate":
                    badge.getElement().getThemeList().add("contrast");
                    break;
                case "advanced":
                case "expert":
                    badge.getElement().getThemeList().add("error");
                    break;
            }
            header.add(badge);
        }

        // Graspable Math canvas container
        this.canvasContainer = new Div();
        this.canvasContainer.setId("gm-canvas-" + System.currentTimeMillis());
        this.canvasContainer.getStyle()
                .set("width", "100%")
                .set("height", "500px")
                .set("border", "2px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "white")
                .set("margin-top", "1rem");

        leftPanel.add(header, this.canvasContainer);

        // Right side: AI Feedback and Hints (30%)
        final var rightPanel = new VerticalLayout();
        rightPanel.setSpacing(true);
        rightPanel.setPadding(true);
        rightPanel.getStyle()
                .set("width", "30%")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-left", "1px solid var(--lumo-contrast-10pct)");

        // AI Feedback section
        final var feedbackHeader = new H4("AI Tutor Feedback");
        this.feedbackContent = new Div();
        this.feedbackContent.getStyle()
                .set("max-height", "300px")
                .set("overflow-y", "auto")
                .set("padding", "0.5rem")
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)");
        this.feedbackContent.add(new Paragraph("Work on the problem and I'll provide feedback on your steps."));

        this.feedbackPanel = new VerticalLayout(feedbackHeader, this.feedbackContent);
        this.feedbackPanel.setSpacing(false);
        this.feedbackPanel.setPadding(false);

        // Hints section
        final var hintsHeader = new H4("Hints");
        this.hintsPanel = new VerticalLayout();
        this.hintsPanel.setSpacing(true);
        this.hintsPanel.setPadding(true);
        this.hintsPanel.getStyle()
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "1rem");

        this.requestHintButton = new Button("Request Hint", e -> this.showNextHint());
        this.requestHintButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var hintsSection = new VerticalLayout(hintsHeader, this.hintsPanel, this.requestHintButton);
        hintsSection.setSpacing(true);
        hintsSection.setPadding(false);

        rightPanel.add(this.feedbackPanel, hintsSection);

        this.add(leftPanel, rightPanel);

        // Initialize Graspable Math after layout is attached
        UI.getCurrent().getPage().executeJs(
                "setTimeout(() => { " +
                        "  const canvas = new GraspableMath.Canvas(document.getElementById($0), {" +
                        "    use_toolbar: true," +
                        "    show_keypad: true" +
                        "  });" +
                        "  window.gmCanvas = canvas;" +
                        "  canvas.model.addEventListener('change', (event) => {" +
                        "    $1.$server.onMathAction(JSON.stringify(event));" +
                        "  });" +
                        "  if ($2) {" +
                        "    canvas.model.createElement('derivation', {" +
                        "      eq: $2," +
                        "      pos: { x: 'center', y: 50 }" +
                        "    });" +
                        "  }" +
                        "}, 500);",
                this.canvasContainer.getId().get(),
                this.getElement(),
                this.exercise.graspableInitialExpression);
    }

    /**
     * Called from JavaScript when student performs an action in Graspable Math
     */
    @ClientCallable
    public void onMathAction(String eventJson) {
        LOG.debug("Received math action: {}", eventJson);

        try {
            // Parse the Graspable Math event
            // For now, create a simplified event DTO
            final var event = new GraspableEventDto();
            event.sessionId = this.currentSessionId;
            event.exerciseId = this.exerciseId;
            event.eventType = "action"; // We'll parse this better later
            event.timestamp = java.time.LocalDateTime.now();

            // Get user ID
            try {
                event.studentId = this.authService.getUserId();
            } catch (final Exception e) {
                LOG.warn("Could not get user ID", e);
            }

            // Process event through GraspableMathService
            this.graspableMathService.processEvent(event);

            // Get AI feedback
            final var feedback = this.aiTutorService.analyzeMathAction(event);

            // Display feedback in UI
            this.displayFeedback(feedback);

        } catch (final Exception e) {
            LOG.error("Error processing math action", e);
        }
    }

    private void displayFeedback(AIFeedbackDto feedback) {
        if (feedback == null) {
            return;
        }

        UI.getCurrent().access(() -> {
            final var feedbackDiv = new Div();
            feedbackDiv.getStyle()
                    .set("margin-bottom", "0.5rem")
                    .set("padding", "0.75rem")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("border-left", "4px solid " + getFeedbackColor(feedback.type));

            final var icon = getFeedbackIcon(feedback.type);
            final var message = new Html("<div><strong>" + icon + " " +
                    feedback.type.toString() + ":</strong> " +
                    feedback.message + "</div>");

            feedbackDiv.add(message);

            // Add hints if provided
            if (feedback.hints != null && !feedback.hints.isEmpty()) {
                final var hintsList = new VerticalLayout();
                hintsList.setSpacing(false);
                hintsList.setPadding(false);
                hintsList.getStyle().set("margin-top", "0.5rem");
                for (final String hint : feedback.hints) {
                    hintsList.add(new Paragraph("💡 " + hint));
                }
                feedbackDiv.add(hintsList);
            }

            // Add to top of feedback panel
            this.feedbackContent.addComponentAsFirst(feedbackDiv);

            // Limit feedback history to 10 items
            if (this.feedbackContent.getChildren().count() > 10) {
                this.feedbackContent.getChildren()
                        .skip(10)
                        .forEach(component -> this.feedbackContent.remove(component));
            }
        });
    }

    private void showNextHint() {
        if (this.exercise.graspableHints == null || this.exercise.graspableHints.trim().isEmpty()) {
            NotificationUtil.showInfo("No hints available for this exercise");
            return;
        }

        // Split hints by newline
        final String[] hints = this.exercise.graspableHints.split("\\n");

        if (this.hintCount >= hints.length) {
            NotificationUtil.showInfo("No more hints available");
            this.requestHintButton.setEnabled(false);
            return;
        }

        final String hint = hints[this.hintCount];
        this.hintCount++;

        // Record hint usage
        try {
            this.graspableMathService.recordHintUsed(this.currentSessionId);
        } catch (final Exception e) {
            LOG.warn("Could not record hint usage", e);
        }

        // Display hint
        final var hintDiv = new Div();
        hintDiv.getStyle()
                .set("padding", "0.5rem")
                .set("margin-bottom", "0.5rem")
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("border-left", "3px solid var(--lumo-primary-color)")
                .set("border-radius", "var(--lumo-border-radius-s)");
        hintDiv.add(new Paragraph("💡 Hint " + this.hintCount + ": " + hint));

        this.hintsPanel.add(hintDiv);

        // Update button text
        if (this.hintCount >= hints.length) {
            this.requestHintButton.setText("No More Hints");
            this.requestHintButton.setEnabled(false);
        } else {
            this.requestHintButton.setText("Request Hint (" + (hints.length - this.hintCount) + " remaining)");
        }

        NotificationUtil.showSuccess("Hint revealed!");
    }

    private String getFeedbackColor(AIFeedbackDto.FeedbackType type) {
        return switch (type) {
            case POSITIVE -> "var(--lumo-success-color)";
            case CORRECTIVE -> "var(--lumo-error-color)";
            case HINT -> "var(--lumo-primary-color)";
            case SUGGESTION -> "var(--lumo-contrast-60pct)";
            case NEUTRAL -> "var(--lumo-contrast-40pct)";
        };
    }

    private String getFeedbackIcon(AIFeedbackDto.FeedbackType type) {
        return switch (type) {
            case POSITIVE -> "✓";
            case CORRECTIVE -> "✗";
            case HINT -> "💡";
            case SUGGESTION -> "💭";
            case NEUTRAL -> "ℹ";
        };
    }
}
