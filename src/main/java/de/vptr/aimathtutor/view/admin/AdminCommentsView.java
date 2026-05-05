package de.vptr.aimathtutor.view.admin;

import java.time.LocalDate;
import java.util.function.Consumer;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.HideButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.button.RestoreButton;
import de.vptr.aimathtutor.component.button.ShowButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.CommentDto;
import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.dto.CommentViewDto;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.service.CommentService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Administrative view for managing comments. Provides search, filtering,
 * editing and moderation tools for administrators.
 */
@Route(value = "admin/comments", layout = AdminMainLayout.class)
public class AdminCommentsView extends AbstractAdminView {

    private static final Logger LOG = LoggerFactory.getLogger(AdminCommentsView.class);

    @Inject
    private transient CommentService commentService;
    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private Grid<CommentViewDto> grid;
    private TextField searchField;
    private Button searchButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;
    private IntegerField exerciseIdField;
    private Select<String> statusFilterSelect;
    private IntegerField flagsFilterField;

    private Dialog commentDialog;
    private Binder<CommentDto> binder;
    private transient CommentDto currentComment;

    /**
     * Construct the admin comments view and initialize layout properties.
     */
    public AdminCommentsView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Lifecycle callback invoked before entering the view. Ensures the user is
     * authenticated and initializes the UI and data loading.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.buildUi();

