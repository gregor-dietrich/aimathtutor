package de.vptr.aimathtutor.component.layout;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.ReplyButton;
import de.vptr.aimathtutor.component.button.ReportButton;
import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.event.CommentCreatedEvent;
import de.vptr.aimathtutor.service.CommentService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;

/**
 * CommentsPanel: Reusable Vaadin component for displaying and creating comments
 * on exercises. Supports threading, permission-based UI, and pagination.
 *
 * NOTE: This component is NOT a CDI bean and is instantiated directly via
 * new().
 * The @Vetoed annotation excludes it from CDI bean discovery.
 * CommentService is looked up programmatically at runtime rather than injected.
 */
@Vetoed
public class CommentsPanel extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(CommentsPanel.class);

    private final Long exerciseId;
    private final String sessionId;
    private final Long currentUserId;
    private final int pageSize = 50;

    private Div commentsContainer;
    private TextArea commentTextArea;
    private Button submitButton;
    private int currentPage = 0;
    private Long currentParentId = null; // For threading

    public CommentsPanel(final Long exerciseId, final String sessionId, final Long currentUserId) {
        this.exerciseId = exerciseId;
        this.sessionId = sessionId;
        this.currentUserId = currentUserId;
        this.initializeUI();
    }

    /**
     * Helper method to retrieve CommentService via CDI programmatic lookup.
     * Used because CommentsPanel is not a CDI bean (instantiated with new()).
     */
    private CommentService getCommentService() {
        return CDI.current().select(CommentService.class).get();
    }

    private void initializeUI() {
        this.setWidthFull();
        this.setPadding(true);
        this.setSpacing(true);
        this.addClassName("comments-panel");

        // Header
        final Span header = new Span("Comments");
        header.addClassName("comments-header");

        // Comments container
        this.commentsContainer = new Div();
        this.commentsContainer.setWidthFull();
        this.commentsContainer.addClassName("comments-list");
        this.commentsContainer.setMaxHeight("400px");
        this.commentsContainer.getStyle().set("overflow-y", "auto");

        // New comment form
        final Div formContainer = this.createCommentForm();

        // Add all components
        this.add(header, this.commentsContainer, formContainer);

        // Load initial comments
        this.loadComments();
    }

    private Div createCommentForm() {
        final Div formContainer = new Div();
        formContainer.setWidthFull();
        formContainer.addClassName("comment-form");

        // Textarea
        this.commentTextArea = new TextArea();
        this.commentTextArea.setLabel("Your comment");
        this.commentTextArea.setPlaceholder("Write a comment...");
        this.commentTextArea.setWidthFull();
        this.commentTextArea.setMaxLength(1000);

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
            final List<CommentViewDto> comments = this.getCommentService()
                    .listCommentsByExercise(this.exerciseId, this.currentPage, this.pageSize, this.currentParentId);
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
        }

        // Add load more button if we got full page
        if (comments.size() >= this.pageSize) {
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

        // Header with author and date
        final String relativeDate = this.formatRelativeDate(comment.created);
        final Span header = new Span(comment.username + " · " + relativeDate);
        header.addClassName("comment-header");

        // Content
        final String displayContent = "DELETED".equals(comment.status) ? "[deleted]" : comment.content;
        final Span content = new Span(displayContent);
        content.addClassName("comment-content");

        // Action buttons
        final Div actions = new Div();
        actions.addClassName("comment-actions");

        // Show/hide based on status
        if (!"DELETED".equals(comment.status)) {
            final Button replyButton = new ReplyButton(e -> this.onReplyClicked(comment.id));
            actions.add(replyButton);

            if (comment.authorId == null || !comment.authorId.equals(this.currentUserId)) {
                final Button reportButton = new ReportButton(e -> this.onReportClicked(comment.id));
                actions.add(reportButton);
            }
        }

        // Edit/Delete if author
        if (comment.authorId != null && comment.authorId.equals(this.currentUserId)) {
            final Button editButton = new EditButton(e -> this.onEditClicked(comment.id, comment.content));
            actions.add(editButton);

            final Button deleteButton = new DeleteButton(e -> this.onDeleteClicked(comment.id));
            actions.add(deleteButton);
        }

        commentDiv.add(header, content, actions);

        // Add edit timestamp if applicable
        if (comment.editedAt != null) {
            final Span editedNote = new Span("(edited)");
            editedNote.addClassName("comment-edited");
            commentDiv.add(editedNote);
        }

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
            dto.exerciseId = this.exerciseId;
            dto.parentCommentId = this.currentParentId;
            dto.sessionId = this.sessionId;

            // Call service
            this.getCommentService().createComment(dto, this.currentUserId);

            // Clear form
            this.commentTextArea.clear();

            // Reset pagination and reload
            this.currentPage = 0;
            this.loadComments();

            NotificationUtil.showSuccess("Comment posted!");
        } catch (final Exception e) {
            LOG.error("Failed to create comment", e);
            NotificationUtil.showError("Failed to post comment: " + e.getMessage());
        }
    }

    private void onReplyClicked(final Long commentId) {
        this.currentParentId = commentId;
        this.commentTextArea.focus();
        this.commentTextArea.setPlaceholder("Reply to comment...");
        this.submitButton.setText("Post Reply");

        // Load replies
        this.currentPage = 0;
        this.loadComments();
    }

    private void onEditClicked(final Long commentId, final String currentContent) {
        final Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Comment");

        final TextArea editArea = new TextArea();
        editArea.setValue(currentContent);
        editArea.setWidthFull();

        final Button saveButton = new Button("Save", e -> {
            try {
                final CommentDto dto = new CommentDto();
                dto.content = editArea.getValue();
                this.getCommentService().editComment(commentId, dto, this.currentUserId);
                dialog.close();
                this.loadComments();
                NotificationUtil.showSuccess("Comment updated!");
            } catch (final Exception ex) {
                LOG.error("Failed to edit comment", ex);
                NotificationUtil.showError("Failed to edit: " + ex.getMessage());
            }
        });

        final Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(editArea);
        dialog.getFooter().add(saveButton, cancelButton);
        dialog.open();
    }

    private void onDeleteClicked(final Long commentId) {
        final Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Comment");

        final Paragraph message = new Paragraph("Are you sure you want to delete this comment?");
        confirmDialog.add(message);

        final Button confirmButton = new Button("Delete", e -> {
            try {
                this.getCommentService().deleteComment(commentId, this.currentUserId, true); // soft delete
                confirmDialog.close();
                this.loadComments();
                NotificationUtil.showSuccess("Comment deleted!");
            } catch (final Exception ex) {
                LOG.error("Failed to delete comment", ex);
                NotificationUtil.showError("Failed to delete: " + ex.getMessage());
            }
        });

        final Button cancelButton = new Button("Cancel", e -> confirmDialog.close());

        confirmDialog.getFooter().add(confirmButton, cancelButton);
        confirmDialog.open();
    }

    private void onReportClicked(final Long commentId) {
        try {
            this.getCommentService().flagComment(commentId, this.currentUserId, "Comment reported");
            NotificationUtil.showSuccess("Comment flagged for review");
        } catch (final Exception ex) {
            LOG.error("Failed to flag comment", ex);
            NotificationUtil.showError("Failed to flag: " + ex.getMessage());
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

    public void refresh() {
        this.currentPage = 0;
        this.currentParentId = null;
        this.loadComments();
    }

    /**
     * CDI Event observer: Listen for new comments on this exercise.
     * When a new comment is created elsewhere (by another user), this method
     * fires and refreshes the comments panel automatically.
     */
    public void onCommentCreated(@Observes final CommentCreatedEvent event) {
        // Only refresh if the event is for our exercise
        if (event.getExerciseId().equals(this.exerciseId)) {
            LOG.debug("Comment created event received for exercise {}, refreshing comments", this.exerciseId);

            // Must use UI.getCurrent().access() to update the UI from another thread
            UI.getCurrent().access(() -> {
                this.refresh();
                NotificationUtil.showInfo("New comment added");
            });
        }
    }
}
