package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.vptr.aimathtutor.dto.ChatMessageDto;

/**
 * Reusable AI chat panel component for displaying chat messages and handling
 * user input.
 * This component is designed to be used as a right-side panel (30% width) with
 * proper styling and layout that matches across all views.
 * Provides a chat-style interface with proper message alignment and styling.
 */
public class AiChatPanel extends VerticalLayout {

    private final VerticalLayout chatHistoryPanel;
    private final TextField chatInput;
    private final Button sendButton;
    private final MessageSendListener messageSendListener;
    private String userAvatarEmoji = "🧒";
    private String tutorAvatarEmoji = "🧑‍🏫";
    private HorizontalLayout currentTypingIndicator; // Track current typing indicator

    /**
     * Callback interface for handling message sends.
     */
    @FunctionalInterface
    public interface MessageSendListener {
        void onMessageSend(String message);
    }

    /**
     * Creates a new AI chat panel.
     *
     * @param messageSendListener Callback to invoke when user sends a message
     */
    public AiChatPanel(final MessageSendListener messageSendListener) {
        this(messageSendListener, "🧒", "🧑‍🏫");
    }

    /**
     * Creates a new AI chat panel with custom avatars.
     *
     * @param messageSendListener Callback to invoke when user sends a message
     * @param userAvatarEmoji     The emoji to use for user messages
     * @param tutorAvatarEmoji    The emoji to use for AI tutor messages
     */
    public AiChatPanel(final MessageSendListener messageSendListener,
            final String userAvatarEmoji,
            final String tutorAvatarEmoji) {
        this.messageSendListener = messageSendListener;
        this.userAvatarEmoji = userAvatarEmoji != null ? userAvatarEmoji : "🧒";
        this.tutorAvatarEmoji = tutorAvatarEmoji != null ? tutorAvatarEmoji : "🧑‍🏫";

        // Apply right panel styling
        this.setWidth("30%");
        this.setSpacing(true);
        this.setPadding(true);
        this.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-left", "1px solid var(--lumo-contrast-10pct)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("position", "sticky")
                .set("top", "0")
                .set("height", "calc(100vh - 100px)") // Subtract approximate header height
                .set("overflow", "hidden"); // Prevent the panel itself from scrolling

        // Chat header
        final var chatHeader = new H4("AI Tutor Chat");
        chatHeader.getStyle()
                .set("margin-top", "0")
                .set("flex-shrink", "0");

        // Chat history panel
        this.chatHistoryPanel = new VerticalLayout();
        this.chatHistoryPanel.setSpacing(true);
        this.chatHistoryPanel.setPadding(false);
        this.chatHistoryPanel.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        this.chatHistoryPanel.getStyle()
                .set("flex-grow", "1") // Grow to fill available space
                .set("flex-shrink", "1") // Allow shrinking if needed
                .set("overflow-y", "auto") // Only the history scrolls
                .set("min-height", "0") // Important for flex shrinking
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Chat input area
        this.chatInput = new TextField();
        this.chatInput.setPlaceholder("Ask me a question...");
        this.chatInput.setWidthFull();
        this.chatInput.setValueChangeMode(ValueChangeMode.EAGER);

        this.sendButton = new Button("Send", VaadinIcon.PAPERPLANE.create());
        this.sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.sendButton.setEnabled(false);
        this.sendButton.addClickListener(e -> this.handleSend());

        // Set up value change listener after sendButton is initialized
        this.chatInput.addValueChangeListener(e -> this.sendButton.setEnabled(!e.getValue().trim().isEmpty()));

        // Allow Enter key to send
        this.chatInput.addKeyPressListener(Key.ENTER, e -> {
            if (!this.chatInput.isEmpty()) {
                this.handleSend();
            }
        });

        final var inputLayout = new HorizontalLayout(this.chatInput, this.sendButton);
        inputLayout.setWidthFull();
        inputLayout.setSpacing(true);
        inputLayout.setAlignItems(Alignment.END);
        inputLayout.getStyle().set("flex-shrink", "0"); // Don't shrink input area
        this.chatInput.getStyle().set("flex-grow", "1");

        this.add(chatHeader, this.chatHistoryPanel, inputLayout);
        this.setFlexGrow(1, this.chatHistoryPanel); // Let chat history grow to fill space
    }

    /**
     * Handles sending a message.
     */
    private void handleSend() {
        final String message = this.chatInput.getValue().trim();
        if (message.isEmpty()) {
            return;
        }

        // Clear input and disable button
        this.chatInput.clear();
        this.sendButton.setEnabled(false);

        // Notify listener
        if (this.messageSendListener != null) {
            this.messageSendListener.onMessageSend(message);
        }
    }