        // If navigated with an exerciseId query parameter, filter comments
        final var params = event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("exerciseId")) {
            try {
                final Long exerciseId = Long.valueOf(params.get("exerciseId").get(0));
                if (exerciseId == null || exerciseId <= 0) {
                    LOG.warn("Invalid exerciseId parameter: not a positive number");
                } else {
                    // Load comments for that exercise only
                    AsyncDataLoader.load(
                            () -> this.commentService.findByExerciseId(exerciseId),
                            this,
                            comments -> this.grid.setItems(comments),
                            "Failed to load comments. Please try again.");
                    return;
                }
            } catch (final Exception ex) {
                LOG.warn("Invalid exerciseId parameter: {}", params.get("exerciseId"), ex);
            }
        }

        this.loadCommentsAsync();
    }

    private void loadCommentsAsync() {
        LOG.info("Loading comments");

        AsyncDataLoader.load(
                () -> this.commentService.getAllComments(),
                this,
                data -> this.grid.setItems(data),
                "Failed to load comments. Please try again.");
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Comments");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.commentDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().isBlank()) {
                        this.loadCommentsAsync();
                    }
                },
                ignored -> this.searchComments(),
                "Search by author or content...",
                "Search Comments");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(ignored -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                ignored -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        // Exercise ID filter
        final var exerciseFilterLayout = new IntegerFilterLayout(
                ignored -> this.filterByExerciseId(),
                "Enter Exercise ID...",
                "Filter by Exercise");
        this.exerciseIdField = exerciseFilterLayout.getIntegerField();

        // Status filter (VISIBLE, HIDDEN, DELETED)
        this.statusFilterSelect = new Select<>();
        this.statusFilterSelect.setLabel("Filter by Status");
        this.statusFilterSelect.setItems(CommentStatus.VISIBLE.getValue(), CommentStatus.HIDDEN.getValue(),
                CommentStatus.DELETED.getValue());
        this.statusFilterSelect.setValue(CommentStatus.VISIBLE.getValue());
        this.statusFilterSelect.addValueChangeListener(ignored -> this.filterByStatus());

        // Flags filter (show comments with N+ flags)
        this.flagsFilterField = new IntegerField();
        this.flagsFilterField.setLabel("Min Flags (0+)");
        this.flagsFilterField.setMin(0);
        this.flagsFilterField.setMax(1000);
        this.flagsFilterField.setValue(0);
        this.flagsFilterField.setWidth("150px");
        this.flagsFilterField.addValueChangeListener(ignored -> this.filterByFlags());

        final var statusAndFlagsLayout = new HorizontalLayout(this.statusFilterSelect, this.flagsFilterField);
        statusAndFlagsLayout.setAlignItems(Alignment.END);
        statusAndFlagsLayout.setSpacing(true);

        searchLayout.add(dateFilterLayout, userFilterLayout, exerciseFilterLayout, statusAndFlagsLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignored -> this.loadCommentsAsync());

        layout.add(refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(CommentViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(comment -> comment.publicId).setHeader("ID").setWidth(AppConstants.GRID_ID_WIDTH).setFlexGrow(0);

        // Exercise title column
        this.grid.addComponentColumn(comment -> {
            final var title = comment.exerciseTitle != null ? comment.exerciseTitle : "(No title)";
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("color", "var(--lumo-contrast-70pct)");
            titleSpan.getStyle().set("font-weight", "500");
            return titleSpan;
        }).setHeader("Exercise").setWidth(AppConstants.GRID_NAME_WIDTH).setFlexGrow(1);

        // Author column
        this.grid.addColumn(comment -> comment.username != null ? comment.username : "(Unknown)")
                .setHeader("Author").setWidth("120px").setFlexGrow(0);

        // Content column with limited display
        this.grid.addComponentColumn(comment -> {
            final var content = comment.content != null ? comment.content : "";
            final var truncated = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            final var span = new Span(truncated);
            final var safeTitle = content.replaceAll("<[^>]*>", "").trim();
            span.setTitle(safeTitle.length() > 200 ? safeTitle.substring(0, 200) + "..." : safeTitle);
            return span;
        }).setHeader("Content").setFlexGrow(2);

        this.grid.addColumn(comment -> this.dateTimeFormatter.formatDateTime(comment.created)).setHeader("Created")
                .setWidth("180px").setFlexGrow(0);

        // Status column
        this.grid.addComponentColumn(comment -> {
            final var status = comment.status != null ? comment.status : CommentStatus.VISIBLE;
            final var statusSpan = new Span(status.getValue());
            statusSpan.getStyle().set("font-weight", "600");

            // Color-code status
            if (CommentStatus.VISIBLE.equals(status)) {
                statusSpan.getStyle().set("color", "var(--lumo-success-color)");
            } else if (CommentStatus.HIDDEN.equals(status)) {
                statusSpan.getStyle().set("color", "var(--lumo-warning-color)");
            } else if (CommentStatus.DELETED.equals(status)) {
                statusSpan.getStyle().set("color", "var(--lumo-error-color)");
            }
            statusSpan.getElement().setAttribute("aria-label", status.getValue());

            return statusSpan;
        }).setHeader("Status").setWidth("100px").setFlexGrow(0);

        // Flags column
        this.grid.addColumn(comment -> String.valueOf(comment.flagsCount))
                .setHeader("Flags").setWidth(AppConstants.GRID_ID_WIDTH).setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions")
                .setWidth(AppConstants.GRID_NAME_WIDTH).setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final CommentViewDto comment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        // Edit button
        final var editButton = new EditButton(ignored -> this.openCommentDialog(comment.toCommentDto()));

        // Hide/Show button (toggle moderation)
        Button moderateButton;
        if (CommentStatus.VISIBLE.equals(comment.status)) {
            moderateButton = new HideButton(ignored -> this.hideComment(comment));
        } else if (CommentStatus.HIDDEN.equals(comment.status)) {
            moderateButton = new ShowButton(ignored -> this.showComment(comment));
        } else {
            // DELETED - offer restore option
            moderateButton = new RestoreButton(ignored -> this.restoreComment(comment));
        }

        // Delete button
        final var deleteButton = new DeleteButton(ignored -> this.deleteComment(comment.toCommentDto()));

        layout.add(editButton, moderateButton, deleteButton);
        return layout;
    }

    private void openCommentDialog(final CommentDto comment) {
        this.commentDialog.removeAll();
        if (comment == null || comment.publicId == null) {
            NotificationUtil.showError("Cannot create new comments from the admin panel");
            return;
        }
        this.currentComment = comment;

        this.binder = new Binder<>(CommentDto.class);

        final var title = new H3("Edit Comment");

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

        final var saveButton = new Button("Save", ignored -> this.saveComment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignored -> this.commentDialog.close());
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

            final var editorId = this.authService.getUserId();
            if (editorId == null) {
                NotificationUtil.showError("You must be logged in to edit comments");
                return;
            }
            this.commentService.editComment(this.currentComment.publicId, this.currentComment, editorId);
            NotificationUtil.showSuccess("Comment updated successfully");

            this.commentDialog.close();
            this.loadCommentsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Error saving comment", e);
            NotificationUtil.showError("An error occurred while saving the comment. Please try again.");
        }
    }

    private void deleteComment(final CommentDto comment) {
        try {
            final var requesterId = this.authService.getUserId();
            if (requesterId == null) {
                NotificationUtil.showError("You must be logged in to delete comments");
                return;
            }
            this.commentService.deleteComment(comment.publicId, requesterId, true);
            NotificationUtil.showSuccess("Comment deleted successfully");
            this.loadCommentsAsync();
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Error deleting comment", e);
            NotificationUtil.showError("An error occurred while deleting the comment. Please try again.");
        }
    }

    private void searchComments() {
        final String query = this.searchField.getValue();
        if (query == null || query.isBlank()) {
            this.loadCommentsAsync();
            return;
        }

        this.searchButton.setEnabled(false);
        LOG.info("Searching comments with query: {}", query);
        AsyncDataLoader.load(
                () -> this.commentService.searchComments(query.trim()),
                this,
                comments -> {
                    LOG.info("Successfully found {} comments", comments.size());
                    this.grid.setItems(comments);
                    this.searchButton.setEnabled(true);
                },
                () -> this.searchButton.setEnabled(true),
                "An error occurred while searching comments. Please try again.");
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

        AsyncDataLoader.load(
                () -> this.commentService.findByDateRange(startDate.toString(), endDate.toString()),
                this,
                comments -> this.grid.setItems(comments),
                "An error occurred while filtering comments. Please try again.");
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        AsyncDataLoader.load(
                () -> this.commentService.findByUserId(userId.longValue()),
                this,
                comments -> this.grid.setItems(comments),
                "An error occurred while filtering comments. Please try again.");
    }

    private void filterByExerciseId() {
        final Integer exerciseId = this.exerciseIdField.getValue();
        if (exerciseId == null) {
            NotificationUtil.showWarning("Please enter an exercise ID");
            return;
        }

        AsyncDataLoader.load(
                () -> this.commentService.findByExerciseId(exerciseId.longValue()),
                this,
                comments -> this.grid.setItems(comments),
                "An error occurred while filtering comments. Please try again.");
    }

    private void filterByStatus() {
        final String status = this.statusFilterSelect.getValue();
        if (status == null) {
            this.loadCommentsAsync();
            return;
        }

        AsyncDataLoader.load(
                () -> this.commentService.findByStatus(CommentStatus.fromString(status)),
                this,
                comments -> this.grid.setItems(comments),
                "An error occurred while filtering comments. Please try again.");
    }

    private void filterByFlags() {
        final Integer minFlags = this.flagsFilterField.getValue();
        if (minFlags == null || minFlags < 0) {
            NotificationUtil.showWarning("Please enter a valid minimum flag count");
            return;
        }

        AsyncDataLoader.load(
                () -> this.commentService.findFlaggedComments(minFlags),
                this,
                comments -> this.grid.setItems(comments),
                "An error occurred while filtering comments. Please try again.");
    }

    private void hideComment(final CommentViewDto comment) {
        this.showModerationReasonDialog("Hide Comment", "Why are you hiding this comment?", reason -> {
            try {
                final var currentUserId = this.authService.getUserId();
                this.commentService.moderateComment(comment.publicId, "HIDE", currentUserId, reason);
                NotificationUtil.showSuccess("Comment hidden successfully");
                this.loadCommentsAsync();
            } catch (final PermissionDeniedException e) {
                NotificationUtil.showError(e.getMessage());
            } catch (final Exception e) {
                LOG.error("Error hiding comment", e);
                NotificationUtil.showError("An error occurred while hiding the comment. Please try again.");
            }
        });
    }

    private void showComment(final CommentViewDto comment) {
        this.showModerationReasonDialog("Show Comment", "Why are you showing this comment again?", reason -> {
            try {
                final var currentUserId = this.authService.getUserId();
                this.commentService.moderateComment(comment.publicId, "SHOW", currentUserId, reason);
                NotificationUtil.showSuccess("Comment shown successfully");
                this.loadCommentsAsync();
            } catch (final PermissionDeniedException e) {
                NotificationUtil.showError(e.getMessage());
            } catch (final Exception e) {
                LOG.error("Error showing comment", e);
                NotificationUtil.showError("An error occurred while showing the comment. Please try again.");
            }
        });
    }

    private void restoreComment(final CommentViewDto comment) {
        this.showModerationReasonDialog("Restore Comment", "Why are you restoring this comment?", reason -> {
            try {
                final var currentUserId = this.authService.getUserId();
                this.commentService.moderateComment(comment.publicId, "RESTORE", currentUserId, reason);
                NotificationUtil.showSuccess("Comment restored successfully");
                this.loadCommentsAsync();
            } catch (final PermissionDeniedException e) {
                NotificationUtil.showError(e.getMessage());
            } catch (final Exception e) {
                LOG.error("Error restoring comment", e);
                NotificationUtil.showError("An error occurred while restoring the comment. Please try again.");
            }
        });
    }

    private void showModerationReasonDialog(final String title, final String label, final Consumer<String> onConfirm) {
        final var dialog = new Dialog();
        dialog.setHeaderTitle(title);

        final var reasonField = new TextArea();
        reasonField.setLabel(label);
        reasonField.setWidthFull();
        reasonField.setHeight("150px");
        reasonField.setMaxLength(500);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var confirmButton = new Button("Confirm", ignored -> {
            onConfirm.accept(reasonField.getValue());
            dialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignored -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(confirmButton, cancelButton);

        final var layout = new VerticalLayout(reasonField, buttonLayout);
        layout.setSpacing(true);
        layout.setPadding(false);

        dialog.add(layout);
        dialog.open();
    }
}
