package de.vptr.aimathtutor.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.dto.GraspableProblemDto;
import de.vptr.aimathtutor.service.AITutorService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.GraspableMathService;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.component.AIChatPanel;
import jakarta.inject.Inject;

/**
 * Vaadin view that embeds Graspable Math workspace with AI tutor integration.
 * Students can work on math problems and receive real-time AI feedback.
 */
@Route(value = "graspable-math", layout = MainLayout.class)
public class GraspableMathView extends HorizontalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(GraspableMathView.class);

    @Inject
    AuthService authService;

    @Inject
    AITutorService aiTutorService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    GraspableMathService graspableMathService;

    private Div graspableCanvas;
    private AIChatPanel chatPanel;
    private String currentExpression;
    private String targetExpression;
    private String sessionId;
    private boolean initialized = false;

    public GraspableMathView() {
        // Constructor intentionally empty - initialization happens in buildUI()
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo("login");
            return;
        }

        // Only build UI once
        if (!this.initialized) {
            this.buildUI();
            this.initialized = true;
        }
    }

    private void buildUI() {
        this.removeAll();

        this.setSizeFull();
        this.setSpacing(false);
        this.setPadding(false);

        // Generate session ID
        this.sessionId = "session-" + System.currentTimeMillis();

        // Left side: Graspable Math workspace (70%)
        final var leftPanel = new VerticalLayout();
        leftPanel.setSizeFull();
        leftPanel.setSpacing(true);
        leftPanel.setPadding(true);
        leftPanel.getStyle().set("width", "70%");

        // Header
        final var header = new H2("Graspable Math Workspace");
        header.getStyle().set("margin-top", "0");

        // Graspable Math canvas container
        this.graspableCanvas = new Div();
        this.graspableCanvas.setId("graspable-canvas");
        this.graspableCanvas.getStyle()
                .set("width", "100%")
                .set("height", "80vh")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("margin-top", "1rem");

        // Controls
        final var controls = new HorizontalLayout();
        controls.setSpacing(true);

        final var generateProblemButton = new Button("Generate New Problem", e -> this.generateNewProblem());
        generateProblemButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var customProblemButton = new Button("Enter Custom Problem", e -> this.showCustomProblemDialog());
        customProblemButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        final var resetButton = new Button("Reset", e -> this.resetCanvas());

        controls.add(generateProblemButton, customProblemButton, resetButton);
        leftPanel.add(header, this.graspableCanvas, controls);

        // Right side: AI Chat panel with built-in styling (30%)
        // Get user's avatar settings
        final var currentUserEntity = this.authService.getCurrentUserEntity();
        final String userAvatar = currentUserEntity != null && currentUserEntity.userAvatarEmoji != null
                ? currentUserEntity.userAvatarEmoji
                : "🧒";
        final String tutorAvatar = currentUserEntity != null && currentUserEntity.tutorAvatarEmoji != null
                ? currentUserEntity.tutorAvatarEmoji
                : "🧑‍🏫";
        this.chatPanel = new AIChatPanel(this::handleUserQuestion, userAvatar, tutorAvatar);

        // Add welcome message
        this.chatPanel.addMessage(ChatMessageDto.system(
                "Welcome! I'm your AI math tutor. Work on problems and I'll help you along the way. Feel free to ask me questions anytime!"));

        this.add(leftPanel, this.chatPanel);
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

        // Auto-load a problem after canvas initialization
        this.loadInitialProblem();
    }

    /**
     * Loads an initial problem automatically when the view is first opened.
     */
    private void loadInitialProblem() {
        // Generate a problem
        final GraspableProblemDto problem = this.aiTutorService.generateProblem("intermediate", "algebra");

        // Wait for canvas to be ready, then load the problem
        final String loadScript = String.format("""
                setTimeout(function() {
                    var loadProblemWhenReady = function() {
                        if (window.graspableCanvas && window.graspableMathUtils) {
                            console.log('[GM] Canvas ready, loading initial problem');
                            window.graspableMathUtils.loadProblem('%s', 100, 50);
                        } else {
                            console.log('[GM] Waiting for canvas...');
                            setTimeout(loadProblemWhenReady, 200);
                        }
                    };
                    loadProblemWhenReady();
                }, 500);
                """, problem.initialExpression);

        UI.getCurrent().getPage().executeJs(loadScript);

        // Store initial expression and target for completion checking
        this.currentExpression = problem.initialExpression;
        this.targetExpression = problem.targetExpression;

        // Add message about the loaded problem
        this.chatPanel.addMessage(ChatMessageDto.system("Problem loaded: " + problem.title));
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

        // Update current expression
        this.currentExpression = expressionAfter;

        // Create event DTO
        final var event = new GraspableEventDto();
        event.eventType = eventType;
        event.expressionBefore = expressionBefore;
        event.expressionAfter = expressionAfter;
        event.studentId = this.authService.getUserId();
        event.sessionId = this.sessionId;
        // No exercise needed for standalone workspace

        // Check if problem is completed (if target is set)
        if (this.targetExpression != null && !this.targetExpression.trim().isEmpty()) {
            final boolean isComplete = this.graspableMathService.checkCompletion(
                    expressionAfter,
                    this.targetExpression);

            if (isComplete) {
                event.isComplete = true;

                // Show success notification
                UI.getCurrent().access(() -> {
                    NotificationUtil.showSuccess("🎉 Congratulations! You've solved the problem correctly!");
                });

                // Clear target so we don't keep checking
                this.targetExpression = null;
            }
        }

        // Get AI feedback asynchronously (may return null if action is insignificant)
        // Don't show typing indicator for math actions - only show it when we get
        // actual feedback
        final var ui = UI.getCurrent();

        this.aiTutorService.analyzeMathActionAsync(event).thenAccept(feedback -> {
            ui.access(() -> {
                // Only log and display if we got feedback
                if (feedback != null) {
                    // Log interaction (optional - for analytics)
                    this.aiTutorService.logInteraction(event, feedback);

                    // Display feedback to user
                    this.addAIFeedback(feedback);
                }
            });
        }).exceptionally(ex -> {
            ui.access(() -> {
                LOG.error("Error getting AI feedback", ex);
            });
            return null;
        });
    }

    /**
     * Handles user questions from the chat panel.
     */
    private void handleUserQuestion(final String question) {
        // Display user message
        this.chatPanel.addMessage(ChatMessageDto.userQuestion(question));

        // Show typing indicator while waiting for AI response
        this.chatPanel.showTypingIndicator();

        // Get AI answer asynchronously
        final var ui = UI.getCurrent();
        this.aiTutorService.answerQuestionAsync(question, this.currentExpression, this.sessionId)
                .thenAccept(answer -> {
                    ui.access(() -> {
                        // Hide typing indicator
                        this.chatPanel.hideTypingIndicator();

                        // Display AI answer
                        this.chatPanel.addMessage(answer);
                    });
                })
                .exceptionally(ex -> {
                    ui.access(() -> {
                        this.chatPanel.hideTypingIndicator();
                        LOG.error("Error getting AI answer", ex);
                        this.chatPanel.addMessage(ChatMessageDto.aiAnswer(
                                "Sorry, I encountered an error. Please try again."));
                    });
                    return null;
                });
    }

    /**
     * Displays AI feedback as a chat message.
     */
    private void addAIFeedback(final AIFeedbackDto feedback) {
        final var message = ChatMessageDto.aiFeedback(feedback.message);
        message.sessionId = this.sessionId;

        // Add hints as part of the message if present
        if (feedback.hints != null && !feedback.hints.isEmpty()) {
            final StringBuilder fullMessage = new StringBuilder(feedback.message);
            for (final String hint : feedback.hints) {
                fullMessage.append("\n💡 ").append(hint);
            }
            message.message = fullMessage.toString();
        }

        this.chatPanel.addMessage(message);
    }

    /**
     * Shows a dialog for entering a custom math problem.
     */
    private void showCustomProblemDialog() {
        final var dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Enter Custom Problem");
        dialog.setWidth("400px");

        final var layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);

        final var instructions = new Paragraph(
                "Enter a mathematical expression to work with. Examples: 2x+5=15, x^2-4=0, (x+3)(x-2)");
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");

        final var expressionField = new TextField("Expression");
        expressionField.setWidthFull();
        expressionField.setPlaceholder("e.g., 2x+5=15");
        expressionField.setValueChangeMode(ValueChangeMode.EAGER);
        expressionField.focus();

        layout.add(instructions, expressionField);
        dialog.add(layout);

        // Buttons
        final var cancelButton = new Button("Cancel", e -> dialog.close());
        final var loadButton = new Button("Load Problem", e -> {
            final String expression = expressionField.getValue().trim();
            if (!expression.isEmpty()) {
                this.loadCustomProblem(expression);
                dialog.close();
            }
        });
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadButton.setEnabled(false);

        expressionField.addValueChangeListener(e -> {
            loadButton.setEnabled(!e.getValue().trim().isEmpty());
        });

        // Allow Enter to submit
        expressionField.addKeyPressListener(Key.ENTER, e -> {
            if (!expressionField.isEmpty()) {
                this.loadCustomProblem(expressionField.getValue().trim());
                dialog.close();
            }
        });

        dialog.getFooter().add(cancelButton, loadButton);
        dialog.open();
    }

    /**
     * Loads a custom problem into the Graspable Math canvas.
     */
    private void loadCustomProblem(final String expression) {
        final String jsCode = String.format("""
                if (window.graspableMathUtils) {
                    window.graspableMathUtils.clearCanvas();
                    window.graspableMathUtils.loadProblem('%s', 100, 50);
                }
                """, expression.replace("'", "\\'"));

        UI.getCurrent().getPage().executeJs(jsCode);

        // Store expression
        this.currentExpression = expression;

        this.chatPanel.addMessage(ChatMessageDto.system("Custom problem loaded: " + expression));
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

        // Store initial expression and target
        this.currentExpression = problem.initialExpression;
        this.targetExpression = problem.targetExpression; // Store target for completion checking

        this.chatPanel.addMessage(ChatMessageDto.system("New problem loaded: " + problem.title));
    }

    private void resetCanvas() {
        UI.getCurrent().getPage().executeJs("""
                if (window.graspableMathUtils) {
                    window.graspableMathUtils.clearCanvas();
                }
                """);

        this.currentExpression = null;
        this.targetExpression = null; // Clear target too
        this.chatPanel.clearMessages();
        this.chatPanel.addMessage(ChatMessageDto.system("Canvas reset. Click 'Generate New Problem' to start!"));
    }
}
