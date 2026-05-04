package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.ManageUsersButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.button.RemoveUserButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.UserGroupDto;
import de.vptr.aimathtutor.dto.UserGroupViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.service.UserGroupService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for managing user groups and their memberships.
 */
@Route(value = "admin/user-groups", layout = AdminMainLayout.class)
public class AdminUserGroupsView extends AbstractAdminView {
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserGroupsView.class);

    @Inject
    private transient UserGroupService groupService;
    @Inject
    private transient UserService userService;

    private transient Grid<UserGroupViewDto> grid;
    private transient TextField searchField;
    private transient Button searchButton;
    private transient IntegerField userIdField;

    private transient Dialog groupDialog;
    private transient Binder<UserGroupDto> binder;
    private transient UserGroupDto currentGroup;

    // User management components
    private transient Dialog userManagementDialog;
    private transient Grid<UserViewDto> userGrid;
    private transient ComboBox<UserViewDto> availableUsersCombo;
    private transient UserGroupViewDto selectedGroup;

    /**
     * Construct the admin view for managing user groups.
     */
    public AdminUserGroupsView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Verify authentication and initialize the view before navigation.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.buildUi();
        this.loadGroupsAsync();
    }

    private void loadGroupsAsync() {
        LOG.info("Starting async group loading");

        AsyncDataLoader.load(
                () -> this.groupService.getAllGroups(),
                this,
                data -> this.grid.setItems(data),
                "Failed to load groups. Please try again.");
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("User Groups");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.groupDialog = new FormDialog();
        this.userManagementDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().isBlank()) {
                        this.loadGroupsAsync();
                    }
                },
                ignored -> this.searchGroups(),
                "Search by name...",
                "Search Groups");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                ignored -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(ignored -> this.openGroupDialog(null));
        final var refreshButton = new RefreshButton(ignored -> this.loadGroupsAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserGroupViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(group -> group.id).setHeader("ID").setWidth(AppConstants.GRID_ID_WIDTH).setFlexGrow(0);

        // Make the name column clickable
        this.grid.addComponentColumn(group -> {
            final var nameSpan = new Span(group.name);
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(ignored -> this.openGroupDialog(group));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2);

        this.grid.addColumn(group -> group.userCount != null ? group.userCount : 0).setHeader("User Count")
                .setWidth("120px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserGroupViewDto group) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignored -> this.openGroupDialog(group));
        final var deleteButton = new DeleteButton(ignored -> this.deleteGroup(group));
        final var manageUsersButton = new ManageUsersButton(ignored -> this.openUserManagementDialog(group));

        layout.add(editButton, deleteButton, manageUsersButton);
        return layout;
    }

    private void openGroupDialog(final UserGroupViewDto group) {
        this.groupDialog.removeAll();
        this.currentGroup = group != null ? group.toUserGroupDto() : new UserGroupDto();

        this.binder = new Binder<>(UserGroupDto.class);

        final var title = new H3(group != null ? "Edit Group" : "Create Group");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.binder.forField(nameField)
                .withValidator(value -> value != null && !value.isBlank(), "Name is required")
                .bind(group1 -> group1.name, (group1, value) -> group1.name = value);

        form.add(nameField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", ignored -> this.saveGroup());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignored -> this.groupDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.groupDialog.add(dialogLayout);

        // Load current group data
        this.binder.readBean(this.currentGroup);

        this.groupDialog.open();
    }

    private void saveGroup() {
        try {
            // Validate the form before attempting to save
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentGroup);

            if (this.currentGroup.id == null) {
                this.groupService.createGroup(this.currentGroup);
                NotificationUtil.showSuccess("Group created successfully");
            } else {
                this.groupService.updateGroup(this.currentGroup.id, this.currentGroup);
                NotificationUtil.showSuccess("Group updated successfully");
            }

            this.groupDialog.close();
            this.loadGroupsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteGroup(final UserGroupViewDto group) {
        try {
            if (this.groupService.deleteGroup(group.id)) {
                NotificationUtil.showSuccess("Group deleted successfully");
                this.loadGroupsAsync();
            } else {
                NotificationUtil.showError("Failed to delete group");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchGroups() {
        final String query = this.searchField.getValue();
        if (query == null || query.isBlank()) {
            this.loadGroupsAsync();
            return;
        }

        this.searchButton.setEnabled(false);
        LOG.info("Starting async group search with query: {}", query);

        AsyncDataLoader.load(
                () -> this.groupService.searchGroups(query),
                this,
                groups -> {
                    this.searchButton.setEnabled(true);
                    this.grid.setItems(groups);
                },
                () -> this.searchButton.setEnabled(true),
                "Failed to search groups. Please try again.");
    }

    private void openUserManagementDialog(final UserGroupViewDto group) {
        this.selectedGroup = group;
        this.userManagementDialog.removeAll();

        final var title = new H3("Manage Users for Group: " + group.name);

        // Create user grid
        this.userGrid = new Grid<>(UserViewDto.class, false);
        this.userGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.userGrid.setHeight("300px");

        this.userGrid.addColumn(user -> user.id).setHeader("ID").setWidth(AppConstants.GRID_ID_WIDTH).setFlexGrow(0);
        this.userGrid.addColumn(user -> user.username).setHeader("Username").setFlexGrow(1);
        this.userGrid.addColumn(user -> user.email != null ? user.email : "no email").setHeader("Email").setFlexGrow(1);

        // Add remove button column
        this.userGrid.addComponentColumn(user -> {
            final var removeButton = new RemoveUserButton(ignored -> this.removeUserFromGroup(user));
            return removeButton;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        // Create add user section
        final var addUserLayout = new HorizontalLayout();
        addUserLayout.setAlignItems(Alignment.END);
        addUserLayout.setSpacing(true);

        this.availableUsersCombo = new ComboBox<>("Add User");
        this.availableUsersCombo.setItemLabelGenerator(
                user -> user.username + " (" + (user.email != null ? user.email : "no email") + ")");
        this.availableUsersCombo.setWidth("300px");
        this.availableUsersCombo.setPlaceholder("Select a user to add...");

        final var addButton = new Button("Add User", ignored -> this.addUserToGroup());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addUserLayout.add(this.availableUsersCombo, addButton);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignored -> this.loadGroupUsers());
        final var closeButton = new Button("Close", ignored -> this.userManagementDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(refreshButton, closeButton);

        final var dialogLayout = new VerticalLayout(title, this.userGrid, addUserLayout, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.userManagementDialog.add(dialogLayout);

        // Load current users and available users
        this.loadGroupUsers();
        this.loadAvailableUsers();

        this.userManagementDialog.open();
    }

    private void loadGroupUsers() {
        AsyncDataLoader.load(
                () -> this.groupService.getUsersInGroup(this.selectedGroup.id),
                this,
                users -> this.userGrid.setItems(users),
                "Unexpected error occurred");
    }

    private void loadAvailableUsers() {
        AsyncDataLoader.load(
                () -> {
                    final var allUsers = this.userService.getAllUsers();
                    if (allUsers == null) {
                        return List.<UserViewDto>of();
                    }
                    final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                            .collect(Collectors.toList());
                    final var currentUserIds = currentUsers.stream()
                            .map(user -> user.id)
                            .collect(Collectors.toSet());
                    return allUsers.stream()
                            .filter(user -> !currentUserIds.contains(user.id))
                            .collect(Collectors.toList());
                },
                this,
                availableUsers -> this.availableUsersCombo.setItems(availableUsers),
                "Error loading available users");
    }

    private void addUserToGroup() {
        final var selectedUser = this.availableUsersCombo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return;
        }

        // Check if user is already in the group
        final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                .collect(Collectors.toList());
        final boolean alreadyInGroup = currentUsers.stream()
                .anyMatch(user -> user.id.equals(selectedUser.id));

        if (alreadyInGroup) {
            NotificationUtil.showWarning("User is already a member of this group");
            this.availableUsersCombo.clear();
            return;
        }

        try {
            this.groupService.addUserToGroup(selectedUser.id, this.selectedGroup.id);
            NotificationUtil.showSuccess("User added to group successfully");
            // Refresh user lists for the dialog
            this.loadGroupUsers();
            this.loadAvailableUsers(); // Refresh the combo to exclude the newly added user
            // Also refresh the main groups grid so computed columns (user count) update
            this.loadGroupsAsync();
            this.availableUsersCombo.clear();
        } catch (final Exception e) {
            LOG.error("Unexpected error adding user to group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void removeUserFromGroup(final UserViewDto user) {
        try {
            if (this.groupService.removeUserFromGroup(user.id, this.selectedGroup.id)) {
                NotificationUtil.showSuccess("User removed from group successfully");
                // Refresh user lists for the dialog
                this.loadGroupUsers();
                this.loadAvailableUsers(); // Refresh the combo to include the removed user
                // Also refresh the main groups grid so computed columns (user count) update
                this.loadGroupsAsync();
            } else {
                NotificationUtil.showError("Failed to remove user from group");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error removing user from group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            this.loadGroupsAsync();
            return;
        }

        AsyncDataLoader.load(
                () -> this.groupService.getGroupsForUser(userId.longValue()),
                this,
                groups -> this.grid.setItems(groups),
                "Failed to filter groups");
    }
}
