package de.vptr.aimathtutor.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Login view for the application. Provides username/password fields and
 * handles user authentication via the
 * {@link de.vptr.aimathtutor.service.AuthService}.
 */
@Route(value = "login", layout = MainLayout.class)
@PageTitle("AI Math Tutor - Login")
public class LoginView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LoginView.class);

    @Inject
    private transient AuthService authService;

    /**
     * Construct the login view with username/password fields and a login
     * handler.
     */
    public LoginView() {
        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);

        final var title = new H1("AI Math Tutor - Login");

        final var usernameField = new TextField("Username");
        usernameField.setPrefixComponent(LineAwesomeIcon.USER_SOLID.create());
        usernameField.setRequired(true);
        usernameField.setWidth("300px");

        final var passwordField = new PasswordField("Password");
        passwordField.setPrefixComponent(LineAwesomeIcon.LOCK_SOLID.create());
        passwordField.setRequired(true);
        passwordField.setWidth("300px");

        final var loginButton = new Button("Login");
        loginButton.addClickListener(e -> {
            final var username = usernameField.getValue();
            final var password = passwordField.getValue();

            try {
                // Disable button during authentication
                loginButton.setEnabled(false);
                loginButton.setText("Authenticating...");

                // Perform authentication
                final var result = this.authService.authenticate(username, password);

                LOG.trace("Authentication result - Status: {}, Message: {}", result.getStatus(), result.getMessage());

                switch (result.getStatus()) {
                    case SUCCESS:
                        LOG.trace("Authentication successful, navigating to main view");
                        this.getUI().ifPresent(ui -> ui.navigate(""));
                        break;

                    case INVALID_CREDENTIALS:
                        LOG.trace("Invalid credentials, showing error message");
                        NotificationUtil.showError(result.getMessage());
                        passwordField.clear();
                        passwordField.focus();
                        break;

                    case BACKEND_UNAVAILABLE:
                        LOG.error("Backend unavailable during login, redirecting to error page");
                        this.getUI().ifPresent(ui -> ui.navigate("backend-error"));
                        break;

                    case INVALID_INPUT:
                        LOG.trace("Invalid input, showing warning");
                        NotificationUtil.showWarning(result.getMessage());
                        break;

                    default:
                        LOG.error("Unknown authentication result status: {}", result.getStatus());
                        NotificationUtil.showError("Unknown error occurred");
                        break;
                }

            } catch (final Exception ex) {
                LOG.error("Exception during authentication", ex);
                NotificationUtil.showError("Unexpected error: " + ex.getMessage());
            } finally {
                // Re-enable button
                loginButton.setEnabled(true);
                loginButton.setText("Login");
            }
        });

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickShortcut(Key.ENTER);
        loginButton.setWidth("300px");

        this.add(title, usernameField, passwordField, loginButton);

        usernameField.focus();
    }
}
