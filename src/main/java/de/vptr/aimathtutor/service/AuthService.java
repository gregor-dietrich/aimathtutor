package de.vptr.aimathtutor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
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
    private static final String LAST_DB_CHECK_KEY = "authenticated.lastDbCheck";

    /**
     * How long an {@link #isAuthenticated()} result may be served from the session
     * without re-validating against the database. Keeps {@code beforeEnter} navigation
     * checks off the DB while still picking up bans/deactivations within a short window.
     */
    private static final long AUTH_CACHE_TTL_MILLIS = 30_000L;

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
        final String clientIp = this.extractClientIp();

        // Check login attempt throttling by username
        if (this.loginAttemptService.isLockedOut(usernameKey)) {
            final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(usernameKey);
            LOG.warn("Authentication throttled for user: {} ({}s remaining)", username, remaining);
            return AuthResultDto
                    .backendUnavailable("Too many failed attempts. Try again later.");
        }

        // Check login attempt throttling by IP
        if (clientIp != null && this.loginAttemptService.isLockedOut(clientIp)) {
            final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(clientIp);
            LOG.warn("Authentication throttled for IP: {} ({}s remaining)", clientIp, remaining);
            return AuthResultDto
                    .backendUnavailable("Too many failed attempts. Try again later.");
        }

        try {
            // Find user by normalized username using repository
            final var user = this.userRepository.findByUsername(usernameKey);

            if (user == null) {
                LOG.trace("Authentication failed - user not found: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is banned
            if (user.banned) {
                LOG.trace("Authentication failed - user is banned: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is activated
            if (!user.activated) {
                LOG.trace("Authentication failed - user is not activated: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Verify password using password hashing service
            if (!this.passwordHashingService.verifyPassword(password, user.password)) {
                LOG.trace("Authentication failed - invalid password for user: {}", usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Persist the user entity if needed
            try {
                this.userRepository.persist(user);
            } catch (final PersistenceException e) {
                LOG.warn("Failed to persist user {} during login: {}", user.username, e.getMessage());
                // continue with login even if persist failed
            }

            try {
                this.loginAttemptService.recordSuccessfulLogin(usernameKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordSuccessfulLogin(clientIp);
                }
                // Regenerate session ID to defeat session-fixation attacks where an
                // attacker pre-sets the victim's session ID before login.
                final VaadinRequest request = VaadinRequest.getCurrent();
                if (request != null) {
                    VaadinService.reinitializeSession(request);
                }
                final var session = VaadinSession.getCurrent();
                if (session != null) {
                    session.setAttribute(USERNAME_KEY, user.username);
                    session.setAttribute(AUTHENTICATED_KEY, true);
                    session.setAttribute(LAST_DB_CHECK_KEY, System.currentTimeMillis());
                }
            } catch (final RuntimeException e) {
                LOG.error("Failed to complete login for user {}: {}", username, e.getMessage(), e);
                return AuthResultDto
                        .backendUnavailable("Authentication service temporarily unavailable. Please try again later.");
            }

            LOG.trace("User authenticated successfully: {}", username);
            return AuthResultDto.success();

        } catch (final PersistenceException e) {
            LOG.error("Database error during authentication for user: {}", username, e);
            return AuthResultDto
                    .backendUnavailable("Authentication service temporarily unavailable. Please try again later.");
        }
    }

    private String extractClientIp() {
        final VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            return null;
        }
        final String remoteAddr = request.getRemoteAddr();
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && this.isTrustedProxy(remoteAddr)) {
            return forwarded.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(final String remoteAddr) {
        return "127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr);
    }

    /**
     * Clears the current user's authentication session.
     * Removes stored username, password, and authentication status from the
     * session.
     */
    public void logout() {
        final var username = this.getUsername();
        LOG.trace("Logging out user: {}", username);

        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }
        session.setAttribute(USERNAME_KEY, null);
        session.setAttribute(AUTHENTICATED_KEY, false);
        session.setAttribute(LAST_DB_CHECK_KEY, null);

        // Regenerate session ID after logout so a leaked pre-logout ID cannot
        // be reused by an attacker on a future login from the same browser.
        final VaadinRequest request = VaadinRequest.getCurrent();
        if (request != null) {
            VaadinService.reinitializeSession(request);
        }

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

        // Skip the DB lookup when we re-validated within the cache window.
        // Vaadin navigation calls beforeEnter on every route change, and the
        // findByUsername hit otherwise dominates page-to-page latency.
        final var lastCheck = (Long) session.getAttribute(LAST_DB_CHECK_KEY);
        if (lastCheck != null && System.currentTimeMillis() - lastCheck < AUTH_CACHE_TTL_MILLIS) {
            return true;
        }

        final var user = this.userRepository.findByUsername(username);
        final var result = user != null && user.activated && !user.banned;
        if (result) {
            session.setAttribute(LAST_DB_CHECK_KEY, System.currentTimeMillis());
        } else {
            session.setAttribute(LAST_DB_CHECK_KEY, null);
        }
        LOG.trace("Checking authentication status (DB hit): {}", result);
        return result;
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username of the current user, or null if not authenticated
     */
    public String getUsername() {
        // VaadinSession.getCurrent() can return null outside UI request context.
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(USERNAME_KEY);
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