    /**
     * Adds a chat message to the chat panel.
     *
     * @param message The message to add
     */
    public void addMessage(final ChatMessageDto message) {
        UI.getCurrent().access(() -> {
            // Outer container for the entire message row (avatar + bubble)
            final var messageRow = new HorizontalLayout();
            messageRow.setWidthFull();
            messageRow.setSpacing(true);
            messageRow.setPadding(false);
            messageRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");

            // Avatar label (outside bubble)
            final var avatarLabel = new Span();
            avatarLabel.getStyle()
                    .set("font-size", "1.5rem")
                    .set("flex-shrink", "0")
                    .set("align-self", "flex-end");

            // Message bubble
            final var messageDiv = new Div();
            messageDiv.getStyle()
                    .set("padding", "var(--lumo-space-s)")
                    .set("border-radius", "var(--lumo-border-radius-l)")
                    .set("max-width", "80%")
                    .set("word-wrap", "break-word");

            final var messagePara = new Paragraph(message.message);
            messagePara.getStyle().set("margin", "0").set("white-space", "pre-wrap");
            messageDiv.add(messagePara);

            // Style based on sender
            if (message.sender == ChatMessageDto.Sender.USER) {
                // User messages: right-aligned, avatar on right
                messageRow.setJustifyContentMode(JustifyContentMode.END);
                avatarLabel.setText(this.userAvatarEmoji);
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-primary-color-10pct)")
                        .set("border", "1px solid var(--lumo-primary-color-50pct)");
                messageRow.add(messageDiv, avatarLabel);
            } else if (message.messageType == ChatMessageDto.MessageType.SYSTEM) {
                // System messages: centered, no avatar
                messageRow.setJustifyContentMode(JustifyContentMode.CENTER);
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-contrast-5pct)")
                        .set("border", "1px solid var(--lumo-contrast-20pct)")
                        .set("font-style", "italic")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("max-width", "80%");
                messageRow.add(messageDiv);
            } else {
                // AI messages: left-aligned, avatar on left
                messageRow.setJustifyContentMode(JustifyContentMode.START);
                avatarLabel.setText(this.tutorAvatarEmoji);
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-contrast-10pct)")
                        .set("border", "1px solid var(--lumo-contrast-20pct)");
                messageRow.add(avatarLabel, messageDiv);
            }

            this.chatHistoryPanel.add(messageRow);

            // Auto-scroll to bottom
            UI.getCurrent().getPage().executeJs(
                    "const panel = $0; panel.scrollTop = panel.scrollHeight;",
                    this.chatHistoryPanel.getElement());

            // Limit chat history to 20 messages
            if (this.chatHistoryPanel.getComponentCount() > 20) {
                this.chatHistoryPanel.remove(this.chatHistoryPanel.getComponentAt(0));
            }
        });
    }

    /**
     * Shows a typing indicator (simple version that doesn't require tracking the
     * indicator).
     */
    public void showTypingIndicator() {
        // Remove any existing typing indicator first
        this.hideTypingIndicator();

        this.currentTypingIndicator = this.createTypingIndicator();
    }

    /**
     * Creates and displays a typing indicator.
     *
     * @return The typing indicator component for manual management
     */
    public HorizontalLayout createTypingIndicator() {
        final var typingRow = new HorizontalLayout();
        typingRow.setWidthFull();
        typingRow.setSpacing(true);
        typingRow.setPadding(false);
        typingRow.setJustifyContentMode(JustifyContentMode.START);
        typingRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        // Avatar
        final var avatarLabel = new Span(this.tutorAvatarEmoji);
        avatarLabel.getStyle()
                .set("font-size", "1.5rem")
                .set("flex-shrink", "0")
                .set("align-self", "flex-end");

        // Typing indicator
        final var typingDiv = new Div();
        typingDiv.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        final var typingText = new Span("typing...");
        typingText.getStyle()
                .set("font-style", "italic")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("animation", "pulse 1.5s ease-in-out infinite");
        typingDiv.add(typingText);

        typingRow.add(avatarLabel, typingDiv);

        UI.getCurrent().access(() -> {
            this.chatHistoryPanel.add(typingRow);
            // Auto-scroll to bottom
            UI.getCurrent().getPage().executeJs(
                    "const panel = $0; panel.scrollTop = panel.scrollHeight;",
                    this.chatHistoryPanel.getElement());
        });

        return typingRow;
    }

    /**
     * Hides the typing indicator (simple version that uses tracked indicator).
     */
    public void hideTypingIndicator() {
        final HorizontalLayout indicatorToRemove = this.currentTypingIndicator;
        if (indicatorToRemove != null) {
            UI.getCurrent().access(() -> {
                try {
                    this.chatHistoryPanel.remove(indicatorToRemove);
                } catch (final Exception e) {
                    // Indicator might have already been removed, ignore
                }
                this.currentTypingIndicator = null;
            });
        }
    }

    /**
     * Removes a specific typing indicator.
     *
     * @param indicator The indicator component to remove
     */
    public void hideTypingIndicator(final HorizontalLayout indicator) {
        if (indicator != null) {
            UI.getCurrent().access(() -> this.chatHistoryPanel.remove(indicator));
        }
    }

    /**
     * Clears all messages from the chat panel.
     */
    public void clearMessages() {
        this.chatHistoryPanel.removeAll();
    }
}
