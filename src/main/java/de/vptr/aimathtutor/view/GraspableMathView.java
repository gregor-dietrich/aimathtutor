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
import de.vptr.aimathtutor.service.GraspableMathService;
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
    GraspableMathService graspableMathService;

    @Inject
    AITutorService aiTutorService;

    @Inject
    ObjectMapper objectMapper;

    private String sessionId;
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

        buildUI();
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
                .set("background-color", "var(--lumo-base-color)")
                .set("padding", "var(--lumo-space-m)");

        // Controls
        final var controls = new HorizontalLayout();
        controls.setSpacing(true);

        this.generateProblemButton = new Button("Generate New Problem", e -> generateNewProblem());
        this.generateProblemButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        this.getHintButton = new Button("Get Hint", e -> requestHint());
        this.getHintButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        final var resetButton = new Button("Reset", e -> resetCanvas());

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

        // Initialize session
        initializeSession();
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initialize Graspable Math widget
        initializeGraspableMath();
    }

    private void initializeSession() {
        // Create a new session for this user
        // For demo purposes, using exercise ID 1
        final Long userId = this.authService.getUserId();
        final Long exerciseId = 1L;

        this.sessionId = this.graspableMathService.createSession(userId, exerciseId);
        LOG.info("Initialized session: {}", this.sessionId);

        addFeedback(AIFeedbackDto.positive("Welcome! Start working on the problem below."));
    }

    /**
     * Initializes the Graspable Math JavaScript widget.
     * This method injects the necessary JavaScript code into the page.
     */
    private void initializeGraspableMath() {
        final String jsCode = """
                // Load Graspable Math library
                if (!document.getElementById('graspable-math-lib')) {
                    var script = document.createElement('script');
                    script.id = 'graspable-math-lib';
                    script.src = 'https://graspablemath.com/shared/libs/gmath/gm-inject.js';
                    script.onload = function() {
                        console.log('Graspable Math library loaded');
                        initializeCanvas();
                    };
                    document.head.appendChild(script);
                } else {
                    initializeCanvas();
                }

                function initializeCanvas() {
                    // Wait for library to be ready
                    if (typeof GraspableMath === 'undefined') {
                        setTimeout(initializeCanvas, 100);
                        return;
                    }

                    var canvasElement = document.getElementById('graspable-canvas');
                    if (!canvasElement) {
                        console.error('Canvas element not found');
                        return;
                    }

                    // Initialize Graspable Math canvas
                    var canvas = new GraspableMath.Canvas(canvasElement, {
                        use_fade_effects: true,
                        use_property_effect: true
                    });

                    // Store canvas reference globally
                    window.graspableCanvas = canvas;

                    // Add initial problem
                    canvas.model.createElement('derivation', {
                        eq: '2x + 5 = 13',
                        pos: { x: 50, y: 50 }
                    });

                    // Listen to events
                    canvas.model.on('change', function(event) {
                        handleGraspableEvent(event);
                    });

                    console.log('Graspable Math canvas initialized');
                }

                function handleGraspableEvent(event) {
                    // Extract event details
                    var eventData = {
                        type: event.type || 'unknown',
                        expressionBefore: event.before || '',
                        expressionAfter: event.after || ''
                    };

                    console.log('Graspable Math event:', eventData);

                    // Call server-side method
                    if (window.graspableViewConnector) {
                        window.graspableViewConnector.onMathAction(
                            eventData.type,
                            eventData.expressionBefore,
                            eventData.expressionAfter
                        );
                    }
                }
                """;

        UI.getCurrent().getPage().executeJs(jsCode);

        // Register server-side connector
        registerServerConnector();
    }

    /**
     * Registers a server-side connector that JavaScript can call.
     */
    private void registerServerConnector() {
        UI.getCurrent().getPage().executeJs(
                "window.graspableViewConnector = { onMathAction: function(type, before, after) { " +
                        "   $0.$server.onMathAction(type, before, after); " +
                        "}}",
                getElement());
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
        event.exerciseId = 1L; // For demo
        event.sessionId = this.sessionId;

        // Process event
        this.graspableMathService.processEvent(event);

        // Get AI feedback
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Log interaction
        this.aiTutorService.logInteraction(event, feedback);

        // Display feedback to user
        addFeedback(feedback);
    }

    private void addFeedback(final AIFeedbackDto feedback) {
        final var feedbackDiv = new Div();
        feedbackDiv.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background-color", getFeedbackColor(feedback.type));

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

        // Load problem into Graspable Math
        final String jsCode = String.format("""
                if (window.graspableCanvas) {
                    window.graspableCanvas.model.clear();
                    window.graspableCanvas.model.createElement('derivation', {
                        eq: '%s',
                        pos: { x: 50, y: 50 }
                    });
                }
                """, problem.initialExpression);

        UI.getCurrent().getPage().executeJs(jsCode);

        addFeedback(AIFeedbackDto.positive("New problem loaded: " + problem.title));
    }

    private void requestHint() {
        this.graspableMathService.recordHintUsed(this.sessionId);

        final var feedback = AIFeedbackDto.hint("Try isolating the variable on one side of the equation.");
        feedback.hints.add("What operation would cancel out the constant term?");

        addFeedback(feedback);
    }

    private void resetCanvas() {
        UI.getCurrent().getPage().executeJs("""
                if (window.graspableCanvas) {
                    window.graspableCanvas.model.clear();
                }
                """);

        this.feedbackPanel.removeAll();
        addFeedback(AIFeedbackDto.positive("Canvas reset. Ready for a new problem!"));
    }
}
