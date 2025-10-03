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

        // Page permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.pageAdd != null && rank.pageAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Pages");
            }

            final var editIcon = rank.pageEdit != null && rank.pageEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Pages");
            }

            final var deleteIcon = rank.pageDelete != null && rank.pageDelete ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Pages");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Pages").setWidth("150px").setFlexGrow(0);

        // Post permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.postAdd != null && rank.postAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Posts");
            }

            final var editIcon = rank.postEdit != null && rank.postEdit ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Posts");
            }

            final var deleteIcon = rank.postDelete != null && rank.postDelete ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Posts");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Posts").setWidth("150px").setFlexGrow(0);

        // Post category permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.postCategoryAdd != null && rank.postCategoryAdd
                    ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Post Categories");
            }

            final var editIcon = rank.postCategoryEdit != null && rank.postCategoryEdit
                    ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Post Categories");
            }

            final var deleteIcon = rank.postCategoryDelete != null && rank.postCategoryDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Post Categories");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Post Categories").setWidth("150px").setFlexGrow(0);

        // Post comment permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.postCommentAdd != null && rank.postCommentAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Comments");
            }

            final var editIcon = rank.postCommentEdit != null && rank.postCommentEdit
                    ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Comments");
            }

            final var deleteIcon = rank.postCommentDelete != null && rank.postCommentDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Comments");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Post Comments").setWidth("150px").setFlexGrow(0);

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

        // Account permissions
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final var addIcon = rank.userAccountAdd != null && rank.userAccountAdd ? LineAwesomeIcon.PLUS_SOLID.create()
                    : new Span("");
            addIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (addIcon instanceof SvgIcon) {
                ((SvgIcon) addIcon).setTooltipText("Add Accounts");
            }

            final var editIcon = rank.userAccountEdit != null && rank.userAccountEdit
                    ? LineAwesomeIcon.EDIT_SOLID.create()
                    : new Span("");
            editIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (editIcon instanceof SvgIcon) {
                ((SvgIcon) editIcon).setTooltipText("Edit Accounts");
            }

            final var deleteIcon = rank.userAccountDelete != null && rank.userAccountDelete
                    ? LineAwesomeIcon.TRASH_ALT_SOLID.create()
                    : new Span("");
            deleteIcon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            if (deleteIcon instanceof SvgIcon) {
                ((SvgIcon) deleteIcon).setTooltipText("Delete Accounts");
            }

            layout.add(addIcon, editIcon, deleteIcon);
            return layout;
        }).setHeader("Accounts").setWidth("150px").setFlexGrow(0);

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

        // Page permissions
        final var pageAddField = new Checkbox("Can Add Pages");
        final var pageEditField = new Checkbox("Can Edit Pages");
        final var pageDeleteField = new Checkbox("Can Delete Pages");

        // Post permissions
        final var postAddField = new Checkbox("Can Add Posts");
        final var postEditField = new Checkbox("Can Edit Posts");
        final var postDeleteField = new Checkbox("Can Delete Posts");

        // Post category permissions
        final var postCategoryAddField = new Checkbox("Can Add Post Categories");
        final var postCategoryEditField = new Checkbox("Can Edit Post Categories");
        final var postCategoryDeleteField = new Checkbox("Can Delete Post Categories");

        // Post comment permissions
        final var postCommentAddField = new Checkbox("Can Add Post Comments");
        final var postCommentEditField = new Checkbox("Can Edit Post Comments");
        final var postCommentDeleteField = new Checkbox("Can Delete Post Comments");

        // User permissions
        final var userAddField = new Checkbox("Can Add Users");
        final var userEditField = new Checkbox("Can Edit Users");
        final var userDeleteField = new Checkbox("Can Delete Users");

        // User group permissions
        final var userGroupAddField = new Checkbox("Can Add User Groups");
        final var userGroupEditField = new Checkbox("Can Edit User Groups");
        final var userGroupDeleteField = new Checkbox("Can Delete User Groups");

        // Account permissions
        final var userAccountAddField = new Checkbox("Can Add User Accounts");
        final var userAccountEditField = new Checkbox("Can Edit User Accounts");
        final var userAccountDeleteField = new Checkbox("Can Delete User Accounts");

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

        // Page permissions bindings
        this.binder.bind(pageAddField, rank1 -> rank1.pageAdd != null ? rank1.pageAdd : false,
                (rank1, value) -> rank1.pageAdd = value);
        this.binder.bind(pageEditField, rank1 -> rank1.pageEdit != null ? rank1.pageEdit : false,
                (rank1, value) -> rank1.pageEdit = value);
        this.binder.bind(pageDeleteField, rank1 -> rank1.pageDelete != null ? rank1.pageDelete : false,
                (rank1, value) -> rank1.pageDelete = value);

        // Post permissions bindings
        this.binder.bind(postAddField, rank1 -> rank1.postAdd != null ? rank1.postAdd : false,
                (rank1, value) -> rank1.postAdd = value);
        this.binder.bind(postEditField, rank1 -> rank1.postEdit != null ? rank1.postEdit : false,
                (rank1, value) -> rank1.postEdit = value);
        this.binder.bind(postDeleteField, rank1 -> rank1.postDelete != null ? rank1.postDelete : false,
                (rank1, value) -> rank1.postDelete = value);

        // Post category permissions bindings
        this.binder.bind(postCategoryAddField, rank1 -> rank1.postCategoryAdd != null ? rank1.postCategoryAdd : false,
                (rank1, value) -> rank1.postCategoryAdd = value);
        this.binder.bind(postCategoryEditField,
                rank1 -> rank1.postCategoryEdit != null ? rank1.postCategoryEdit : false,
                (rank1, value) -> rank1.postCategoryEdit = value);
        this.binder.bind(postCategoryDeleteField,
                rank1 -> rank1.postCategoryDelete != null ? rank1.postCategoryDelete : false,
                (rank1, value) -> rank1.postCategoryDelete = value);

        // Post comment permissions bindings
        this.binder.bind(postCommentAddField, rank1 -> rank1.postCommentAdd != null ? rank1.postCommentAdd : false,
                (rank1, value) -> rank1.postCommentAdd = value);
        this.binder.bind(postCommentEditField, rank1 -> rank1.postCommentEdit != null ? rank1.postCommentEdit : false,
                (rank1, value) -> rank1.postCommentEdit = value);
        this.binder.bind(postCommentDeleteField,
                rank1 -> rank1.postCommentDelete != null ? rank1.postCommentDelete : false,
                (rank1, value) -> rank1.postCommentDelete = value);

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

        // Account permissions bindings
        this.binder.bind(userAccountAddField, rank1 -> rank1.userAccountAdd != null ? rank1.userAccountAdd : false,
                (rank1, value) -> rank1.userAccountAdd = value);
        this.binder.bind(userAccountEditField, rank1 -> rank1.userAccountEdit != null ? rank1.userAccountEdit : false,
                (rank1, value) -> rank1.userAccountEdit = value);
        this.binder.bind(userAccountDeleteField,
                rank1 -> rank1.userAccountDelete != null ? rank1.userAccountDelete : false,
                (rank1, value) -> rank1.userAccountDelete = value);

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
        form.add(new H3("Page Permissions"));
        form.add(pageAddField, pageEditField, pageDeleteField);

        form.add(new H3("Post Permissions"));
        form.add(postAddField, postEditField, postDeleteField);

        form.add(new H3("Post Category Permissions"));
        form.add(postCategoryAddField, postCategoryEditField, postCategoryDeleteField);

        form.add(new H3("Post Comment Permissions"));
        form.add(postCommentAddField, postCommentEditField, postCommentDeleteField);

        form.add(new H3("User Permissions"));
        form.add(userAddField, userEditField, userDeleteField);

        form.add(new H3("User Group Permissions"));
        form.add(userGroupAddField, userGroupEditField, userGroupDeleteField);

        form.add(new H3("Account Permissions"));
        form.add(userAccountAddField, userAccountEditField, userAccountDeleteField);

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
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting rank", e);
            NotificationUtil.showError("Unexpected error occurred");
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
