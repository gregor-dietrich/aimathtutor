package de.vptr.aimathtutor.view.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
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
public class AIChatPanel extends VerticalLayout {

    private final VerticalLayout chatHistoryPanel;
    private final TextField chatInput;
    private final Button sendButton;
    private final MessageSendListener messageSendListener;

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
    public AIChatPanel(final MessageSendListener messageSendListener) {
        this.messageSendListener = messageSendListener;

        // Apply right panel styling
        this.setWidth("30%");
        this.setSpacing(true);
        this.setPadding(true);
        this.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-left", "1px solid var(--lumo-contrast-10pct)");

        // Chat header
        final var chatHeader = new H4("AI Tutor Chat");
        chatHeader.getStyle().set("margin-top", "0");

        // Chat history panel
        this.chatHistoryPanel = new VerticalLayout();
        this.chatHistoryPanel.setSpacing(true);
        this.chatHistoryPanel.setPadding(false);
        this.chatHistoryPanel.setHeightFull();
        this.chatHistoryPanel.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        this.chatHistoryPanel.getStyle()
                .set("overflow-y", "auto")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("flex-shrink", "1");

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
        this.chatInput.getStyle().set("flex-grow", "1");

        this.add(chatHeader, this.chatHistoryPanel, inputLayout);
        this.setFlexGrow(1, this.chatHistoryPanel);
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
            // Container for alignment
            final var messageContainer = new Div();
            messageContainer.getStyle()
                    .set("display", "flex")
                    .set("margin-bottom", "var(--lumo-space-s)");

            // Message bubble
            final var messageDiv = new Div();
            messageDiv.getStyle()
                    .set("padding", "var(--lumo-space-s)")
                    .set("border-radius", "var(--lumo-border-radius-l)")
                    .set("max-width", "80%")
                    .set("word-wrap", "break-word");

            // Style based on sender
            if (message.sender == ChatMessageDto.Sender.USER) {
                // User messages: right-aligned, 20% margin on left
                messageContainer.getStyle()
                        .set("justify-content", "flex-end")
                        .set("margin-left", "20%");
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-primary-color-10pct)")
                        .set("border", "1px solid var(--lumo-primary-color-50pct)");
            } else if (message.messageType == ChatMessageDto.MessageType.SYSTEM) {
                // System messages: centered with distinct styling
                messageContainer.getStyle()
                        .set("justify-content", "center");
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-contrast-5pct)")
                        .set("border", "1px solid var(--lumo-contrast-20pct)")
                        .set("font-style", "italic")
                        .set("text-align", "center")
                        .set("color", "var(--lumo-secondary-text-color)");
            } else {
                // AI messages: left-aligned, 20% margin on right
                messageContainer.getStyle()
                        .set("justify-content", "flex-start")
                        .set("margin-right", "20%");
                messageDiv.getStyle()
                        .set("background-color", "var(--lumo-contrast-10pct)")
                        .set("border", "1px solid var(--lumo-contrast-20pct)");
            }

            // Add icon based on message type
            String icon = "";
            if (message.sender == ChatMessageDto.Sender.USER) {
                icon = "🧒 ";
            } else if (message.messageType == ChatMessageDto.MessageType.SYSTEM) {
                icon = "ℹ️ ";
            } else {
                icon = "🧑‍🏫 ";
            }

            final var messagePara = new Paragraph(icon + message.message);
            messagePara.getStyle().set("margin", "0").set("white-space", "pre-wrap");

            messageDiv.add(messagePara);
            messageContainer.add(messageDiv);

            this.chatHistoryPanel.add(messageContainer);

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
     * Clears all messages from the chat panel.
     */
    public void clearMessages() {
        this.chatHistoryPanel.removeAll();
    }
}
