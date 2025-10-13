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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AIFeedbackDto;
import de.vptr.aimathtutor.dto.ChatMessageDto;
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
    private TextField chatInput;
    private Button sendButton;
    private String currentExpression;
    private String sessionId;

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

        // Generate session ID
        this.sessionId = "session-" + System.currentTimeMillis();

        // Header
        final var header = new H2("Graspable Math Workspace");
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        // Main layout: Graspable Math canvas on left, AI chat on right
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

        final var generateProblemButton = new Button("Generate New Problem", e -> this.generateNewProblem());
        generateProblemButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var resetButton = new Button("Reset", e -> this.resetCanvas());

        controls.add(generateProblemButton, resetButton);

        leftPanel.add(this.graspableCanvas, controls);

        // Right side: AI Chat panel
        final var rightPanel = new VerticalLayout();
        rightPanel.setWidth("30%");
        rightPanel.setSpacing(true);

        final var chatHeader = new H3("AI Tutor Chat");
        chatHeader.getStyle().set("margin-top", "0");

        // Chat history panel
        this.feedbackPanel = new VerticalLayout();
        this.feedbackPanel.setSpacing(true);
        this.feedbackPanel.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto")
                .set("max-height", "500px")
                .set("flex-grow", "1");

        // Chat input area
        this.chatInput = new TextField();
        this.chatInput.setPlaceholder("Ask me a question...");
        this.chatInput.setWidthFull();
        this.chatInput.addValueChangeListener(e -> this.sendButton.setEnabled(!e.getValue().trim().isEmpty()));

        this.sendButton = new Button("Send", VaadinIcon.PAPERPLANE.create());
        this.sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.sendButton.setEnabled(false);
        this.sendButton.addClickListener(e -> this.sendQuestion());

        // Allow Enter key to send
        this.chatInput.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> {
            if (!this.chatInput.isEmpty()) {
                this.sendQuestion();
            }
        });

        final var inputLayout = new HorizontalLayout(this.chatInput, this.sendButton);
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.setAlignItems(Alignment.END);
        this.chatInput.getStyle().set("flex-grow", "1");

        rightPanel.add(chatHeader, this.feedbackPanel, inputLayout);
        rightPanel.setFlexGrow(1, this.feedbackPanel);

        mainLayout.add(leftPanel, rightPanel);

        this.add(header, mainLayout);

        // Show welcome message
        this.addChatMessage(ChatMessageDto.system(
                "Welcome! I'm your AI math tutor. Work on problems and I'll help you along the way. Feel free to ask me questions anytime!"));
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

        // Store initial expression
        this.currentExpression = problem.initialExpression;

        // Add message about the loaded problem
        this.addChatMessage(ChatMessageDto.system("Problem loaded: " + problem.title));
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

        // Get AI feedback (may return null if action is insignificant)
        final AIFeedbackDto feedback = this.aiTutorService.analyzeMathAction(event);

        // Only log and display if we got feedback
        if (feedback != null) {
            // Log interaction (optional - for analytics)
            this.aiTutorService.logInteraction(event, feedback);

            // Display feedback to user
            this.addAIFeedback(feedback);
        }
    }

    /**
     * Handles sending a user question to the AI tutor.
     */
    private void sendQuestion() {
        final String question = this.chatInput.getValue().trim();
        if (question.isEmpty()) {
            return;
        }

        // Display user message
        this.addChatMessage(ChatMessageDto.userQuestion(question));

        // Clear input
        this.chatInput.clear();
        this.sendButton.setEnabled(false);

        // Get AI answer
        final ChatMessageDto answer = this.aiTutorService.answerQuestion(
                question,
                this.currentExpression,
                this.sessionId);

        // Display AI answer
        this.addChatMessage(answer);
    }

    /**
     * Adds a chat message to the chat panel.
     */
    private void addChatMessage(final ChatMessageDto message) {
        final var messageDiv = new Div();
        messageDiv.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Style based on sender
        if (message.sender == ChatMessageDto.Sender.USER) {
            messageDiv.getStyle()
                    .set("background-color", "var(--lumo-primary-color-10pct)")
                    .set("margin-left", "var(--lumo-space-l)")
                    .set("border", "1px solid var(--lumo-primary-color-50pct)");
        } else {
            messageDiv.getStyle()
                    .set("background-color", "var(--lumo-contrast-10pct)")
                    .set("margin-right", "var(--lumo-space-l)");
        }

        // Add icon based on message type
        String icon = "";
        if (message.sender == ChatMessageDto.Sender.USER) {
            icon = "👤 ";
        } else if (message.messageType == ChatMessageDto.MessageType.SYSTEM) {
            icon = "ℹ️ ";
        } else {
            icon = "🤖 ";
        }

        final var messagePara = new Paragraph(icon + message.message);
        messagePara.getStyle().set("margin", "0");

        messageDiv.add(messagePara);

        this.feedbackPanel.add(messageDiv);

        // Auto-scroll to bottom
        UI.getCurrent().getPage().executeJs(
                "const panel = $0; panel.scrollTop = panel.scrollHeight;",
                this.feedbackPanel.getElement());

        // Limit chat history to 20 messages
        if (this.feedbackPanel.getComponentCount() > 20) {
            this.feedbackPanel.remove(this.feedbackPanel.getComponentAt(0));
        }
    }

    /**
     * Converts AIFeedbackDto to ChatMessageDto and displays it.
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

        this.addChatMessage(message);
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

        // Store initial expression
        this.currentExpression = problem.initialExpression;

        this.addChatMessage(ChatMessageDto.system("New problem loaded: " + problem.title));
    }

    private void resetCanvas() {
        UI.getCurrent().getPage().executeJs("""
                if (window.graspableMathUtils) {
                    window.graspableMathUtils.clearCanvas();
                }
                """);

        this.currentExpression = null;
        this.feedbackPanel.removeAll();
        this.addChatMessage(ChatMessageDto.system("Canvas reset. Click 'Generate New Problem' to start!"));
    }
}
