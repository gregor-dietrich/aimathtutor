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

/**
 * Authentication helper service offering login/logout and current user info.
 */
@ApplicationScoped
public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankService userRankService;

    @Inject
    LoginAttemptService loginAttemptService;

    private static final String USERNAME_KEY = "authenticated.username";
    private static final String AUTHENTICATED_KEY = "authenticated.status";

    /**
     * Authenticates a user with the provided credentials.
     * Validates username and password, checks user activation and ban status,
     * updates last login time, and stores authentication information in the
     * session.
     *
     * @param username the username to authenticate
     * @param password the plaintext password to verify
     * @return an {@link AuthResultDto} indicating success or the reason for failure
     */
    @Transactional
    public AuthResultDto authenticate(final String username, final String password) {
        LOG.trace("Starting authentication for user: {}", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOG.trace("Username or password is empty");
            return AuthResultDto.invalidInput();
        }

        final String usernameKey = username.toLowerCase().trim();

        // Check login attempt throttling
        if (this.loginAttemptService.isLockedOut(usernameKey)) {
            final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(usernameKey);
            LOG.warn("Authentication throttled for user: {} ({}s remaining)", username, remaining);
            return AuthResultDto.backendUnavailable("Too many failed attempts. Please try again in " + remaining + " seconds.");
        }

        try {
            // Find user by normalized username using repository
            final var user = this.userRepository.findByUsername(usernameKey);

            if (user == null) {
                LOG.trace("Authentication failed - user not found: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is banned
            if (user.banned != null && user.banned) {
                LOG.trace("Authentication failed - user is banned: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is activated
            if (user.activated == null || !user.activated) {
                LOG.trace("Authentication failed - user is not activated: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                return AuthResultDto.invalidCredentials();
            }

            // Verify password using password hashing service
            if (!this.passwordHashingService.verifyPassword(password, user.password, user.salt)) {
                LOG.trace("Authentication failed - invalid password for user: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                return AuthResultDto.invalidCredentials();
            }

            // Update last login time and persist the user entity
            try {
                user.lastLogin = LocalDateTime.now();
                this.userRepository.persist(user);
            } catch (final Exception e) {
                LOG.warn("Failed to update lastLogin for user {}: {}", user.username, e.getMessage());
                // continue with login even if lastLogin couldn't be updated
            }

            this.loginAttemptService.recordSuccessfulLogin(usernameKey);
            VaadinSession.getCurrent().setAttribute(USERNAME_KEY, user.username);
            VaadinSession.getCurrent().setAttribute(AUTHENTICATED_KEY, true);

            LOG.trace("User authenticated successfully: {}", username);
            return AuthResultDto.success();

        } catch (final Exception e) {
            LOG.error("Unexpected error during authentication for user: {} - Error: {}", username, e.getMessage(), e);
            return AuthResultDto.backendUnavailable("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Clears the current user's authentication session.
     * Removes stored username, password, and authentication status from the
     * session.
     */
    public void logout() {
        final var username = this.getUsername();
        LOG.trace("Logging out user: {}", username);

        VaadinSession.getCurrent().setAttribute(USERNAME_KEY, null);
        VaadinSession.getCurrent().setAttribute(AUTHENTICATED_KEY, false);

        LOG.trace("User logged out");
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if the user has an active authenticated session, false otherwise
     */
    public boolean isAuthenticated() {
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return false;
        }

        final var authenticated = (Boolean) session.getAttribute(AUTHENTICATED_KEY);
        if (authenticated == null || !authenticated) {
            return false;
        }

        // Verify the user still exists and is active to prevent stale session bypass
        final var username = (String) session.getAttribute(USERNAME_KEY);
        if (username == null || username.isBlank()) {
            return false;
        }

        final var user = this.userRepository.findByUsername(username);
        final var result = user != null && Boolean.TRUE.equals(user.activated) && !Boolean.TRUE.equals(user.banned);
        LOG.trace("Checking authentication status: {}", result);
        return result;
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username of the current user, or null if not authenticated
     */
    public String getUsername() {
        return (String) VaadinSession.getCurrent().getAttribute(USERNAME_KEY);
    }

    /**
     * Retrieves the user ID of the currently authenticated user.
     *
     * @return the ID of the current user, or null if not authenticated or user not
     *         found
     */
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
