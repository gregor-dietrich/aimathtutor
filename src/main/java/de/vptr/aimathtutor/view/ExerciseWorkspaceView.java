package de.vptr.aimathtutor.view;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.layout.AiChatPanel;
import de.vptr.aimathtutor.component.layout.CommentsPanel;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.service.AiTutorService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.GraspableMathService;
import de.vptr.aimathtutor.util.AppConstants;
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
    private transient ExerciseService exerciseService;

    @Inject
    private transient AiTutorService aiTutorService;

    @Inject
    private transient GraspableMathService graspableMathService;

    @Inject
    private transient AuthService authService;

    private transient Long exerciseId;
    private transient ExerciseViewDto exercise;
    private transient String currentSessionId;
    private transient int hintCount = 0;
    private transient boolean problemSolved = false;
    private final transient ConversationContextDto conversationContext = new ConversationContextDto();

    // UI Components
    private transient Div graspableCanvas;
    private transient AiChatPanel chatPanel;
    private transient CommentsPanel commentsPanel;
    private transient VerticalLayout hintsPanel;
    private transient Button requestHintButton;
    private transient Button backButton;
    private transient String currentExpression;
    private final transient ConcurrentHashMap<String, CompletableFuture<?>> pendingAsyncFutures = new ConcurrentHashMap<>();

    /**
     * Called before navigation occurs. Extracts exercise ID from route, loads
     * exercise data,
     * initializes session ID, validates exercise is published/commentable, and
     * builds workspace UI.
     * Redirects to lessons view if exercise not found or invalid.
     *
     * @param event the before enter navigation event
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        // Extract exerciseId from route parameters
        final var params = event.getRouteParameters();
        final var exerciseIdParam = params.get("exerciseId");

        if (exerciseIdParam.isEmpty()) {
            NotificationUtil.showError("Exercise ID is required");
            event.rerouteTo(LessonsView.class);
            return;
        }

        try {
            this.exerciseId = Long.parseLong(exerciseIdParam.get());
            if (this.exerciseId == null || this.exerciseId <= 0) {
                NotificationUtil.showError("Invalid exercise ID");
                event.rerouteTo(LessonsView.class);
                return;
            }
        } catch (final NumberFormatException e) {
            NotificationUtil.showError("Invalid exercise ID");
            event.rerouteTo(LessonsView.class);
            return;
        }

        // Load exercise from database
        final Optional<ExerciseViewDto> exerciseOpt = this.exerciseService.findById(this.exerciseId);
        if (exerciseOpt.isEmpty()) {
            NotificationUtil.showError("Exercise not found");
            event.rerouteTo(LessonsView.class);
            return;
        }

        this.exercise = exerciseOpt.get();

        // Check if exercise is published (students can only see published exercises)
        if (!Boolean.TRUE.equals(this.exercise.published)) {
            NotificationUtil.showError("This exercise is not available");
            event.rerouteTo(LessonsView.class);
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

        this.problemSolved = false;

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
        titleLayout.setSpacing(true);

        final var titleSection = new VerticalLayout();
        titleSection.setPadding(false);
        titleSection.setSpacing(false);

        final var title = new H2(this.exercise.title);
        title.getStyle().set("margin", "0");
        titleSection.add(title);

        // Add difficulty badge right under the title
        if (this.exercise.graspableDifficulty != null) {
            final var badge = new Span("Difficulty: " + this.exercise.graspableDifficulty.getValue());
            badge.getElement().getThemeList().add("badge");
            switch (this.exercise.graspableDifficulty) {
                case BEGINNER:
                    badge.getElement().getThemeList().add("success");
                    break;
                case INTERMEDIATE:
                    badge.getElement().getThemeList().add("contrast");
                    break;
                case ADVANCED:
                case EXPERT:
                    badge.getElement().getThemeList().add("error");
                    break;
                default:
                    // Unknown difficulty - keep default styling
                    break;
            }
            badge.getStyle().set("margin-top", "0.5rem");
            titleSection.add(badge);
        }

        this.backButton = new Button("← Back to Exercises", ignored -> {
            final var ui = this.getUI().orElse(null);
            if (ui != null) {
                ui.navigate(LessonsView.class);
            }
        });
        this.backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        titleLayout.add(titleSection, this.backButton);
        titleLayout.setFlexGrow(1, titleSection);
        header.add(titleLayout);

        // Completion status indicator (if applicable)
        if (Boolean.TRUE.equals(this.exercise.userCompleted)) {
            final var completionInfo = new Paragraph();
            completionInfo.getStyle()
                    .set("color", "var(--lumo-success-color)")
                    .set("font-weight", "500")
                    .set("margin", "0.5rem 0 0 0");
            final String pluralSuffix = this.exercise.userCompletionCount != null
                    && this.exercise.userCompletionCount > 1
                            ? "times"
                            : "time";
            completionInfo.setText("✓ You have completed this exercise "
                    + (this.exercise.userCompletionCount != null ? this.exercise.userCompletionCount : 1)
                    + " " + pluralSuffix);
            header.add(completionInfo);
        }

        // Exercise content/instructions in a separate section
        if (this.exercise.content != null && !this.exercise.content.isBlank()) {
            final var contentSection = new VerticalLayout();
            contentSection.setPadding(true);
            contentSection.setSpacing(true);
            contentSection.getStyle()
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("margin-top", "1rem");

            final var instructionsHeader = new H4("Instructions");
            instructionsHeader.getStyle().set("margin-top", "0");
            contentSection.add(instructionsHeader);

            final var contentDiv = new Div();
            contentDiv.getStyle().set("white-space", "pre-wrap");
            contentDiv.add(new Text(this.exercise.content));
            contentSection.add(contentDiv);

            header.add(contentSection);
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

        this.requestHintButton = new Button("Request Hint", ignored -> this.showNextHint());
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
                    .set("height", AppConstants.CANVAS_HEIGHT_WORKSPACE)
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
                : AppConstants.AVATAR_DEFAULT_USER;
        final String tutorAvatar = currentUserEntity != null && currentUserEntity.tutorAvatarEmoji != null
                ? currentUserEntity.tutorAvatarEmoji
                : AppConstants.AVATAR_DEFAULT_TUTOR;
        this.chatPanel = new AiChatPanel(this::handleUserQuestion, userAvatar, tutorAvatar);

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

    /**
     * Attaches event listener when view is added to the UI tree.
     * Initializes Graspable Math JavaScript widget if enabled for the exercise.
     *
     * @param attachEvent the attach event containing lifecycle information
     */
    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initialize Graspable Math widget using external JavaScript (only if enabled)
        if (Boolean.TRUE.equals(this.exercise.graspableEnabled)) {
            this.initializeGraspableMath();
        }
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        // Use detachEvent.getUI(), NOT getUI() — may return empty during detach.
        this.pendingAsyncFutures.values().forEach(future -> {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        });
        this.pendingAsyncFutures.clear();
        // Nullify the JS connector to prevent stale callbacks
        final var ui = detachEvent.getUI();
        if (ui != null) {
            ui.getPage().executeJs("if (window.graspableViewConnector) { window.graspableViewConnector = null; }");
        }
        super.onDetach(detachEvent);
    }

    /**
     * Initializes the Graspable Math JavaScript widget using the external file.
     * This loads the problem from the exercise configuration.
     */
    private void initializeGraspableMath() {
        if (this.exercise.graspableInitialExpression == null
                || this.exercise.graspableInitialExpression.isBlank()) {
            LOG.warn("No initial expression configured for exercise {}", this.exerciseId);
            return;
        }

        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }

        // Load the external JavaScript file
        ui.getPage().addJavaScript("/js/graspable-math-init.js");

        // Initialize canvas and load problem once ready
        ui.getPage().executeJs(
                """
                        var initAttempts = 0;
                        var maxInitAttempts = 50;
                        var checkAndInitialize = function() {
                          if (window.initializeGraspableMath) {
                            window.initializeGraspableMath();
                            var loadProblemWhenReady = function() {
                              if (window.graspableCanvas && window.graspableMathUtils) {
                                console.log('[Exercise] Canvas ready, loading problem');
                                window.graspableMathUtils.loadProblem($0, 100, 50);
                              } else {
                                console.log('[Exercise] Waiting for canvas...');
                                setTimeout(loadProblemWhenReady, 200);
                              }
                            };
                            setTimeout(loadProblemWhenReady, 500);
                          } else {
                            initAttempts++;
                            if (initAttempts < maxInitAttempts) {
                              console.log('[Exercise] Waiting for initializeGraspableMath... attempt ' + initAttempts);
                              setTimeout(checkAndInitialize, 100);
                            } else {
                              console.error('[Exercise] Graspable Math initialization function not found after ' + maxInitAttempts + ' attempts');
                            }
                          }
                        };
                        checkAndInitialize();
                        """,
                this.exercise.graspableInitialExpression);

        // Register server-side connector
        this.registerServerConnector();
    }

    /**
     * Registers a server-side connector that JavaScript can call.
     */
    private void registerServerConnector() {
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        ui.getPage().executeJs(
                "window.graspableViewConnector = { onMathAction: function(type, before, after) { "
                        + "   $0.$server.onMathAction(type, before, after); "
                        + "}}",
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
        // By default, assume all student actions are correct (they're performing valid
        // math operations)
        // TODO: Implement validation logic to determine if the action is correct.
        // The current design correctly handles completion checking via
        // `checkCompletion()` which compares against the target expression.
        // Without a robust math validation library, setting `event.correct = true`
        // for valid math operations (which Graspable Math already validates on the
        // frontend) is a reasonable interim approach
        event.correct = true;

        // Add event to conversation context
        this.conversationContext.addAction(event);

        final boolean wasAlreadySolved = this.problemSolved;

        // Check if problem is completed (only if target expression is defined)
        if (!wasAlreadySolved && this.exercise.graspableTargetExpression != null
                && !this.exercise.graspableTargetExpression.isBlank()) {
            final boolean isComplete = this.graspableMathService.checkCompletion(
                    expressionAfter,
                    this.exercise.graspableTargetExpression);

            if (isComplete) {
                event.isComplete = true;
                this.problemSolved = true;
                // Mark session as completed
                this.graspableMathService.markSessionComplete(this.currentSessionId);

                // Show success notification
                final var notifyUi = this.getUI().orElse(null);
                if (notifyUi != null) {
                    notifyUi.access(() -> {
                        NotificationUtil.showSuccess("🎉 Congratulations! You've solved the problem correctly!");
                    });
                }
            }
        }

        // Process event through GraspableMathService (for session tracking)
        this.graspableMathService.processEvent(event);

        // Skip action analysis if problem was already solved before this action
        if (wasAlreadySolved) {
            return;
        }

        // Get AI feedback asynchronously (may return null if action is insignificant)
        // Don't show typing indicator for math actions - only show it when we get
        // actual feedback
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        final var userIdForRateLimit = event.studentId != null ? String.valueOf(event.studentId) : "ANONYMOUS";

        final var rootFuture = this.aiTutorService
                .analyzeMathActionAsync(event, this.conversationContext, userIdForRateLimit);
        final String requestId = UUID.randomUUID().toString();
        this.pendingAsyncFutures.put(requestId, rootFuture);
        rootFuture.thenAccept(feedback -> {
            ui.access(() -> {
                // Only log and display if we got feedback
                if (feedback != null) {
                    // Log interaction
                    this.aiTutorService.logInteraction(event, feedback);

                    // Build feedback message with hints
                    final StringBuilder fullMessage = new StringBuilder(feedback.message);
                    if (feedback.hints != null && !feedback.hints.isEmpty()) {
                        for (final String hint : feedback.hints) {
                            fullMessage.append("\n💡 ").append(hint);
                        }
                    }

                    final var message = ChatMessageDto.aiFeedback(fullMessage.toString());
                    message.sessionId = this.currentSessionId;
                    this.conversationContext.addAiMessage(message);
                    this.chatPanel.addMessage(message);
                }
            });
        }).exceptionally(ex -> {
            ui.access(() -> {
                LOG.error("Error getting AI feedback", ex);
            });
            return null;
        }).whenComplete((result, throwable) -> this.pendingAsyncFutures.remove(requestId));
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

        // Get user ID and exercise ID before async call (to avoid context issues)
        final var userId = this.authService.getUserId();
        final var userIdStr = userId != null ? String.valueOf(userId) : "ANONYMOUS";
        final var exerciseId = this.exercise != null ? this.exercise.id : null;
        final var sessionId = this.currentSessionId;

        // Get AI answer asynchronously
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            this.chatPanel.hideTypingIndicator();
            return;
        }
        final var rootFuture = this.aiTutorService
                .answerQuestionAsync(question, this.currentExpression,
                        this.exercise != null ? this.exercise.graspableInitialExpression : null,
                        this.exercise != null ? this.exercise.graspableTargetExpression : null,
                        this.currentSessionId, this.conversationContext, userIdStr);
        final String requestId = UUID.randomUUID().toString();
        this.pendingAsyncFutures.put(requestId, rootFuture);
        rootFuture.thenAccept(answer -> {
            // Log the question and answer interaction BEFORE UI access (to ensure proper
            // transaction context)
            if (sessionId != null) {
                try {
                    this.aiTutorService.logQuestionInteraction(
                            sessionId,
                            userId,
                            exerciseId,
                            question,
                            answer.message);
                } catch (final Exception e) {
                    LOG.warn("Failed to log question interaction", e);
                }
            }

            ui.access(() -> {
                // Hide typing indicator
                this.chatPanel.hideTypingIndicator();

                // Add AI answer to conversation context
                this.conversationContext.addAiMessage(answer);

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
                }).whenComplete((result, throwable) -> this.pendingAsyncFutures.remove(requestId));
    }

    private void showNextHint() {
        if (this.exercise.graspableHints == null || this.exercise.graspableHints.isBlank()) {
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
