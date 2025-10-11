package de.vptr.aimathtutor.view;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
 * Uses the updated Graspable Math API with external JavaScript.
 */
@Route(value = "exercise/:exerciseId", layout = MainLayout.class)
@PageTitle("Exercise Workspace")
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
    public void beforeEnter(final BeforeEnterEvent event) {
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
        this.initializeView();
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
        this.canvasContainer.setId("graspable-canvas"); // Fixed ID expected by JavaScript
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
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        this.feedbackPanel = new VerticalLayout(feedbackHeader, this.feedbackContent);
        this.feedbackPanel.setSpacing(false);
        this.feedbackPanel.setPadding(false);

        // Add welcome message as a proper feedback item
        final var welcomeDiv = new Div();
        welcomeDiv.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background-color", "var(--lumo-primary-color-10pct)");
        final var welcomeMsg = new Paragraph("💡 Work on the problem and I'll provide feedback on your steps.");
        welcomeMsg.getStyle().set("margin", "0");
        welcomeDiv.add(welcomeMsg);
        this.feedbackContent.add(welcomeDiv);

        // Hints section
        final var hintsHeader = new H4("Hints");
        this.hintsPanel = new VerticalLayout();
        this.hintsPanel.setSpacing(true);
        this.hintsPanel.setPadding(true);
        this.hintsPanel.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)");

        this.requestHintButton = new Button("Request Hint", e -> this.showNextHint());
        this.requestHintButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var hintsSection = new VerticalLayout(hintsHeader, this.hintsPanel, this.requestHintButton);
        hintsSection.setSpacing(true);
        hintsSection.setPadding(false);

        rightPanel.add(this.feedbackPanel, hintsSection);

        this.add(leftPanel, rightPanel);
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initialize Graspable Math widget using external JavaScript
        this.initializeGraspableMath();
    }

    /**
     * Initializes the Graspable Math JavaScript widget using the external file.
     * This loads the problem from the exercise configuration.
     */
    private void initializeGraspableMath() {
        if (this.exercise.graspableInitialExpression == null
                || this.exercise.graspableInitialExpression.trim().isEmpty()) {
            LOG.warn("No initial expression configured for exercise {}", this.exerciseId);
            return;
        }

        // Load the external JavaScript file
        UI.getCurrent().getPage().addJavaScript("/js/graspable-math-init.js");

        // Initialize canvas and load problem once ready
        final String initScript = String.format("""
                setTimeout(function() {
                    if (window.initializeGraspableMath) {
                        window.initializeGraspableMath();

                        // Wait for canvas to be ready, then load the problem
                        var loadProblemWhenReady = function() {
                            if (window.graspableCanvas && window.graspableMathUtils) {
                                console.log('[Exercise] Canvas ready, loading problem');
                                window.graspableMathUtils.loadProblem('%s', 100, 50);
                            } else {
                                console.log('[Exercise] Waiting for canvas...');
                                setTimeout(loadProblemWhenReady, 200);
                            }
                        };
                        setTimeout(loadProblemWhenReady, 500);
                    } else {
                        console.error('[Exercise] Graspable Math initialization function not found');
                    }
                }, 100);
                """, this.exercise.graspableInitialExpression);

        UI.getCurrent().getPage().executeJs(initScript);

        // Register server-side connector
        this.registerServerConnector();
    }

    /**
     * Registers a server-side connector that JavaScript can call.
     */
    private void registerServerConnector() {
        UI.getCurrent().getPage().executeJs(
                "window.graspableViewConnector = { onMathAction: function(type, before, after) { " +
                        "   $0.$server.onMathAction(type, before, after); " +
                        "}}",
                this.getElement());
    }

    /**
     * Called from JavaScript when student performs an action in Graspable Math.
     * Updated to match the new event signature from graspable-math-init.js
     */
    @ClientCallable
    public void onMathAction(final String eventType, final String expressionBefore, final String expressionAfter) {
        LOG.debug("Math action: type={}, before={}, after={}", eventType, expressionBefore, expressionAfter);

        // Create event DTO
        final var event = new GraspableEventDto();
        event.eventType = eventType;
        event.expressionBefore = expressionBefore;
        event.expressionAfter = expressionAfter;
        event.studentId = this.authService.getUserId();
        event.exerciseId = this.exerciseId;
        event.sessionId = this.currentSessionId;
        event.timestamp = java.time.LocalDateTime.now();

        // Process event through GraspableMathService (for session tracking)
        this.graspableMathService.processEvent(event);

        // Get AI feedback
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Log interaction
        this.aiTutorService.logInteraction(event, feedback);

        // Display feedback
        this.displayFeedback(feedback);
    }

    /**
     * @deprecated Old method signature - no longer called by new JavaScript
     */
    @Deprecated
    @ClientCallable
    public void onMathActionOld(final String eventJson) {
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

    private void displayFeedback(final AIFeedbackDto feedback) {
        if (feedback == null) {
            return;
        }

        UI.getCurrent().access(() -> {
            final var feedbackDiv = new Div();
            feedbackDiv.getStyle()
                    .set("padding", "var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-s)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("background-color", this.getFeedbackColor(feedback.type));

            final var icon = this.getFeedbackIcon(feedback.type);
            final var message = new Paragraph(icon + " " + feedback.message);
            message.getStyle().set("margin", "0");

            feedbackDiv.add(message);

            // Add hints if provided
            if (feedback.hints != null && !feedback.hints.isEmpty()) {
                for (final String hint : feedback.hints) {
                    final var hintPara = new Paragraph("💡 " + hint);
                    hintPara.getStyle()
                            .set("margin", "var(--lumo-space-xs) 0 0 0")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("font-style", "italic");
                    feedbackDiv.add(hintPara);
                }
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

        // Split hints by newline, semicolon, or pipe character
        // Supports formats like: "hint1\nhint2" or "hint1;hint2" or "hint1|hint2"
        final String[] hints = this.exercise.graspableHints.split("\\r?\\n|;|\\|");

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

    private String getFeedbackColor(final AIFeedbackDto.FeedbackType type) {
        return switch (type) {
            case POSITIVE -> "var(--lumo-success-color-10pct)";
            case CORRECTIVE -> "var(--lumo-error-color-10pct)";
            case HINT -> "var(--lumo-primary-color-10pct)";
            case SUGGESTION -> "var(--lumo-contrast-10pct)";
            case NEUTRAL -> "var(--lumo-contrast-5pct)";
        };
    }

    private String getFeedbackIcon(final AIFeedbackDto.FeedbackType type) {
        return switch (type) {
            case POSITIVE -> "✅";
            case CORRECTIVE -> "❌";
            case HINT -> "💡";
            case SUGGESTION -> "⚡";
            case NEUTRAL -> "ℹ️";
        };
    }
}
