package de.vptr.aimathtutor.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.UserSettingsDto;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

/**
 * User settings view for changing password and customizing chat avatars.
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
public class UserSettingsView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(UserSettingsView.class);

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;

    private ComboBox<String> userAvatarSelect;
    private ComboBox<String> tutorAvatarSelect;
    private Div previewBox;

    private Long currentUserId;
    private String currentUsername;
    private String currentEmail;

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        // Check authentication
        if (!this.authService.isAuthenticated()) {
            event.rerouteTo(LoginView.class);
            return;
        }

        // Get current user info
        this.currentUserId = this.authService.getUserId();
        if (this.currentUserId == null) {
            NotificationUtil.showError("Could not load user information");
            event.rerouteTo(LessonsView.class);
            return;
        }

        final var user = this.userService.getCurrentUser();
        this.currentUsername = user.username;
        this.currentEmail = user.email;

        this.buildUI();
        this.loadCurrentSettings();
    }

    private void buildUI() {
        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);
        this.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        final var content = new VerticalLayout();
        content.setWidth("800px");
        content.setSpacing(true);
        content.setPadding(false);

        final var title = new H2("Settings");

        // User Info Section (read-only)
        final var userInfoSection = this.createUserInfoSection();

        // Password Section
        final var passwordSection = this.createPasswordSection();

        // Avatar Section
        final var avatarSection = this.createAvatarSection();

        content.add(title, userInfoSection, passwordSection, avatarSection);
        this.add(content);
    }

    private VerticalLayout createUserInfoSection() {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        final var header = new H3("Account Information");
        header.getStyle().set("margin-top", "0");

        final var usernameInfo = new Div();
        usernameInfo.add(new Span("Username: "), new Span(this.currentUsername));

        final var emailInfo = new Div();
        if (this.currentEmail != null && !this.currentEmail.isEmpty()) {
            emailInfo.add(new Span("Email: "), new Span(this.currentEmail));
        } else {
            emailInfo.add(new Span("Email: "), new Span("Not set"));
        }

        final var notice = new Paragraph("Contact an administrator to change your username or email.");
        notice.getStyle().set("font-style", "italic").set("color", "var(--lumo-secondary-text-color)");

        section.add(header, usernameInfo, emailInfo, notice);
        return section;
    }

    private VerticalLayout createPasswordSection() {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        final var header = new H3("Change Password");
        header.getStyle().set("margin-top", "0");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        this.currentPasswordField = new PasswordField("Current Password");
        this.currentPasswordField.setRequired(true);
        this.currentPasswordField.setWidthFull();

        this.newPasswordField = new PasswordField("New Password");
        this.newPasswordField.setRequired(true);
        this.newPasswordField.setHelperText("Minimum 4 characters");
        this.newPasswordField.setWidthFull();

        this.confirmPasswordField = new PasswordField("Confirm New Password");
        this.confirmPasswordField.setRequired(true);
        this.confirmPasswordField.setWidthFull();

        form.add(this.currentPasswordField, this.newPasswordField, this.confirmPasswordField);

        final var changePasswordButton = new Button("Change Password");
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> this.handlePasswordChange());

        section.add(header, form, changePasswordButton);
        return section;
    }

    private VerticalLayout createAvatarSection() {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        final var header = new H3("Chat Avatars");
        header.getStyle().set("margin-top", "0");

        final var description = new Paragraph(
                "Select emojis to represent yourself and the AI tutor in chat conversations.");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        this.userAvatarSelect = new ComboBox<>("Your Avatar");
        this.userAvatarSelect.setItems("🧒", "👦", "👧", "🧑‍🎓", "👨‍🎓", "👩‍🎓", "🐱", "🐶", "🐭", "👽");
        this.userAvatarSelect.addValueChangeListener(e -> this.updatePreview());

        this.tutorAvatarSelect = new ComboBox<>("AI Tutor Avatar");
        this.tutorAvatarSelect.setItems("🤖", "🦉", "🖥️", "💻", "🧑‍🏫", "👨‍🏫", "👩‍🏫", "🧑‍💻", "👨‍💻", "👩‍💻");
        this.tutorAvatarSelect.addValueChangeListener(e -> this.updatePreview());

        form.add(this.userAvatarSelect, this.tutorAvatarSelect);

        // Preview box
        this.previewBox = new Div();
        this.previewBox.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        final var previewLabel = new Span("Preview:");
        previewLabel.getStyle().set("font-weight", "bold");

        final var saveAvatarsButton = new Button("Save Avatars");
        saveAvatarsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveAvatarsButton.addClickListener(e -> this.handleAvatarChange());

        section.add(header, description, form, previewLabel, this.previewBox, saveAvatarsButton);
        return section;
    }

    private void updatePreview() {
        this.previewBox.removeAll();

        final String userEmoji = this.userAvatarSelect.getValue();
        final String tutorEmoji = this.tutorAvatarSelect.getValue();

        // User message example
        final var userMessage = new HorizontalLayout();
        userMessage.setWidthFull();
        userMessage.setJustifyContentMode(JustifyContentMode.END);
        userMessage.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        final var userBubble = new Div();
        userBubble.setText(userEmoji + " Hello, can you help me?");
        userBubble.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-primary-color-50pct)");
        userMessage.add(userBubble);

        // AI message example
        final var aiMessage = new HorizontalLayout();
        aiMessage.setWidthFull();
        aiMessage.setJustifyContentMode(JustifyContentMode.START);

        final var aiBubble = new Div();
        aiBubble.setText(tutorEmoji + " Of course! I'm here to help.");
        aiBubble.getStyle()
                .set("padding", "var(--lumo-space-s)")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
        aiMessage.add(aiBubble);

        this.previewBox.add(userMessage, aiMessage);
    }

    private void loadCurrentSettings() {
        try {
            final UserSettingsDto settings = this.userService.getSettings(this.currentUserId);
            this.userAvatarSelect.setValue(settings.userAvatarEmoji);
            this.tutorAvatarSelect.setValue(settings.tutorAvatarEmoji);
            this.updatePreview();
        } catch (final Exception e) {
            LOG.error("Failed to load settings", e);
            NotificationUtil.showError("Failed to load settings");
        }
    }

    private void handlePasswordChange() {
        final String currentPassword = this.currentPasswordField.getValue();
        final String newPassword = this.newPasswordField.getValue();
        final String confirmPassword = this.confirmPasswordField.getValue();

        // Validation
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            NotificationUtil.showError("Please enter your current password");
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            NotificationUtil.showError("Please enter a new password");
            return;
        }

        if (newPassword.length() < 4) {
            NotificationUtil.showError("New password must be at least 4 characters long");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            NotificationUtil.showError("New password and confirmation do not match");
            return;
        }

        if (currentPassword.equals(newPassword)) {
            NotificationUtil.showError("New password must be different from current password");
            return;
        }

        try {
            this.userService.changePassword(this.currentUserId, currentPassword, newPassword);
            NotificationUtil.showSuccess("Password changed successfully");

            // Clear fields
            this.currentPasswordField.clear();
            this.newPasswordField.clear();
            this.confirmPasswordField.clear();
        } catch (final ValidationException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Failed to change password", e);
            NotificationUtil.showError("Failed to change password");
        }
    }

    private void handleAvatarChange() {
        final String userEmoji = this.userAvatarSelect.getValue();
        final String tutorEmoji = this.tutorAvatarSelect.getValue();

        if (userEmoji == null || userEmoji.trim().isEmpty()) {
            NotificationUtil.showError("Please select a user avatar");
            return;
        }

        if (tutorEmoji == null || tutorEmoji.trim().isEmpty()) {
            NotificationUtil.showError("Please select a tutor avatar");
            return;
        }

        try {
            this.userService.updateAvatars(this.currentUserId, userEmoji, tutorEmoji);
            NotificationUtil.showSuccess("Avatars updated successfully");
        } catch (final ValidationException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Failed to update avatars", e);
            NotificationUtil.showError("Failed to update avatars");
        }
    }
}
