package de.vptr.aimathtutor.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto;
import de.vptr.aimathtutor.service.AITutorService;
import de.vptr.aimathtutor.service.AuthService;
import jakarta.inject.Inject;

/**
 * Vaadin view that embeds Graspable Math workspace with AI tutor integration.
 * Students can work on math problems and receive real-time AI feedback.
 */
@Route(value = "graspable-math", layout = MainLayout.class)
public class GraspableMathView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(GraspableMathView.class);

    @Inject
    AuthService authService;

    @Inject
    AITutorService aiTutorService;

    @Inject
    ObjectMapper objectMapper;

    private Div graspableCanvas;
    private VerticalLayout feedbackPanel;
    private Button generateProblemButton;
    private Button getHintButton;

    public GraspableMathView() {
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
    }

    private void buildUI() {
        this.removeAll();

        // Header
        final var header = new H2("Graspable Math Workspace");
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        // Main layout: Graspable Math canvas on left, AI feedback on right
        final var mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Left side: Graspable Math workspace
        final var leftPanel = new VerticalLayout();
        leftPanel.setWidth("70%");
        leftPanel.setSpacing(true);

        // Graspable Math canvas container
        this.graspableCanvas = new Div();
        this.graspableCanvas.setId("graspable-canvas");
        this.graspableCanvas.setWidth("100%");
        this.graspableCanvas.setHeight("500px");
        this.graspableCanvas.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "#ffffff")
                .set("padding", "var(--lumo-space-m)")
                .set("position", "relative");

        // Controls
        final var controls = new HorizontalLayout();
        controls.setSpacing(true);

        this.generateProblemButton = new Button("Generate New Problem", e -> this.generateNewProblem());
        this.generateProblemButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        this.getHintButton = new Button("Get Hint", e -> this.requestHint());
        this.getHintButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        final var resetButton = new Button("Reset", e -> this.resetCanvas());

        controls.add(this.generateProblemButton, this.getHintButton, resetButton);

        leftPanel.add(this.graspableCanvas, controls);

        // Right side: AI Feedback panel
        final var rightPanel = new VerticalLayout();
        rightPanel.setWidth("30%");
        rightPanel.setSpacing(true);

        final var feedbackHeader = new H3("AI Tutor Feedback");
        feedbackHeader.getStyle().set("margin-top", "0");

        this.feedbackPanel = new VerticalLayout();
        this.feedbackPanel.setSpacing(true);
        this.feedbackPanel.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto")
                .set("max-height", "600px");

        rightPanel.add(feedbackHeader, this.feedbackPanel);

        mainLayout.add(leftPanel, rightPanel);

        this.add(header, mainLayout);

        // Show welcome message
        this.addFeedback(AIFeedbackDto.positive(
                "Welcome to the Math Workspace! Click 'Generate New Problem' to begin practicing."));
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initialize Graspable Math widget
        this.initializeGraspableMath();
    }

    /**
     * Initializes the Graspable Math JavaScript widget.
     * This method loads the external JavaScript file and initializes the canvas.
     */
    private void initializeGraspableMath() {
        // Load the external JavaScript file
        UI.getCurrent().getPage().addJavaScript("/js/graspable-math-init.js");

        // Wait a bit for the script to load, then call the initialization function
        UI.getCurrent().getPage().executeJs("""
                    setTimeout(function() {
                        if (window.initializeGraspableMath) {
                            window.initializeGraspableMath();
                        } else {
                            console.error('[GM] Initialization function not found');
                        }
                    }, 100);
                """);

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
     * Called from JavaScript when a math action occurs.
     * This is the bridge between Graspable Math events and our backend.
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
        // No exercise or session needed for standalone workspace

        // Get AI feedback
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Log interaction (optional - for analytics)
        this.aiTutorService.logInteraction(event, feedback);

        // Display feedback to user
        this.addFeedback(feedback);
    }

    private void addFeedback(final AIFeedbackDto feedback) {
        final var feedbackDiv = new Div();
        feedbackDiv.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background-color", this.getFeedbackColor(feedback.type));

        final var message = new Paragraph(feedback.message);
        message.getStyle().set("margin", "0");

        feedbackDiv.add(message);

        // Add hints if available
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

        this.feedbackPanel.addComponentAsFirst(feedbackDiv);
    }

    private String getFeedbackColor(final AIFeedbackDto.FeedbackType type) {
        return switch (type) {
            case POSITIVE -> "var(--lumo-success-color-10pct)";
            case CORRECTIVE -> "var(--lumo-error-color-10pct)";
            case HINT -> "var(--lumo-primary-color-10pct)";
            case SUGGESTION -> "var(--lumo-contrast-10pct)";
            default -> "var(--lumo-contrast-5pct)";
        };
    }

    private void generateNewProblem() {
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("intermediate", "algebra");

        // Load problem into Graspable Math using the utility function
        final String jsCode = String.format("""
                if (window.graspableMathUtils) {
                    window.graspableMathUtils.clearCanvas();
                    window.graspableMathUtils.loadProblem('%s', 100, 50);
                }
                """, problem.initialExpression);

        UI.getCurrent().getPage().executeJs(jsCode);

        this.addFeedback(AIFeedbackDto.positive("New problem loaded: " + problem.title));
    }

    private void requestHint() {
        // Provide a generic hint for solving linear equations
        final var feedback = AIFeedbackDto.hint("Try isolating the variable on one side of the equation.");
        feedback.hints.add("What operation would cancel out the constant term?");
        feedback.hints.add("Remember to perform the same operation on both sides.");

        this.addFeedback(feedback);
    }

    private void resetCanvas() {
        UI.getCurrent().getPage().executeJs("""
                if (window.graspableMathUtils) {
                    window.graspableMathUtils.clearCanvas();
                }
                """);

        this.feedbackPanel.removeAll();
        this.addFeedback(AIFeedbackDto.positive("Canvas reset. Ready for a new problem!"));
    }
}
