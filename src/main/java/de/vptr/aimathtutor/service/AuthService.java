package de.vptr.aimathtutor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    UserRankService userRankService;

    private static final String USERNAME_KEY = "authenticated.username";
    private static final String PASSWORD_KEY = "authenticated.password";
    private static final String AUTHENTICATED_KEY = "authenticated.status";

    public AuthResultDto authenticate(final String username, final String password) {
        LOG.trace("Starting authentication for user: {}", username);

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            LOG.trace("Username or password is empty");
            return AuthResultDto.invalidInput();
        }

        try {
            // Find user by username directly from database
            final var user = UserEntity.<UserEntity>find("username = ?1", username).firstResult();

            if (user == null) {
                LOG.trace("Authentication failed - user not found: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            // Verify password using password hashing service
            if (!this.passwordHashingService.verifyPassword(password, user.password, user.salt)) {
                LOG.trace("Authentication failed - invalid password for user: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            VaadinSession.getCurrent().setAttribute(USERNAME_KEY, username);
            VaadinSession.getCurrent().setAttribute(PASSWORD_KEY, password);
            VaadinSession.getCurrent().setAttribute(AUTHENTICATED_KEY, true);

            LOG.trace("User authenticated successfully: {}", username);
            return AuthResultDto.success();

        } catch (final Exception e) {
            LOG.error("Unexpected error during authentication for user: {} - Error: {}", username, e.getMessage(), e);
            return AuthResultDto.backendUnavailable("Unexpected error: " + e.getMessage());
        }
    }

    public void logout() {
        final var username = this.getUsername();
        LOG.trace("Logging out user: {}", username);

        VaadinSession.getCurrent().setAttribute(USERNAME_KEY, null);
        VaadinSession.getCurrent().setAttribute(PASSWORD_KEY, null);
        VaadinSession.getCurrent().setAttribute(AUTHENTICATED_KEY, false);

        LOG.trace("User logged out");
    }

    public boolean isAuthenticated() {
        final var authenticated = (Boolean) VaadinSession.getCurrent().getAttribute(AUTHENTICATED_KEY);
        final var result = authenticated != null && authenticated;
        LOG.trace("Checking authentication status: {}", result);
        return result;
    }

    public String getUsername() {
        return (String) VaadinSession.getCurrent().getAttribute(USERNAME_KEY);
    }
}