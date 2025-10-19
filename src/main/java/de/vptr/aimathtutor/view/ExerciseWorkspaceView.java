package de.vptr.aimathtutor.view;

import java.time.LocalDateTime;
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

import de.vptr.aimathtutor.component.layout.AIChatPanel;
import de.vptr.aimathtutor.component.layout.CommentsPanel;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
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
    private final ConversationContextDto conversationContext = new ConversationContextDto();

    // UI Components
    private Div graspableCanvas;
    private AIChatPanel chatPanel;
    private CommentsPanel commentsPanel;
    private VerticalLayout hintsPanel;
    private Button requestHintButton;
    private Button backButton;
    private String currentExpression;

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

        // Initialize the view (supports both Graspable and non-Graspable exercises)
        this.initializeView();
    }

    private void initializeView() {
        this.setWidthFull(); // Full width only
        this.setSpacing(false);
        this.setPadding(false);
        this.setAlignItems(Alignment.STRETCH); // Make children stretch to same height

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
        leftPanel.setWidthFull(); // Only set width, let height be natural
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

        // Hints section (below canvas or instructions)
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
        hintsSection.setWidthFull();

        // Graspable Math canvas container (only if enabled)
        if (Boolean.TRUE.equals(this.exercise.graspableEnabled)) {
            this.graspableCanvas = new Div();
            this.graspableCanvas.setId("graspable-canvas"); // Fixed ID expected by JavaScript
            this.graspableCanvas.getStyle()
                    .set("width", "100%")
                    .set("height", "80vh")
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("background-color", "var(--lumo-base-color)")
                    .set("margin-top", "1rem");

            leftPanel.add(header, this.graspableCanvas, hintsSection);
        } else {
            // For non-Graspable exercises, just show the instructions
            leftPanel.add(header);

            // Add a notice that this is a non-interactive exercise
            final var noticeDiv = new Div();
            noticeDiv.getStyle()
                    .set("padding", "1rem")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("margin-top", "1rem");
            noticeDiv.add(new Paragraph(
                    "This is a reading/study exercise. Review the content above and use the AI tutor if you have questions."));
            leftPanel.add(noticeDiv, hintsSection);
        }

        if (this.exercise.commentable) {
            // Create comments panel (full width, below canvas)
            this.commentsPanel = new CommentsPanel(this.exerciseId, this.currentSessionId,
                    this.authService.getUserId());
            this.commentsPanel.getStyle().set("margin-top", "1rem");

            // Add comments section to left panel
            final var commentsHeader = new H4("Discussion");
            commentsHeader.getStyle().set("margin-top", "1.5rem");
            leftPanel.add(commentsHeader, this.commentsPanel);
        }

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
                "Work on the problem and I'll provide feedback. Feel free to ask questions anytime!"));

        // Main layout: 70% for exercise + graspable + comments, 30% for chat
        final var mainContentLayout = new HorizontalLayout();
        mainContentLayout.setWidthFull();
        mainContentLayout.setSpacing(false);
        mainContentLayout.setPadding(false);
        mainContentLayout.setFlexGrow(1, leftPanel);
        mainContentLayout.add(leftPanel, this.chatPanel);

        this.add(mainContentLayout);
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initialize Graspable Math widget using external JavaScript (only if enabled)
        if (Boolean.TRUE.equals(this.exercise.graspableEnabled)) {
            this.initializeGraspableMath();
        }
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

        // Update current expression
        this.currentExpression = expressionAfter;

        // Create event DTO
        final var event = new GraspableEventDto();
        event.eventType = eventType;
        event.expressionBefore = expressionBefore;
        event.expressionAfter = expressionAfter;
        event.studentId = this.authService.getUserId();
        event.exerciseId = this.exerciseId;
        event.sessionId = this.currentSessionId;
        event.timestamp = LocalDateTime.now();

        // Add event to conversation context
        this.conversationContext.addAction(event);

        // Check if problem is completed (only if target expression is defined)
        if (this.exercise.graspableTargetExpression != null
                && !this.exercise.graspableTargetExpression.trim().isEmpty()) {
            final boolean isComplete = this.graspableMathService.checkCompletion(
                    expressionAfter,
                    this.exercise.graspableTargetExpression);

            if (isComplete) {
                event.isComplete = true;
                // Mark session as completed
                this.graspableMathService.markSessionComplete(this.currentSessionId);

                // Show success notification
                UI.getCurrent().access(() -> {
                    NotificationUtil.showSuccess("🎉 Congratulations! You've solved the problem correctly!");
                });
            }
        }

        // Process event through GraspableMathService (for session tracking)
        this.graspableMathService.processEvent(event);

        // Get AI feedback asynchronously (may return null if action is insignificant)
        // Don't show typing indicator for math actions - only show it when we get
        // actual feedback
        final var ui = UI.getCurrent();

        this.aiTutorService.analyzeMathActionAsync(event, this.conversationContext).thenAccept(feedback -> {
            ui.access(() -> {
                // Only log and display if we got feedback
                if (feedback != null) {
                    // Log interaction
                    this.aiTutorService.logInteraction(event, feedback);

                    // Create feedback message and add to conversation context
                    final var feedbackMessage = ChatMessageDto.aiFeedback(feedback.message);
                    feedbackMessage.sessionId = this.currentSessionId;
                    this.conversationContext.addAIMessage(feedbackMessage);

                    // Display feedback inline (replaces displayFeedback method)
                    final var message = ChatMessageDto.aiFeedback(feedback.message);
                    message.sessionId = this.currentSessionId;

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
        // Create and add user message to context
        final var userMessage = ChatMessageDto.userQuestion(question);
        userMessage.sessionId = this.currentSessionId;
        this.conversationContext.addQuestion(userMessage);

        // Display user message
        this.chatPanel.addMessage(userMessage);

        // Show typing indicator while waiting for AI response
        this.chatPanel.showTypingIndicator();

        // Get AI answer asynchronously
        final var ui = UI.getCurrent();
        this.aiTutorService
                .answerQuestionAsync(question, this.currentExpression, this.currentSessionId, this.conversationContext)
                .thenAccept(answer -> {
                    ui.access(() -> {
                        // Hide typing indicator
                        this.chatPanel.hideTypingIndicator();

                        // Add AI answer to conversation context
                        this.conversationContext.addAIMessage(answer);

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
}
