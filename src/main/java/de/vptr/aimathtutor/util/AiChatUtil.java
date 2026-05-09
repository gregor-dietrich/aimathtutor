package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.UI;

import de.vptr.aimathtutor.component.layout.AiChatPanel;
import de.vptr.aimathtutor.dto.ChatMessageDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.annotation.Nullable;

/**
 * Utility for common AI chat panel operations.
 */
public final class AiChatUtil {

    private AiChatUtil() {
    }

    /**
     * Pair of user and tutor avatar strings.
     */
    public static record AvatarPair(String userAvatar, String tutorAvatar) {
    }

    /**
     * Gets the avatar pair for the current user entity.
     *
     * @param currentUserEntity
     *            the current user entity
     * @return the avatar pair
     */
    public static AvatarPair getAvatars(@Nullable final UserEntity currentUserEntity) {
        final String userAvatar = currentUserEntity != null && currentUserEntity.userAvatarEmoji != null
                ? currentUserEntity.userAvatarEmoji : AppConstants.AVATAR_DEFAULT_USER;
        final String tutorAvatar = currentUserEntity != null && currentUserEntity.tutorAvatarEmoji != null
                ? currentUserEntity.tutorAvatarEmoji : AppConstants.AVATAR_DEFAULT_TUTOR;
        return new AvatarPair(userAvatar, tutorAvatar);
    }

    /**
     * Displays an AI answer in the chat panel.
     *
     * @param answer
     *            the answer to display
     * @param chatPanel
     *            the chat panel
     * @param conversationContext
     *            the conversation context
     */
    public static void displayAiAnswer(final ChatMessageDto answer, final AiChatPanel chatPanel,
            final ConversationContextDto conversationContext) {
        chatPanel.hideTypingIndicator();
        conversationContext.addAiMessage(answer);
        chatPanel.addMessage(answer);
    }

    /**
     * Displays an error message in the chat panel.
     *
     * @param ex
     *            the error
     * @param chatPanel
     *            the chat panel
     * @param log
     *            the logger
     */
    public static void displayAiError(final Throwable ex, final AiChatPanel chatPanel, final Logger log) {
        chatPanel.hideTypingIndicator();
        log.error("Error getting AI answer", ex);
        chatPanel.addMessage(ChatMessageDto.aiAnswer("Sorry, I encountered an error. Please try again."));
    }

    /**
     * Handles an async AI answer by updating the UI on the Vaadin UI thread.
     *
     * @param ui
     *            the UI
     * @param answer
     *            the answer
     * @param chatPanel
     *            the chat panel
     * @param conversationContext
     *            the conversation context
     */
    public static void handleAsyncAnswer(final UI ui, final ChatMessageDto answer, final AiChatPanel chatPanel,
            final ConversationContextDto conversationContext) {
        if (ui == null || !ui.isAttached()) {
            return;
        }
        ui.access(() -> displayAiAnswer(answer, chatPanel, conversationContext));
    }

    /**
     * Handles an async error by updating the UI on the Vaadin UI thread.
     *
     * @param ui
     *            the UI
     * @param ex
     *            the error
     * @param chatPanel
     *            the chat panel
     * @param log
     *            the logger
     */
    public static void handleAsyncError(final UI ui, final Throwable ex, final AiChatPanel chatPanel,
            final Logger log) {
        if (ui == null || !ui.isAttached()) {
            return;
        }
        ui.access(() -> displayAiError(ex, chatPanel, log));
    }
}
