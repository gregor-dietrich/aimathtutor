package de.vptr.aimathtutor.component.layout;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.ReplyButton;
import de.vptr.aimathtutor.component.button.ReportButton;
import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.event.CommentCreatedEvent;
import de.vptr.aimathtutor.event.CommentCreatedEventBridge;
import de.vptr.aimathtutor.service.CommentService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.enterprise.inject.spi.CDI;

/**
 * CommentsPanel: Reusable Vaadin component for displaying and creating
 * comments on exercises. Supports threading, permission-based UI, and
 * pagination. This component is not a CDI bean and performs a programmatic
 * lookup of {@link CommentService} when needed.
 *
 * NOTE: This class must NOT be @Vetoed and must NOT contain @Observes
 * methods. @Vetoed classes cannot have CDI observers. Real-time refresh
 * uses CommentCreatedEventBridge with a programmatic listener.
 */
public class CommentsPanel extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(CommentsPanel.class);

    private final Long exerciseId;
    private final String exercisePublicId;
    private final String sessionId;
    private final Long currentUserId;
    private static final int pageSize = 50;

    private Div commentsContainer;
    private TextArea commentTextArea;
    private Button submitButton;
    private int currentPage = 0;
    private String currentParentPublicId = null;
    private transient Consumer<CommentCreatedEvent> commentCreatedListener;

    /**
     * Create a comments panel for the specified exercise/session and user.
     *
     * @param exerciseId       id of the exercise to display comments for
     * @param exercisePublicId public id of the exercise to display comments for
     * @param sessionId        external session id (optional)
     * @param currentUserId    current user database id (may be null)
     */
    public CommentsPanel(final Long exerciseId, final String exercisePublicId, final String sessionId,
            final Long currentUserId) {
        this.exerciseId = exerciseId;
        this.exercisePublicId = exercisePublicId;
        this.sessionId = sessionId;
        this.currentUserId = currentUserId;
        this.buildUi();
    }

    /**
     * Helper method to retrieve CommentService via CDI programmatic lookup.
     * Used because CommentsPanel is not a CDI bean (instantiated with new()).
     */
    private CommentService getCommentService() {
        return CDI.current().select(CommentService.class).get();
    }

    private void buildUi() {
        this.setWidthFull();
        this.setPadding(true);
        this.setSpacing(true);
        this.addClassName("comments-panel");

        // Comments container
        this.commentsContainer = new Div();
        this.commentsContainer.setWidthFull();
        this.commentsContainer.addClassName("comments-list");
        this.commentsContainer.setMaxHeight("400px");
        this.commentsContainer.getStyle().set("overflow-y", "auto");

        // New comment form
        final Div formContainer = this.createCommentForm();

        // Add all components
        this.add(this.commentsContainer, formContainer);

        // Load initial comments
        this.loadComments();

        // Register programmatic listener for real-time comment updates
        final var bridge = CDI.current().select(CommentCreatedEventBridge.class).get();
        this.commentCreatedListener = event -> {
            if (event.getExerciseId() != null && event.getExerciseId().equals(this.exerciseId)) {
                this.getUI().ifPresent(ui -> ui.access(this::refresh));
            }
        };
        bridge.addListener(this.commentCreatedListener);
    }

    private Div createCommentForm() {
        final Div formContainer = new Div();
        formContainer.addClassName("comment-form");
        formContainer.getStyle()
                .set("width", "100%")
                .set("border-left", "1px solid var(--lumo-primary-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0 1rem 0 1rem");

        // Textarea
        this.commentTextArea = new TextArea();
        this.commentTextArea.setLabel("Your comment");
        this.commentTextArea.setPlaceholder("Write a comment...");
        this.commentTextArea.setMaxLength(1000);
        this.commentTextArea.getStyle()
                .set("width", "95%")
                .set("background-color", "var(--lumo-base-color)");

        // Submit button
        this.submitButton = new Button("Post Comment");
        this.submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.submitButton.addClickListener(e -> this.onCommentSubmitted());
        this.submitButton.addClickShortcut(Key.ENTER, KeyModifier.CONTROL);

        formContainer.add(this.commentTextArea, this.submitButton);
        return formContainer;
    }

    private void loadComments() {
        try {
            // Always load top-level comments
            final List<CommentViewDto> comments = this.getCommentService()
                    .listCommentsByExercise(this.exerciseId, this.currentPage, pageSize, null);
            this.displayComments(comments);
        } catch (final Exception e) {
            LOG.error("Failed to load comments", e);
            NotificationUtil.showError("Failed to load comments");
        }
    }

    private void displayComments(final List<CommentViewDto> comments) {
        this.commentsContainer.removeAll();

        if (comments.isEmpty()) {
            this.commentsContainer.add(new Span("No comments yet. Be the first to comment!"));
            return;
        }

        for (final CommentViewDto comment : comments) {
            final Div commentDiv = this.createCommentElement(comment);
            this.commentsContainer.add(commentDiv);

            // If this comment has replies, load and display them
            if (comment.parentPublicId == null) {
                try {
                    final List<CommentViewDto> replies = this.getCommentService().findReplies(comment.publicId);
                    if (!replies.isEmpty()) {
                        final Div repliesContainer = new Div();
                        repliesContainer.addClassName("comment-replies");
                        repliesContainer.getStyle()
                                .set("margin-left", "2rem")
                                .set("margin-top", "0.5rem")
                                .set("padding-left", "1rem")
                                .set("border-left", "2px solid var(--lumo-contrast-20pct)");

                        for (final CommentViewDto reply : replies) {
                            repliesContainer.add(this.createCommentElement(reply));
                        }
                        this.commentsContainer.add(repliesContainer);
                    }
                } catch (final Exception e) {
                    LOG.debug("Failed to load replies for comment {}", comment.publicId, e);
                }
            }
        }

        // Add load more button if we got full page
        if (comments.size() >= pageSize) {
            final Button loadMoreButton = new Button("Load More Comments");
            loadMoreButton.addClickListener(e -> {
                this.currentPage++;
                this.loadComments();
            });
            this.commentsContainer.add(loadMoreButton);
        }
    }

    private Div createCommentElement(final CommentViewDto comment) {
        final Div commentDiv = new Div();
        commentDiv.addClassName("comment-item");
        commentDiv.getStyle()
                .set("padding", "1rem")
                .set("margin-bottom", "0.75rem")
                .set("background-color", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Header with author and date - styled differently from content
        final String relativeDate = this.formatRelativeDate(comment.created);
        final Span header = new Span(comment.username + " · " + relativeDate);
        header.addClassName("comment-header");
        header.getStyle()
                .set("display", "block")
                .set("font-weight", "600")
                .set("font-size", "0.875rem")
                .set("color", "var(--lumo-contrast-70pct)");

        // Content with line break from header
        final String displayContent = CommentStatus.DELETED.equals(comment.status) ? "[deleted]" : comment.content;
        final Span content = new Span(displayContent != null ? displayContent : "");
        content.addClassName("comment-content");
        content.getStyle()
                .set("display", "block")
                .set("word-wrap", "break-word")
                .set("white-space", "pre-wrap");

        // Action buttons
        final Div actions = new Div();
        actions.addClassName("comment-actions");
        actions.getStyle()
                .set("display", "flex")
                .set("gap", "0.5rem")
                .set("margin-top", "0.75rem");

        // Show/hide based on status
        if (!CommentStatus.DELETED.equals(comment.status)) {
            final Button replyButton = new ReplyButton(e -> this.onReplyClicked(comment.publicId));
            actions.add(replyButton);

            if (comment.authorId == null || !comment.authorId.equals(this.currentUserId)) {
                final Button reportButton = new ReportButton(e -> this.onReportClicked(comment.publicId));
                actions.add(reportButton);
            }
        }

        // Edit/Delete if author
        if (comment.authorId != null && comment.authorId.equals(this.currentUserId)) {
            final Button editButton = new EditButton(e -> this.onEditClicked(comment.publicId, comment.content));
            actions.add(editButton);

            final Button deleteButton = new DeleteButton(e -> this.onDeleteClicked(comment.publicId));
            actions.add(deleteButton);
        }

        commentDiv.add(header);

        // Add edit timestamp if applicable
        if (comment.lastEdit != null) {
            final Span editedNote = new Span("(edited)");
            editedNote.addClassName("comment-edited");
            editedNote.getStyle()
                    .set("font-size", "0.75rem")
                    .set("color", "var(--lumo-contrast-50pct)");
            commentDiv.add(editedNote);
        }

        commentDiv.add(content, actions);

        return commentDiv;
    }

    private void onCommentSubmitted() {
        final String text = this.commentTextArea.getValue().trim();
        if (text.isEmpty()) {
            NotificationUtil.showError("Comment cannot be empty");
            return;
        }

        try {
            // Create DTO
            final CommentDto dto = new CommentDto();
            dto.content = text;
            dto.exercisePublicId = this.exercisePublicId;
            dto.parentCommentPublicId = this.currentParentPublicId;
            dto.sessionId = this.sessionId;

            // Call service
            this.getCommentService().createComment(dto, this.currentUserId);

            // Clear form and fully exit reply mode
            this.commentTextArea.clear();
            this.refresh();
            this.commentTextArea.setPlaceholder("Write a comment...");
            this.submitButton.setText("Post Comment");

            NotificationUtil.showSuccess("Comment posted!");
        } catch (final Exception e) {
            LOG.error("Failed to create comment", e);
            NotificationUtil.showError("Failed to post comment. Please try again.");
        }
    }

    private void onReplyClicked(final String commentPublicId) {
        this.currentParentPublicId = commentPublicId;
        this.commentTextArea.focus();
        this.commentTextArea.setPlaceholder("Reply to comment...");
        this.submitButton.setText("Post Reply");

        // Don't reload - just scroll to form and focus
        // The replies will be shown inline under the parent comment
        // when we next refresh the view
    }

    private void onEditClicked(final String commentPublicId, final String currentContent) {
        final Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Comment");

        final TextArea editArea = new TextArea();
        editArea.setValue(currentContent);
        editArea.setWidthFull();

        final Button saveButton = new Button("Save", e -> {
            try {
                final CommentDto dto = new CommentDto();
                dto.content = editArea.getValue();
                this.getCommentService().editComment(commentPublicId, dto, this.currentUserId);
                dialog.close();
                this.loadComments();
                NotificationUtil.showSuccess("Comment updated!");
            } catch (final Exception ex) {
                LOG.error("Failed to edit comment", ex);
                NotificationUtil.showError("Failed to edit comment. Please try again.");
            }
        });

        final Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(editArea);
        dialog.getFooter().add(saveButton, cancelButton);
        dialog.open();
    }

    private void onDeleteClicked(final String commentPublicId) {
        try {
            this.getCommentService().deleteComment(commentPublicId, this.currentUserId, true); // soft delete
            this.loadComments();
            NotificationUtil.showSuccess("Comment deleted!");
        } catch (final Exception ex) {
            LOG.error("Failed to delete comment", ex);
            NotificationUtil.showError("Failed to delete comment. Please try again.");
        }
    }

    private void onReportClicked(final String commentPublicId) {
        try {
            this.getCommentService().flagComment(commentPublicId, this.currentUserId, "Comment reported");
            NotificationUtil.showSuccess("Comment flagged for review");
        } catch (final Exception ex) {
            LOG.error("Failed to flag comment", ex);
            NotificationUtil.showError("Failed to flag comment. Please try again.");
        }
    }

    private String formatRelativeDate(final LocalDateTime dateTime) {
        if (dateTime == null) {
            return "unknown";
        }

        final LocalDateTime now = LocalDateTime.now();
        final long minutesAgo = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutesAgo < 1) {
            return "just now";
        } else if (minutesAgo < 60) {
            return minutesAgo + " minute" + (minutesAgo == 1 ? "" : "s") + " ago";
        }

        final long hoursAgo = ChronoUnit.HOURS.between(dateTime, now);
        if (hoursAgo < 24) {
            return hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
        }

        final long daysAgo = ChronoUnit.DAYS.between(dateTime, now);
        if (daysAgo < 30) {
            return daysAgo + " day" + (daysAgo == 1 ? "" : "s") + " ago";
        }

        return dateTime.toString();
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        if (this.commentCreatedListener != null) {
            try {
                final var bridge = CDI.current().select(CommentCreatedEventBridge.class).get();
                bridge.removeListener(this.commentCreatedListener);
            } catch (final Exception e) {
                LOG.debug("Failed to remove comment created listener", e);
            }
        }
        super.onDetach(detachEvent);
    }

    /**
     * Refresh the comments list by resetting pagination and reloading from the
     * service. Safe to call from UI threads.
     */
    public void refresh() {
        this.currentPage = 0;
        this.currentParentPublicId = null;
        this.loadComments();
    }

}
