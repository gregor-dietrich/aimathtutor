package de.vptr.aimathtutor.view.admin;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.rest.dto.PostCommentDto;
import de.vptr.aimathtutor.rest.dto.PostCommentViewDto;
import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.service.AuthService;
import de.vptr.aimathtutor.rest.service.PostCommentService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

@Route(value = "admin/comments", layout = AdminMainLayout.class)
public class AdminPostCommentView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPostCommentView.class);

    @Inject
    PostCommentService commentService;

    @Inject
    AuthService authService;

    private Grid<PostCommentViewDto> grid;
    private TextField searchField;
    private Button searchButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private Dialog commentDialog;
    private Binder<PostCommentDto> binder;
    private PostCommentDto currentComment;

    public AdminPostCommentView() {
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
        this.loadCommentsAsync();
    }

    private void loadCommentsAsync() {
        LOG.info("Loading comments");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Loading comments from service");
            try {
                return this.commentService.getAllComments();
            } catch (final Exception e) {
                LOG.error("Error loading comments", e);
                throw new RuntimeException("Failed to load comments", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((comments, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading comments: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to load comments: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully loaded {} comments", comments.size());
                            this.grid.setItems(comments);
                        }
                    }));
                });
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("Post Comments");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.commentDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadCommentsAsync();
                    }
                },
                e -> this.searchComments(),
                "Search by author or content...",
                "Search Comments");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(e -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                e -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(e -> this.loadCommentsAsync());

        layout.add(refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PostCommentViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(comment -> comment.id).setHeader("ID").setWidth("80px").setFlexGrow(0);

        // Post title column
        this.grid.addComponentColumn(comment -> {
            final var title = comment.postTitle != null ? comment.postTitle : "(No title)";
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("color", "var(--lumo-contrast-70pct)");
            titleSpan.getStyle().set("font-weight", "500");
            return titleSpan;
        }).setHeader("Post").setWidth("200px").setFlexGrow(1);

        // Author column
        this.grid.addColumn(comment -> comment.username != null ? comment.username : "(Unknown)")
                .setHeader("Author").setWidth("120px").setFlexGrow(0);

        // Content column with limited display
        this.grid.addComponentColumn(comment -> {
            final var content = comment.content != null ? comment.content : "";
            final var truncated = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            final var span = new Span(truncated);
            span.setTitle(content); // Show full content on hover
            return span;
        }).setHeader("Content").setFlexGrow(2);

        this.grid.addColumn(comment -> comment.created).setHeader("Created").setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCommentViewDto comment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openCommentDialog(comment.toPostCommentDto()));
        final var deleteButton = new DeleteButton(e -> this.deleteComment(comment.toPostCommentDto()));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openCommentDialog(final PostCommentDto comment) {
        this.commentDialog.removeAll();
        this.currentComment = comment != null ? comment : new PostCommentDto();

        this.binder = new Binder<>(PostCommentDto.class);

        final var title = new H3(comment != null ? "Edit Comment" : "Create Comment");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.binder.bind(contentField, comment1 -> comment1.content, (comment1, value) -> comment1.content = value);

        form.add(contentField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveComment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.commentDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.commentDialog.add(dialogLayout);

        // Load current comment data
        this.binder.readBean(this.currentComment);

        this.commentDialog.open();
    }

    private void saveComment() {
        try {
            this.binder.writeBean(this.currentComment);

            // Sync post field
            this.currentComment.syncPost();

            // Convert DTO to Entity for service call
            final var commentEntity = new PostCommentEntity();
            commentEntity.id = this.currentComment.id;
            commentEntity.content = this.currentComment.content;

            // Set post if specified
            if (this.currentComment.postId != null) {
                final var postEntity = new PostEntity();
                postEntity.id = this.currentComment.postId;
                commentEntity.post = postEntity;
            }

            // Get current username from session
            final var session = com.vaadin.flow.server.VaadinSession.getCurrent();
            final var currentUsername = (String) session.getAttribute("authenticated.username");

            if (this.currentComment.id == null) {
                this.commentService.createComment(commentEntity, currentUsername);
                NotificationUtil.showSuccess("Comment created successfully");
            } else {
                this.commentService.updateComment(commentEntity);
                NotificationUtil.showSuccess("Comment updated successfully");
            }

            this.commentDialog.close();
            this.loadCommentsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Error saving comment", e);
            NotificationUtil.showError("Error saving comment: " + e.getMessage());
        }
    }

    private void deleteComment(final PostCommentDto comment) {
        try {
            if (this.commentService.deleteComment(comment.id)) {
                NotificationUtil.showSuccess("Comment deleted successfully");
                this.loadCommentsAsync();
            } else {
                NotificationUtil.showError("Failed to delete comment");
            }
        } catch (final Exception e) {
            LOG.error("Error deleting comment", e);
            NotificationUtil.showError("Error deleting comment: " + e.getMessage());
        }
    }

    private void searchComments() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            this.loadCommentsAsync();
            return;
        }

        this.searchButton.setEnabled(false);
        LOG.info("Searching comments with query: {}", query);
        try {
            final var comments = this.commentService.searchComments(query.trim());
            LOG.info("Successfully found {} comments", comments.size());
            this.grid.setItems(comments);
        } catch (final Exception e) {
            LOG.error("Error searching comments", e);
            NotificationUtil.showError("Error searching comments: " + e.getMessage());
        } finally {
            this.searchButton.setEnabled(true);
        }
    }

    private void filterByDateRange() {
        final LocalDate startDate = this.startDatePicker.getValue();
        final LocalDate endDate = this.endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        try {
            final var comments = this.commentService.getCommentsByDateRange(startDate.toString(), endDate.toString());
            this.grid.setItems(comments);
        } catch (final Exception e) {
            LOG.error("Error filtering comments by date range", e);
            NotificationUtil.showError("Error filtering comments: " + e.getMessage());
        }
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        try {
            final var comments = this.commentService.getCommentsByUser(userId.longValue());
            this.grid.setItems(comments);
        } catch (final Exception e) {
            LOG.error("Error filtering comments by user", e);
            NotificationUtil.showError("Error filtering comments: " + e.getMessage());
        }
    }
}
