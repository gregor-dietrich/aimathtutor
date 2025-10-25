package de.vptr.aimathtutor.view.admin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIcon;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@Route(value = "admin/user-ranks", layout = AdminMainLayout.class)
public class AdminUserRankView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserRankView.class);

    @Inject
    UserRankService rankService;

    @Inject
    AuthService authService;

    private Grid<UserRankViewDto> grid;
    private TextField searchField;
    private Button searchButton;

    private Dialog rankDialog;
    private Binder<UserRankDto> binder;
    private UserRankDto currentRank;

    public AdminUserRankView() {
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
        this.loadRanksAsync();
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Making service call to load ranks");
            try {
                return this.rankService.getAllRanks();
            } catch (final Exception e) {
                LOG.error("Error loading ranks", e);
                throw new RuntimeException("Failed to load ranks", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((ranks, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading ranks: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to load ranks: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully loaded {} ranks", ranks.size());
                            this.grid.setItems(ranks);
                        }
                    }));
                });
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("User Ranks");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.rankDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadRanksAsync();
                    }
                },
                e -> this.searchRanks(),
                "Search by name...",
                "Search Ranks");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

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

        // Configure columns
        this.grid.addColumn(rank -> rank.id).setHeader("ID").setWidth("80px").setFlexGrow(0);

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

        // View permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var adminViewIcon = rank.adminView != null && rank.adminView ? LineAwesomeIcon.CHECK_SOLID.create()
                    : LineAwesomeIcon.BAN_SOLID.create();
            adminViewIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (adminViewIcon instanceof SvgIcon) {
                ((SvgIcon) adminViewIcon).setTooltipText("Admin View");
            }

            layout.add(adminViewIcon);
            return layout;
        }).setHeader("Admin View").setWidth("110px").setFlexGrow(0);

        // Exercise permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.exerciseAdd != null && rank.exerciseAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Exercises");
            }

            final var editIcon = rank.exerciseEdit != null && rank.exerciseEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Exercises");
            }

            final var deleteIcon = rank.exerciseDelete != null && rank.exerciseDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Exercises");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Exercises").setWidth("150px").setFlexGrow(0);

        // Lesson permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.lessonAdd != null && rank.lessonAdd
                    ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Lessons");
            }

            final var editIcon = rank.lessonEdit != null && rank.lessonEdit
                    ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Lessons");
            }

            final var deleteIcon = rank.lessonDelete != null && rank.lessonDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Lessons");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Lessons").setWidth("150px").setFlexGrow(0);

        // Comment permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.commentAdd != null && rank.commentAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Comments");
            }

            final var editIcon = rank.commentEdit != null && rank.commentEdit
                    ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Comments");
            }

            final var deleteIcon = rank.commentDelete != null && rank.commentDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Comments");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Comments").setWidth("150px").setFlexGrow(0);

        // User permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.userAdd != null && rank.userAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Users");
            }

            final var editIcon = rank.userEdit != null && rank.userEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Users");
            }

            final var deleteIcon = rank.userDelete != null && rank.userDelete ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Users");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Users").setWidth("150px").setFlexGrow(0);

        // User group permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.userGroupAdd != null && rank.userGroupAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add User Groups");
            }

            final var editIcon = rank.userGroupEdit != null && rank.userGroupEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit User Groups");
            }

            final var deleteIcon = rank.userGroupDelete != null && rank.userGroupDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete User Groups");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("User Groups").setWidth("150px").setFlexGrow(0);

        // User rank permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.userRankAdd != null && rank.userRankAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Ranks");
            }

            final var editIcon = rank.userRankEdit != null && rank.userRankEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Ranks");
            }

            final var deleteIcon = rank.userRankDelete != null && rank.userRankDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Ranks");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("User Ranks").setWidth("150px").setFlexGrow(0); // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserRankViewDto rank) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openRankDialog(rank));
        final var deleteButton = new DeleteButton(e -> this.deleteRank(rank));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openRankDialog(final UserRankViewDto rank) {
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

        // Bind fields
        this.binder.forField(nameField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Name is required")
                .bind(rank1 -> rank1.name, (rank1, value) -> rank1.name = value);

        // View permissions bindings
        this.binder.bind(adminViewField, rank1 -> rank1.adminView != null ? rank1.adminView : false,
                (rank1, value) -> rank1.adminView = value);

        // Exercise permissions bindings
        this.binder.bind(exerciseAddField, rank1 -> rank1.exerciseAdd != null ? rank1.exerciseAdd : false,
                (rank1, value) -> rank1.exerciseAdd = value);
        this.binder.bind(exerciseEditField, rank1 -> rank1.exerciseEdit != null ? rank1.exerciseEdit : false,
                (rank1, value) -> rank1.exerciseEdit = value);
        this.binder.bind(exerciseDeleteField, rank1 -> rank1.exerciseDelete != null ? rank1.exerciseDelete : false,
                (rank1, value) -> rank1.exerciseDelete = value);

        // Lesson permissions bindings
        this.binder.bind(lessonAddField, rank1 -> rank1.lessonAdd != null ? rank1.lessonAdd : false,
                (rank1, value) -> rank1.lessonAdd = value);
        this.binder.bind(lessonEditField,
                rank1 -> rank1.lessonEdit != null ? rank1.lessonEdit : false,
                (rank1, value) -> rank1.lessonEdit = value);
        this.binder.bind(lessonDeleteField,
                rank1 -> rank1.lessonDelete != null ? rank1.lessonDelete : false,
                (rank1, value) -> rank1.lessonDelete = value);

        // Comment permissions bindings
        this.binder.bind(commentAddField, rank1 -> rank1.commentAdd != null ? rank1.commentAdd : false,
                (rank1, value) -> rank1.commentAdd = value);
        this.binder.bind(commentEditField, rank1 -> rank1.commentEdit != null ? rank1.commentEdit : false,
                (rank1, value) -> rank1.commentEdit = value);
        this.binder.bind(commentDeleteField,
                rank1 -> rank1.commentDelete != null ? rank1.commentDelete : false,
                (rank1, value) -> rank1.commentDelete = value);

        // User permissions bindings
        this.binder.bind(userAddField, rank1 -> rank1.userAdd != null ? rank1.userAdd : false,
                (rank1, value) -> rank1.userAdd = value);
        this.binder.bind(userEditField, rank1 -> rank1.userEdit != null ? rank1.userEdit : false,
                (rank1, value) -> rank1.userEdit = value);
        this.binder.bind(userDeleteField, rank1 -> rank1.userDelete != null ? rank1.userDelete : false,
                (rank1, value) -> rank1.userDelete = value);

        // User group permissions bindings
        this.binder.bind(userGroupAddField, rank1 -> rank1.userGroupAdd != null ? rank1.userGroupAdd : false,
                (rank1, value) -> rank1.userGroupAdd = value);
        this.binder.bind(userGroupEditField, rank1 -> rank1.userGroupEdit != null ? rank1.userGroupEdit : false,
                (rank1, value) -> rank1.userGroupEdit = value);
        this.binder.bind(userGroupDeleteField, rank1 -> rank1.userGroupDelete != null ? rank1.userGroupDelete : false,
                (rank1, value) -> rank1.userGroupDelete = value);

        // User rank permissions bindings
        this.binder.bind(userRankAddField, rank1 -> rank1.userRankAdd != null ? rank1.userRankAdd : false,
                (rank1, value) -> rank1.userRankAdd = value);
        this.binder.bind(userRankEditField, rank1 -> rank1.userRankEdit != null ? rank1.userRankEdit : false,
                (rank1, value) -> rank1.userRankEdit = value);
        this.binder.bind(userRankDeleteField, rank1 -> rank1.userRankDelete != null ? rank1.userRankDelete : false,
                (rank1, value) -> rank1.userRankDelete = value);

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

            if (this.currentRank.id == null) {
                this.rankService.createRank(this.currentRank);
                NotificationUtil.showSuccess("Rank created successfully");
            } else {
                this.rankService.updateRank(this.currentRank.id, this.currentRank);
                NotificationUtil.showSuccess("Rank updated successfully");
            }

            this.rankDialog.close();
            this.loadRanksAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteRank(final UserRankViewDto rank) {
        try {
            if (this.rankService.deleteRank(rank.id)) {
                NotificationUtil.showSuccess("Rank deleted successfully");
                this.loadRanksAsync();
            } else {
                NotificationUtil.showError("Failed to delete rank");
            }
        } catch (final WebApplicationException e) {
            LOG.error("Error deleting rank", e);
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting rank", e);
            NotificationUtil.showError("Unexpected error occurred: " + e.getMessage());
        }
    }

    private void searchRanks() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            this.loadRanksAsync();
            return;
        }

        this.searchButton.setEnabled(false);
        LOG.info("Starting async rank search with query: {}", query);

        // Capture the auth header in the UI thread where VaadinSession is available
        CompletableFuture.supplyAsync(() -> {
            LOG.info("Searching ranks");
            try {
                return this.rankService.searchRanks(query);
            } catch (final Exception e) {
                LOG.error("Error searching ranks", e);
                throw new RuntimeException("Failed to search ranks", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((ranks, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        this.searchButton.setEnabled(true);
                        if (throwable != null) {
                            LOG.error("Error searching ranks: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to search ranks: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully found {} ranks", ranks.size());
                            this.grid.setItems(ranks);
                        }
                    }));
                });
    }
}
