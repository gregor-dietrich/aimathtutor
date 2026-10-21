package de.vptr.aimathtutor.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.Appearance.ColorMode;
import de.vptr.aimathtutor.dto.Appearance.Design;
import de.vptr.aimathtutor.dto.UserSettingsDto;
import de.vptr.aimathtutor.service.ThemeService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AppStyles;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * User settings view for choosing the appearance (design and colour mode), changing the password and customizing chat
 * avatars.
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH",
        justification = "Vaadin lifecycle: fields are built before these methods run")
public class UserSettingsView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private transient AuthService authService;

    @Inject
    private transient UserService userService;

    @Inject
    private transient ThemeService themeService;

    @Nullable
    private PasswordField currentPasswordField;
    @Nullable
    private PasswordField newPasswordField;
    @Nullable
    private PasswordField confirmPasswordField;

    @Nullable
    private RadioButtonGroup<Design> designGroup;
    @Nullable
    private RadioButtonGroup<ColorMode> colorModeGroup;

    @Nullable
    private ComboBox<String> userAvatarSelect;
    @Nullable
    private ComboBox<String> tutorAvatarSelect;
    @Nullable
    private Div previewBox;

    @Nullable
    private String currentUsername;
    @Nullable
    private String currentEmail;

    /** The appearance the section was built from; a save writes only the fields that differ from it. */
    @Nullable
    private Appearance initialAppearance;

    private boolean passwordChangeInProgress;
    private boolean avatarUpdateInProgress;
    private boolean appearanceSaveInProgress;

    /**
     * Called before navigation occurs. Checks authentication, loads current user information, and builds the settings
     * UI. Redirects to login if not authenticated.
     *
     * @param event
     *            the before enter navigation event
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        // Check authentication
        if (!this.authService.isAuthenticated()) {
            event.rerouteTo(LoginView.class);
            return;
        }

        // Get current user info
        if (this.authService.getCurrentUserEntity() == null) {
            NotificationUtil.showError("Could not load user information");
            event.rerouteTo(LessonsView.class);
            return;
        }

        final var user = this.userService.getCurrentUser();
        this.currentUsername = user.username;
        this.currentEmail = user.email;

        this.buildUi();
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        super.onAttach(event);
        this.loadCurrentSettings();
    }

    private void buildUi() {
        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);
        this.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        final var content = new VerticalLayout();
        content.setWidth("800px");
        content.setSpacing(true);
        content.setPadding(false);

        final var title = new H2("Settings");

        // Appearance Section
        final var appearanceSection = this.createAppearanceSection();

        // User Info Section (read-only)
        final var userInfoSection = this.createUserInfoSection();

        // Password Section
        final var passwordSection = this.createPasswordSection();

        // Avatar Section
        final var avatarSection = this.createAvatarSection();

        content.add(title, appearanceSection, userInfoSection, passwordSection, avatarSection);
        this.add(content);
    }

    private static VerticalLayout createSection() {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle().set("background-color", "var(--vaadin-background-container)").set("border-radius",
                "var(--vaadin-radius-m)");
        return section;
    }

    private VerticalLayout createAppearanceSection() {
        final var section = createSection();

        final var header = new H3("Appearance");
        header.getStyle().set("margin-top", "0");

        // Read from the session, so it does not wait for the async settings load.
        final var appearance = this.themeService.current();
        this.initialAppearance = appearance;

        this.designGroup = new RadioButtonGroup<>("Design");
        this.designGroup.setItems(Design.values());
        this.designGroup.setItemLabelGenerator(Design::getDisplayName);
        this.designGroup.setValue(appearance.design());

        this.colorModeGroup = new RadioButtonGroup<>("Colour mode");
        this.colorModeGroup.setItems(ColorMode.values());
        this.colorModeGroup.setItemLabelGenerator(ColorMode::getDisplayName);
        this.colorModeGroup.setValue(appearance.colorMode());

        final var saveButton = new Button("Save Appearance");
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        saveButton.addClickListener(ignored -> this.handleAppearanceChange(saveButton));

        section.add(header, this.designGroup, this.colorModeGroup, saveButton);
        return section;
    }

    private VerticalLayout createUserInfoSection() {
        final var section = createSection();

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
        notice.getStyle().set("font-style", "italic").set("color", "var(--vaadin-text-color-secondary)");

        section.add(header, usernameInfo, emailInfo, notice);
        return section;
    }

    private VerticalLayout createPasswordSection() {
        final var section = createSection();

        final var header = new H3("Change Password");
        header.getStyle().set("margin-top", "0");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        this.currentPasswordField = new PasswordField("Current Password");
        this.currentPasswordField.setRequired(true);
        this.currentPasswordField.setWidthFull();

        this.newPasswordField = new PasswordField("New Password");
        this.newPasswordField.setRequired(true);
        this.newPasswordField.setHelperText("Between " + AppConstants.PASSWORD_MIN_LENGTH + " and "
                + AppConstants.PASSWORD_MAX_LENGTH + " characters with uppercase, lowercase, digit and symbol");
        this.newPasswordField.setWidthFull();

        this.confirmPasswordField = new PasswordField("Confirm New Password");
        this.confirmPasswordField.setRequired(true);
        this.confirmPasswordField.setWidthFull();

        form.add(this.currentPasswordField, this.newPasswordField, this.confirmPasswordField);

        final var changePasswordButton = new Button("Change Password");
        changePasswordButton.addThemeVariants(ButtonVariant.PRIMARY);
        changePasswordButton.addClickListener(ignored -> this.handlePasswordChange(changePasswordButton));

        section.add(header, form, changePasswordButton);
        return section;
    }

    private VerticalLayout createAvatarSection() {
        final var section = createSection();

        final var header = new H3("Chat Avatars");
        header.getStyle().set("margin-top", "0");

        final var description =
                new Paragraph("Select emojis to represent yourself and the AI tutor in chat conversations.");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        this.userAvatarSelect = new ComboBox<>("Your Avatar");
        this.userAvatarSelect.setItems("🧒", "👦", "👧", "🧑‍🎓", "👨‍🎓", "👩‍🎓", "🐱", "🐶", "🐭", "👽");
        this.userAvatarSelect.addValueChangeListener(ignored -> this.updatePreview());

        this.tutorAvatarSelect = new ComboBox<>("AI Tutor Avatar");
        this.tutorAvatarSelect.setItems("🤖", "🦉", "🖥️", "💻", "🧑‍🏫", "👨‍🏫", "👩‍🏫", "🧑‍💻", "👨‍💻", "👩‍💻");
        this.tutorAvatarSelect.addValueChangeListener(ignored -> this.updatePreview());

        form.add(this.userAvatarSelect, this.tutorAvatarSelect);

        // Preview box
        this.previewBox = new Div();
        this.previewBox.setWidthFull();
        this.previewBox.getStyle().set("box-sizing", "border-box").set("padding", "var(--vaadin-padding-m)")
                .set("background-color", "var(--vaadin-background-container-strong)")
                .set("border-radius", "var(--vaadin-radius-m)").set("border", "1px solid var(--vaadin-border-color)");

        final var previewLabel = new Span("Preview:");
        previewLabel.getStyle().set("font-weight", "bold");

        final var saveAvatarsButton = new Button("Save Avatars");
        saveAvatarsButton.addThemeVariants(ButtonVariant.PRIMARY);
        saveAvatarsButton.addClickListener(ignored -> this.handleAvatarChange());

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
        userMessage.getStyle().set("margin-bottom", "var(--vaadin-gap-s)");

        final var userBubble = new Div();
        userBubble.setText(userEmoji + " Hello, can you help me?");
        userBubble.addClassNames(AppStyles.CHAT_BUBBLE, AppStyles.CHAT_BUBBLE_USER);
        userMessage.add(userBubble);

        // AI message example
        final var aiMessage = new HorizontalLayout();
        aiMessage.setWidthFull();
        aiMessage.setJustifyContentMode(JustifyContentMode.START);

        final var aiBubble = new Div();
        aiBubble.setText(tutorEmoji + " Of course! I'm here to help.");
        aiBubble.addClassNames(AppStyles.CHAT_BUBBLE, AppStyles.CHAT_BUBBLE_TUTOR);
        aiMessage.add(aiBubble);

        this.previewBox.add(userMessage, aiMessage);
    }

    private void loadCurrentSettings() {
        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        AsyncDataLoader.load(() -> this.userService.getSettings(credentials), this, this::applySettings,
                "Failed to load settings");
    }

    private void applySettings(final UserSettingsDto settings) {
        this.userAvatarSelect.setValue(settings.userAvatarEmoji);
        this.tutorAvatarSelect.setValue(settings.tutorAvatarEmoji);
        this.updatePreview();
    }

    private void handlePasswordChange(final Button changePasswordButton) {
        final String currentPassword = this.currentPasswordField.getValue();
        final String newPassword = this.newPasswordField.getValue();
        final String confirmPassword = this.confirmPasswordField.getValue();

        // Validation
        if (currentPassword == null || currentPassword.isBlank()) {
            NotificationUtil.showError("Please enter your current password");
            return;
        }

        if (newPassword == null || newPassword.isBlank()) {
            NotificationUtil.showError("Please enter a new password");
            return;
        }

        if (newPassword.length() < AppConstants.PASSWORD_MIN_LENGTH) {
            NotificationUtil.showError(
                    "New password must be at least " + AppConstants.PASSWORD_MIN_LENGTH + " characters long");
            return;
        }

        if (newPassword.length() > AppConstants.PASSWORD_MAX_LENGTH) {
            NotificationUtil
                    .showError("New password must not exceed " + AppConstants.PASSWORD_MAX_LENGTH + " characters");
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

        if (this.passwordChangeInProgress) {
            return;
        }
        // Captured here on the UI thread: the supplier runs off it, and the stamp renewal must not depend on this view
        // still being attached when it finishes.
        final var credentials = this.authService.currentSessionCredentials();
        final var session = VaadinSession.getCurrent();
        if (credentials == null || session == null) {
            NotificationUtil.showError(AppConstants.SESSION_ENDED_MESSAGE);
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            return;
        }
        this.setPasswordControlsEnabled(changePasswordButton, false);

        AsyncDataLoader.load(() -> {
            final var stamp = this.userService.changePassword(credentials, currentPassword, newPassword);
            this.authService.renewCredentialStamp(session, credentials.userPublicId(), stamp);
            return Boolean.TRUE;
        }, this, ignored -> {
            this.setPasswordControlsEnabled(changePasswordButton, true);
            this.currentPasswordField.clear();
            this.newPasswordField.clear();
            this.confirmPasswordField.clear();
            NotificationUtil.showSuccess("Password changed successfully");
        }, () -> this.setPasswordControlsEnabled(changePasswordButton, true), "Failed to change password");
    }

    private void setPasswordControlsEnabled(final Button changePasswordButton, final boolean enabled) {
        this.passwordChangeInProgress = !enabled;
        changePasswordButton.setEnabled(enabled);
        this.currentPasswordField.setEnabled(enabled);
        this.newPasswordField.setEnabled(enabled);
        this.confirmPasswordField.setEnabled(enabled);
    }

    private void handleAvatarChange() {
        final String userEmoji = this.userAvatarSelect.getValue();
        final String tutorEmoji = this.tutorAvatarSelect.getValue();

        if (userEmoji == null || userEmoji.isBlank()) {
            NotificationUtil.showError("Please select a user avatar");
            return;
        }

        if (tutorEmoji == null || tutorEmoji.isBlank()) {
            NotificationUtil.showError("Please select a tutor avatar");
            return;
        }

        if (this.avatarUpdateInProgress) {
            return;
        }
        this.avatarUpdateInProgress = true;
        this.userAvatarSelect.setEnabled(false);
        this.tutorAvatarSelect.setEnabled(false);

        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        AsyncDataLoader.load(() -> {
            this.userService.updateAvatars(credentials, userEmoji, tutorEmoji);
            return null;
        }, this, ignored -> {
            NotificationUtil.showSuccess("Avatars updated successfully");
            this.avatarUpdateInProgress = false;
            this.userAvatarSelect.setEnabled(true);
            this.tutorAvatarSelect.setEnabled(true);
        }, () -> {
            this.avatarUpdateInProgress = false;
            this.userAvatarSelect.setEnabled(true);
            this.tutorAvatarSelect.setEnabled(true);
        }, "Failed to update avatars");
    }

    private void handleAppearanceChange(final Button saveButton) {
        if (this.appearanceSaveInProgress) {
            return;
        }
        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        final var session = VaadinSession.getCurrent();
        final var ui = this.getUI().orElse(null);
        if (credentials == null || session == null || ui == null) {
            NotificationUtil.showError(AppConstants.SESSION_ENDED_MESSAGE);
            this.getUI().ifPresent(currentUi -> currentUi.navigate(LoginView.class));
            return;
        }
        final var initial = this.initialAppearance;
        final var designValue = this.designGroup.getValue();
        final var colorModeValue = this.colorModeGroup.getValue();
        // Only what changed is written, so a mode set by the toggle meanwhile is not overwritten by a stale radio.
        final Design design = designValue == initial.design() ? null : designValue;
        final ColorMode colorMode = colorModeValue == initial.colorMode() ? null : colorModeValue;
        this.setAppearanceControlsEnabled(saveButton, false);

        // Every save reloads: the top bar's colour-mode toggle reads the mode only when it is built.
        AsyncDataLoader.load(() -> {
            this.themeService.saveForSession(session, credentials, design, colorMode);
            return Boolean.TRUE;
        }, this, ignored -> ui.getPage().reload(), () -> this.setAppearanceControlsEnabled(saveButton, true),
                "Could not save the appearance");
    }

    private void setAppearanceControlsEnabled(final Button saveButton, final boolean enabled) {
        this.appearanceSaveInProgress = !enabled;
        saveButton.setEnabled(enabled);
        this.designGroup.setEnabled(enabled);
        this.colorModeGroup.setEnabled(enabled);
    }
}
