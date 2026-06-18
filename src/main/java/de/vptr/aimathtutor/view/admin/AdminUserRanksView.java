package de.vptr.aimathtutor.view.admin;

import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;

/**
 * Admin view for creating, editing and deleting user ranks (permission sets).
 */
@Route(value = "admin/user-ranks", layout = AdminMainLayout.class)
@SuppressWarnings("NullAway")
public class AdminUserRanksView extends AbstractAdminView {

    private static final Logger LOG = Logger.getLogger(AdminUserRanksView.class);

    private Grid<UserRankViewDto> grid;
    private TextField searchField;
    private Button searchButton;

    private Dialog rankDialog;
    private Binder<UserRankDto> binder;
    private transient UserRankDto currentRank;

    /**
     * Constructs the AdminUserRanksView with full size and padding.
     */
    public AdminUserRanksView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        super.onAttach(event);
        this.loadRanksAsync();
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");
        AsyncDataLoader.load(() -> this.userRankService.getAllRanks(), this, ranks -> this.grid.setItems(ranks),
                "Failed to load ranks. Please try again.");
    }

    /**
     * Construct UI elements for the user ranks admin view.
     */
    @Override
    protected void buildUi() {
        this.removeAll();

        final var header = new H2("User Ranks");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.rankDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    /**
     * Create the search layout for searching ranks.
     *
     * @return the created search layout
     */
    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().isBlank()) {
                this.loadRanksAsync();
            }
        }, e -> this.searchRanks(), "Search by name...", "Search Ranks");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    /**
     * Create the button layout containing create/refresh actions.
     *
     * @return the horizontal layout with action buttons
     */
    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openRankDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadRanksAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserRankViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Make the name column clickable
        this.grid.addComponentColumn(rank -> {
            final var nameSpan = new Span(rank.name);
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(e -> this.openRankDialog(rank));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2);

        // View permissions: single check/ban icon
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);
            layout.add(this.permissionIcon(
                    Boolean.TRUE.equals(rank.adminView) ? LineAwesomeIcon.CHECK_SOLID : LineAwesomeIcon.BAN_SOLID,
                    "Admin View"));
            return layout;
        }).setHeader("Admin View").setWidth("110px").setFlexGrow(0);

        // CRUD permission columns share the same add/edit/delete icon pattern
        this.addCrudPermissionColumn("Exercises", "Exercises", rank -> Boolean.TRUE.equals(rank.exerciseAdd),
                rank -> Boolean.TRUE.equals(rank.exerciseEdit), rank -> Boolean.TRUE.equals(rank.exerciseDelete));
        this.addCrudPermissionColumn("Lessons", "Lessons", rank -> Boolean.TRUE.equals(rank.lessonAdd),
                rank -> Boolean.TRUE.equals(rank.lessonEdit), rank -> Boolean.TRUE.equals(rank.lessonDelete));
        this.addCrudPermissionColumn("Comments", "Comments", rank -> Boolean.TRUE.equals(rank.commentAdd),
                rank -> Boolean.TRUE.equals(rank.commentEdit), rank -> Boolean.TRUE.equals(rank.commentDelete));
        this.addCrudPermissionColumn("Users", "Users", rank -> Boolean.TRUE.equals(rank.userAdd),
                rank -> Boolean.TRUE.equals(rank.userEdit), rank -> Boolean.TRUE.equals(rank.userDelete));
        this.addCrudPermissionColumn("User Groups", "User Groups", rank -> Boolean.TRUE.equals(rank.userGroupAdd),
                rank -> Boolean.TRUE.equals(rank.userGroupEdit), rank -> Boolean.TRUE.equals(rank.userGroupDelete));
        this.addCrudPermissionColumn("User Ranks", "Ranks", rank -> Boolean.TRUE.equals(rank.userRankAdd),
                rank -> Boolean.TRUE.equals(rank.userRankEdit), rank -> Boolean.TRUE.equals(rank.userRankDelete));

        // AI configuration permission column
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);
            layout.add(this.permissionIconOrPlaceholder(Boolean.TRUE.equals(rank.aiConfigEdit),
                    LineAwesomeIcon.EDIT_SOLID, "Edit AI Configuration"));
            return layout;
        }).setHeader("AI Config").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions")
                .setWidth(AppConstants.GRID_ACTION_WIDTH).setFlexGrow(0);
    }

    /**
     * Append a CRUD permission column showing add/edit/delete icons whose visibility is driven by per-rank booleans.
     *
     * @param header
     *            column header text (e.g. "Exercises")
     * @param entityName
     *            entity noun used in tooltips (e.g. "Exercises", "Ranks")
     */
    private void addCrudPermissionColumn(final String header, final String entityName,
            final Predicate<UserRankViewDto> canAdd, final Predicate<UserRankViewDto> canEdit,
            final Predicate<UserRankViewDto> canDelete) {
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);
            layout.add(
                    this.permissionIconOrPlaceholder(canAdd.test(rank), LineAwesomeIcon.PLUS_SOLID,
                            "Add " + entityName),
                    this.permissionIconOrPlaceholder(canEdit.test(rank), LineAwesomeIcon.EDIT_SOLID,
                            "Edit " + entityName),
                    this.permissionIconOrPlaceholder(canDelete.test(rank), LineAwesomeIcon.TRASH_ALT_SOLID,
                            "Delete " + entityName));
            return layout;
        }).setHeader(header).setWidth(AppConstants.GRID_ACTION_WIDTH).setFlexGrow(0);
    }

    private Component permissionIconOrPlaceholder(final boolean enabled, final LineAwesomeIcon icon,
            final String label) {
        return enabled ? this.permissionIcon(icon, label) : this.iconPlaceholder();
    }

    private SvgIcon permissionIcon(final LineAwesomeIcon icon, final String label) {
        final var svg = icon.create();
        svg.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
        svg.setTooltipText(label);
        svg.getElement().setAttribute("aria-label", label);
        return svg;
    }

    private Component iconPlaceholder() {
        final var span = new Span("");
        span.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
        return span;
    }

    /**
     * Create action buttons for a rank row (edit/delete).
     *
     * @param rank
     *            the rank dto
     * @return a horizontal layout with action buttons
     */
    private HorizontalLayout createActionButtons(final UserRankViewDto rank) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openRankDialog(rank));
        final var deleteButton = new DeleteButton(e -> this.deleteRank(rank));

        layout.add(editButton, deleteButton);
        return layout;
    }

    /**
     * Open a dialog to edit or create a user rank.
     *
     * @param rank
     *            the rank to edit or null to create a new one
     */
    private void openRankDialog(@Nullable final UserRankViewDto rank) {
        this.rankDialog.removeAll();
        this.currentRank = rank != null ? rank.toUserRankDto() : new UserRankDto();

        this.binder = new Binder<>(UserRankDto.class);

        final var title = new H3(rank != null ? "Edit Rank" : "Create Rank");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state

        // Viewpermissions
        final var adminViewField = new Checkbox("Admin View");

        // Exercise permissions
        final var exerciseAddField = new Checkbox("Can Add Exercises");
        final var exerciseEditField = new Checkbox("Can Edit Exercises");
        final var exerciseDeleteField = new Checkbox("Can Delete Exercises");

        // Lesson permissions
        final var lessonAddField = new Checkbox("Can Add Lessons");
        final var lessonEditField = new Checkbox("Can Edit Lessons");
        final var lessonDeleteField = new Checkbox("Can Delete Lessons");

        // Comment permissions
        final var commentAddField = new Checkbox("Can Add Comments");
        final var commentEditField = new Checkbox("Can Edit Comments");
        final var commentDeleteField = new Checkbox("Can Delete Comments");

        // User permissions
        final var userAddField = new Checkbox("Can Add Users");
        final var userEditField = new Checkbox("Can Edit Users");
        final var userDeleteField = new Checkbox("Can Delete Users");

        // User group permissions
        final var userGroupAddField = new Checkbox("Can Add User Groups");
        final var userGroupEditField = new Checkbox("Can Edit User Groups");
        final var userGroupDeleteField = new Checkbox("Can Delete User Groups");

        // User rank permissions
        final var userRankAddField = new Checkbox("Can Add User Ranks");
        final var userRankEditField = new Checkbox("Can Edit User Ranks");
        final var userRankDeleteField = new Checkbox("Can Delete User Ranks");

        // AI configuration permissions
        final var aiConfigEditField = new Checkbox("Can Edit AI Configuration");

        // Bind fields
        this.binder.forField(nameField).withValidator(value -> value != null && !value.isBlank(), "Name is required")
                .bind(rank1 -> rank1.name, (rank1, value) -> rank1.name = value);

        // View permissions bindings
        this.binder.bind(adminViewField, rank1 -> rank1.adminView, (rank1, value) -> rank1.adminView = value);

        // Exercise permissions bindings
        this.binder.bind(exerciseAddField, rank1 -> rank1.exerciseAdd, (rank1, value) -> rank1.exerciseAdd = value);
        this.binder.bind(exerciseEditField, rank1 -> rank1.exerciseEdit, (rank1, value) -> rank1.exerciseEdit = value);
        this.binder.bind(exerciseDeleteField, rank1 -> rank1.exerciseDelete,
                (rank1, value) -> rank1.exerciseDelete = value);

        // Lesson permissions bindings
        this.binder.bind(lessonAddField, rank1 -> rank1.lessonAdd, (rank1, value) -> rank1.lessonAdd = value);
        this.binder.bind(lessonEditField, rank1 -> rank1.lessonEdit, (rank1, value) -> rank1.lessonEdit = value);
        this.binder.bind(lessonDeleteField, rank1 -> rank1.lessonDelete, (rank1, value) -> rank1.lessonDelete = value);

        // Comment permissions bindings
        this.binder.bind(commentAddField, rank1 -> rank1.commentAdd, (rank1, value) -> rank1.commentAdd = value);
        this.binder.bind(commentEditField, rank1 -> rank1.commentEdit, (rank1, value) -> rank1.commentEdit = value);
        this.binder.bind(commentDeleteField, rank1 -> rank1.commentDelete,
                (rank1, value) -> rank1.commentDelete = value);

        // User permissions bindings
        this.binder.bind(userAddField, rank1 -> rank1.userAdd, (rank1, value) -> rank1.userAdd = value);
        this.binder.bind(userEditField, rank1 -> rank1.userEdit, (rank1, value) -> rank1.userEdit = value);
        this.binder.bind(userDeleteField, rank1 -> rank1.userDelete, (rank1, value) -> rank1.userDelete = value);

        // User group permissions bindings
        this.binder.bind(userGroupAddField, rank1 -> rank1.userGroupAdd, (rank1, value) -> rank1.userGroupAdd = value);
        this.binder.bind(userGroupEditField, rank1 -> rank1.userGroupEdit,
                (rank1, value) -> rank1.userGroupEdit = value);
        this.binder.bind(userGroupDeleteField, rank1 -> rank1.userGroupDelete,
                (rank1, value) -> rank1.userGroupDelete = value);

        // User rank permissions bindings
        this.binder.bind(userRankAddField, rank1 -> rank1.userRankAdd, (rank1, value) -> rank1.userRankAdd = value);
        this.binder.bind(userRankEditField, rank1 -> rank1.userRankEdit, (rank1, value) -> rank1.userRankEdit = value);
        this.binder.bind(userRankDeleteField, rank1 -> rank1.userRankDelete,
                (rank1, value) -> rank1.userRankDelete = value);

        // AI configuration permissions bindings
        this.binder.bind(aiConfigEditField, rank1 -> rank1.aiConfigEdit, (rank1, value) -> rank1.aiConfigEdit = value);

        form.add(nameField);

        // Add section headers and organize permissions in sections
        form.add(new H3("View Permissions"));
        form.add(adminViewField);

        // Add section headers and organize permissions in sections
        form.add(new H3("Exercise Permissions"));
        form.add(exerciseAddField, exerciseEditField, exerciseDeleteField);

        form.add(new H3("Lesson Permissions"));
        form.add(lessonAddField, lessonEditField, lessonDeleteField);

        form.add(new H3("Comment Permissions"));
        form.add(commentAddField, commentEditField, commentDeleteField);

        form.add(new H3("User Permissions"));
        form.add(userAddField, userEditField, userDeleteField);

        form.add(new H3("User Group Permissions"));
        form.add(userGroupAddField, userGroupEditField, userGroupDeleteField);

        form.add(new H3("User Rank Permissions"));
        form.add(userRankAddField, userRankEditField, userRankDeleteField);

        form.add(new H3("AI Configuration Permissions"));
        form.add(aiConfigEditField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveRank());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.rankDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        // Make the form scrollable
        form.getStyle().set("overflow-y", "auto");
        form.setMaxHeight("400px");

        this.rankDialog.add(dialogLayout);

        // Load current rank data
        this.binder.readBean(this.currentRank);

        this.rankDialog.open();
    }

    private void saveRank() {
        try {
            // Validate the form before attempting to save
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentRank);

            if (this.currentRank.publicId == null) {
                this.userRankService.createRank(this.currentRank);
                NotificationUtil.showSuccess("Rank created successfully");
            } else {
                this.userRankService.updateRank(this.currentRank.publicId, this.currentRank);
                NotificationUtil.showSuccess("Rank updated successfully");
            }

            this.rankDialog.close();
            this.loadRanksAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage() != null ? e.getMessage() : "Permission denied");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteRank(final UserRankViewDto rank) {
        try {
            if (rank.publicId != null && this.userRankService.deleteRank(rank.publicId)) {
                NotificationUtil.showSuccess("Rank deleted successfully");
                this.loadRanksAsync();
            } else {
                NotificationUtil.showError("Failed to delete rank");
            }
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage() != null ? e.getMessage() : "Permission denied");
        } catch (final WebApplicationException e) {
            LOG.error("Error deleting rank", e);
            NotificationUtil.showError("Failed to delete rank. Please try again.");
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchRanks() {
        final String query = this.searchField.getValue();
        if (query == null || query.isBlank()) {
            this.loadRanksAsync();
            return;
        }

        this.searchButton.setEnabled(false);
        LOG.infof("Starting async rank search with query: %s", query);

        AsyncDataLoader.load(() -> this.userRankService.searchRanks(query), this, ranks -> {
            this.searchButton.setEnabled(true);
            this.grid.setItems(ranks);
        }, () -> this.searchButton.setEnabled(true), "Failed to search ranks. Please try again.");
    }
}
