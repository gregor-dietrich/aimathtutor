package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankService userRankService;

    private static final String USERNAME_KEY = "authenticated.username";
    private static final String PASSWORD_KEY = "authenticated.password";
    private static final String AUTHENTICATED_KEY = "authenticated.status";

    @Transactional
    public AuthResultDto authenticate(final String username, final String password) {
        LOG.trace("Starting authentication for user: {}", username);

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            LOG.trace("Username or password is empty");
            return AuthResultDto.invalidInput();
        }

        try {
            // Find user by username using repository
            final var user = this.userRepository.findByUsername(username);

            if (user == null) {
                LOG.trace("Authentication failed - user not found: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is banned
            if (user.banned != null && user.banned) {
                LOG.trace("Authentication failed - user is banned: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is activated
            if (user.activated == null || !user.activated) {
                LOG.trace("Authentication failed - user is not activated: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            // Verify password using password hashing service
            if (!this.passwordHashingService.verifyPassword(password, user.password, user.salt)) {
                LOG.trace("Authentication failed - invalid password for user: {}", username);
                return AuthResultDto.invalidCredentials();
            }

            // Update last login time and persist the user entity
            try {
                user.lastLogin = LocalDateTime.now();
                this.userRepository.persist(user);
            } catch (final Exception e) {
                LOG.warn("Failed to update lastLogin for user {}: {}", username, e.getMessage());
                // continue with login even if lastLogin couldn't be updated
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

    public Long getUserId() {
        final String username = this.getUsername();
        if (username == null) {
            return null;
        }
        final var user = this.userRepository.findByUsername(username);
        return user != null ? user.id : null;
    }

    /**
     * Get the current authenticated user entity (for accessing avatar settings,
     * etc.)
     * 
     * @return UserEntity or null if not authenticated
     */
    public UserEntity getCurrentUserEntity() {
        final String username = this.getUsername();
        if (username == null) {
            return null;
        }
        return this.userRepository.findByUsername(username);
    }
}
