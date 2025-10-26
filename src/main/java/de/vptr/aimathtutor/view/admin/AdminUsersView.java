package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.textfield.EmailField;
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
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

@Route(value = "admin/users", layout = AdminMainLayout.class)
public class AdminUsersView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUsersView.class);

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    UserRankService userRankService;

    @Inject
    DateTimeFormatterUtil dateTimeFormatter;

    private Grid<UserViewDto> grid;
    private TextField searchField;
    private Button searchButton;

    private Dialog userDialog;
    private Dialog passwordDialog;
    private Binder<UserDto> binder;
    private UserDto currentUser;
    private List<UserRankViewDto> availableRanks;

    public AdminUsersView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUI();
        this.loadRanksAsync();
        this.loadUsersAsync();
    }

    private void loadUsersAsync() {
        LOG.info("Starting async user loading");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Loading users");
            try {
                return this.userService.getAllUsers();
            } catch (final Exception e) {
                LOG.error("Error loading users", e);
                throw new RuntimeException("Failed to load users", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((users, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading users: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to load users: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully loaded {} users", users.size());
                            this.grid.setItems(users);
                        }
                    }));
                });
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Loading ranks");
            try {
                return this.userRankService.getAllRanks();
            } catch (final Exception e) {
                LOG.error("Error loading ranks", e);
                throw new RuntimeException("Failed to load ranks", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((ranks, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading ranks: {}", throwable.getMessage(), throwable);
                            // Don't show error notification for ranks as it's not critical
                        } else {
                            LOG.info("Successfully loaded {} ranks", ranks.size());
                            this.availableRanks = ranks;
                        }
                    }));
                });
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("Users");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.userDialog = new FormDialog();
        this.passwordDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadUsersAsync();
                    }
                },
                e -> this.searchUsers(),
                "Search by username or email...",
                "Search Users");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openUserDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadUsersAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(user -> user.id).setHeader("ID").setWidth("80px").setFlexGrow(0);

        // Make the username column clickable
        this.grid.addComponentColumn(user -> {
            final var usernameSpan = new Span(user.username);
            usernameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            usernameSpan.getStyle().set("cursor", "pointer");
            usernameSpan.getStyle().set("width", "100%");
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(e -> this.openUserDialog(user));
            return usernameSpan;
        }).setHeader("Username").setFlexGrow(1);

        this.grid.addColumn(user -> user.email).setHeader("Email").setFlexGrow(1);

        this.grid.addColumn(user -> user.rankName != null ? user.rankName : "").setHeader("Rank").setWidth("120px")
                .setFlexGrow(0);

        this.grid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(user.activated != null ? user.activated : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Activated").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(user.banned != null ? user.banned : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Banned").setWidth("100px").setFlexGrow(0);

        this.grid.addColumn(user -> this.dateTimeFormatter.formatDateTime(user.created)).setHeader("Created")
                .setWidth("180px").setFlexGrow(0);

        this.grid.addColumn(user -> this.dateTimeFormatter.formatDateTime(user.lastLogin)).setHeader("Last Login")
                .setWidth("180px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserViewDto user) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openUserDialog(user));
        final var deleteButton = new DeleteButton(e -> this.deleteUser(user));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openUserDialog(final UserViewDto user) {
        this.userDialog.removeAll();
        this.currentUser = user != null ? user.toUserDto() : new UserDto();

        this.binder = new Binder<>(UserDto.class);

        final var title = new H3(user != null ? "Edit User" : "Create User");

        // Add password note for new users
        final VerticalLayout titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.add(title);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setInvalid(false); // Clear any previous validation state

        final var emailField = new EmailField("Email");

        form.add(usernameField, emailField);

        if (user == null) {
            final var passwordField = new TextField("Password");
            passwordField.setRequired(true);
            passwordField.setInvalid(false);
            form.add(passwordField);
            this.binder.forField(passwordField)
                    .asRequired("Password is required")
                    .bind(user1 -> user1.password, (user1, value) -> user1.password = value);
        } else {
            // Create a vertical layout to match the text field styling
            final var passwordLayout = new VerticalLayout();
            passwordLayout.setPadding(false);
            passwordLayout.setMargin(false);
            passwordLayout.setSpacing(false);

            // Add label that matches FormLayout styling
            final var passwordLabel = new Span("Password");
            passwordLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
            passwordLabel.getStyle().set("font-weight", "500");
            passwordLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            passwordLabel.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

            final var passwordButton = new Button("Change Password");
            passwordButton.setPrefixComponent(LineAwesomeIcon.LOCK_SOLID.create());
            passwordButton.addClickListener(e -> this.openPasswordDialog(user));

            passwordLayout.add(passwordLabel, passwordButton);
            form.add(passwordLayout);
        }

        // Rank dropdown
        final var rankField = new ComboBox<UserRankViewDto>("Rank");
        rankField.setItems(this.availableRanks != null ? this.availableRanks : List.of());
        rankField.setItemLabelGenerator(rank -> rank.name);
        rankField.setRequired(true);
        rankField.setPlaceholder("Select a rank");
        rankField.setClearButtonVisible(false);
        rankField.setInvalid(false); // Clear any previous validation state

        final var activatedField = new Checkbox("Activated");
        final var bannedField = new Checkbox("Banned");

        form.add(rankField, activatedField, bannedField);

        // Bind fields with proper validation
        this.binder.forField(usernameField)
                .asRequired("Username is required")
                .bind(user1 -> user1.username, (user1, value) -> user1.username = value);

        this.binder.forField(emailField)
                .withValidator(email -> {
                    if (email == null || email.trim().isEmpty()) {
                        return true; // Empty is allowed
                    }
                    // Simple but effective email regex pattern
                    return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
                }, "Email must be valid or empty")
                .bind(user1 -> user1.email,
                        (user1, value) -> user1.email = (value == null || value.trim().isEmpty()) ? null
                                : value.trim());

        this.binder.bind(activatedField, user1 -> user1.activated != null ? user1.activated : false,
                (user1, value) -> user1.activated = value);
        this.binder.bind(bannedField, user1 -> user1.banned != null ? user1.banned : false,
                (user1, value) -> user1.banned = value);

        // Rank binding - convert between UserRankViewDto and rankId
        this.binder.forField(rankField)
                .asRequired("Rank is required")
                .bind(
                        user1 -> {
                            if (user1.rankId != null && this.availableRanks != null) {
                                return this.availableRanks.stream()
                                        .filter(rank -> rank.id.equals(user1.rankId))
                                        .findFirst()
                                        .orElse(null);
                            }
                            return null;
                        },
                        (user1, value) -> {
                            user1.rankId = value != null ? value.id : null;
                        });

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveUser());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.userDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(titleLayout, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.userDialog.add(dialogLayout);

        // Load current user data
        this.binder.readBean(this.currentUser);

        this.userDialog.open();
    }

    private void openPasswordDialog(final UserViewDto user) {
        this.passwordDialog.removeAll();

        final var title = new H3("Change Password for " + user.username);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var newPasswordField = new TextField("New Password");
        newPasswordField.setRequired(true);
        newPasswordField.setInvalid(false);

        final var confirmPasswordField = new TextField("Confirm Password");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setInvalid(false);

        form.add(newPasswordField, confirmPasswordField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Change Password", e -> {
            final String newPassword = newPasswordField.getValue();
            final String confirmPassword = confirmPasswordField.getValue();

            if (newPassword == null || newPassword.trim().isEmpty()) {
                NotificationUtil.showError("Password is required");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                NotificationUtil.showError("Passwords do not match");
                return;
            }

            this.changeUserPassword(user.id, newPassword);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.passwordDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.passwordDialog.add(dialogLayout);
        this.passwordDialog.open();
    }

    private void changeUserPassword(final Long userId, final String newPassword) {
        try {
            // Create a UserDto with only the password field set
            final var passwordUpdateDto = new UserDto();
            passwordUpdateDto.id = userId;
            passwordUpdateDto.password = newPassword;

            this.userService.patchUser(userId, passwordUpdateDto);
            NotificationUtil.showSuccess("Password changed successfully");
            this.passwordDialog.close();
        } catch (final Exception e) {
            LOG.error("Unexpected error changing password", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void saveUser() {
        try {
            this.binder.writeBean(this.currentUser);

            if (this.currentUser.id == null) {
                // For new users, set a default password
                this.currentUser.password = "defaultPassword123"; // You might want to generate a random password
                                                                  // instead
                this.userService.createUser(this.currentUser);
                NotificationUtil
                        .showSuccess("User created successfully. Use the Password button to set a new password.");
            } else {
                this.userService.updateUser(this.currentUser.id, this.currentUser);
                NotificationUtil.showSuccess("User updated successfully");
            }

            this.userDialog.close();
            this.loadUsersAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving user", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteUser(final UserViewDto user) {
        try {
            if (this.userService.deleteUser(user.id)) {
                NotificationUtil.showSuccess("User deleted successfully");
                this.loadUsersAsync();
            } else {
                NotificationUtil.showError("Failed to delete user");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting user", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchUsers() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.userService.searchUsers(query.trim());
            } catch (final Exception e) {
                LOG.error("Unexpected error searching users", e);
                throw new RuntimeException("Unexpected error occurred", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((users, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        this.searchButton.setEnabled(true);
                        this.searchButton.setText("Search");
                        if (throwable != null) {
                            LOG.error("Error searching users: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError(throwable.getCause() != null ? throwable.getCause().getMessage()
                                    : throwable.getMessage());
                        } else {
                            this.grid.setItems(users);
                        }
                    }));
                });
    }
}
