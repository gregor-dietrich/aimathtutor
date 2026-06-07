package de.vptr.aimathtutor.service.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

/**
 * Authentication helper service offering login/logout and current user info.
 */
@ApplicationScoped
public class AuthService {
    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankService userRankService;

    @Inject
    LoginAttemptService loginAttemptService;

    @ConfigProperty(name = "app.security.trusted-proxy-ips", defaultValue = "127.0.0.1,::1,0:0:0:0:0:0:0:1")
    @Nullable
    String trustedProxyIpsConfig;

    private Set<String> trustedProxyIps;

    @PostConstruct
    void init() {
        this.trustedProxyIps = trustedProxyIpsConfig != null
                ? Arrays.stream(trustedProxyIpsConfig.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet())
                : Set.of(AppConstants.BLOCKED_HOST_LOOPBACK_IPV4, AppConstants.BLOCKED_HOST_LOOPBACK_IPV6,
                        AppConstants.BLOCKED_HOST_LOOPBACK_IPV6_EXPANDED);
    }

    private static final String USERNAME_KEY = AppConstants.SESSION_KEY_USERNAME;
    private static final String AUTHENTICATED_KEY = "authenticated.status";
    private static final String LAST_DB_CHECK_KEY = "authenticated.lastDbCheck";

    // Pre-computed bcrypt hash used for constant-time dummy verification on early-exit
    // authentication paths (user not found, banned, not activated). Prevents username
    // enumeration via timing side-channel by ensuring every authenticate() call pays
    // approximately one bcrypt cost regardless of whether the user exists.
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36zLdGaohNuXiUeKMTfXrvy";

    /**
     * How long an {@link #isAuthenticated()} result may be served from the session without re-validating against the
     * database. Keeps {@code beforeEnter} navigation checks off the DB while still picking up bans/deactivations within
     * a short window.
     */
    private static final long AUTH_CACHE_TTL_MILLIS = 30_000L;

    /**
     * Global eviction map to handle immediate revocation (bans/deactivations) across all sessions for a specific user.
     * Maps username to the timestamp of the most recent eviction request.
     */
    private final Map<String, Long> globalEvictionTimestamps = new ConcurrentHashMap<>();

    /**
     * Evicts the authentication cache for the specified user. This forces the next {@link #isAuthenticated()} call for
     * this user (in any session) to re-validate against the database, regardless of the TTL.
     *
     * @param username
     *            the username to evict
     */
    public void evictCache(final String username) {
        if (username != null) {
            final long now = System.currentTimeMillis();
            this.globalEvictionTimestamps.entrySet().removeIf(e -> e.getValue() < now - AUTH_CACHE_TTL_MILLIS);
            this.globalEvictionTimestamps.put(username.toLowerCase(Locale.ROOT).trim(), now);
        }
    }

    /**
     * Authenticates a user with the provided credentials. Validates username and password, checks user activation and
     * ban status, and stores authentication information in the session.
     *
     * @param username
     *            the username to authenticate
     * @param password
     *            the plaintext password to verify
     * @return an {@link AuthResultDto} indicating success or the reason for failure
     */
    @Transactional
    public AuthResultDto authenticate(final String username, final String password) {
        LOG.trace("Starting authentication");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            LOG.trace("Username or password is empty");
            return AuthResultDto.invalidInput();
        }

        final String usernameKey = username.toLowerCase(Locale.ROOT).trim();
        final String clientIp = this.extractClientIp();
        final String usernameIpKey = usernameKey + ":" + (clientIp != null ? clientIp : "unknown");

        // Check login attempt throttling by account (username only)
        if (this.loginAttemptService.isAccountLockedOut(usernameKey)) {
            final long remaining = this.loginAttemptService.getRemainingAccountLockoutSeconds(usernameKey);
            LOG.warnf("Authentication throttled by account (%ss remaining)", remaining);
            return AuthResultDto.backendUnavailable("Too many failed attempts. Try again later.");
        }

        // Check login attempt throttling by username + IP to prevent global lockout of a user by an attacker
        if (this.loginAttemptService.isLockedOut(usernameIpKey)) {
            final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(usernameIpKey);
            LOG.warnf("Authentication throttled by username:ip (%ss remaining)", remaining);
            return AuthResultDto.backendUnavailable("Too many failed attempts. Try again later.");
        }

        // Check login attempt throttling by IP
        if (clientIp != null && this.loginAttemptService.isLockedOut(clientIp)) {
            final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(clientIp);
            LOG.warnf("Authentication throttled by IP (%ss remaining)", remaining);
            return AuthResultDto.backendUnavailable("Too many failed attempts. Try again later.");
        }

        try {
            // Find user by normalized username using repository
            final var user = this.userRepository.findByUsername(usernameKey);

            if (user == null) {
                LOG.trace("Authentication failed - user not found");
                // Dummy bcrypt call to normalise timing and prevent username enumeration
                this.passwordHashingService.verifyPassword(password, DUMMY_BCRYPT_HASH);
                this.loginAttemptService.recordFailedAccountAttempt(usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameIpKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is banned
            if (user.banned) {
                LOG.trace("Authentication failed - user is banned");
                this.passwordHashingService.verifyPassword(password, DUMMY_BCRYPT_HASH);
                this.loginAttemptService.recordFailedAccountAttempt(usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameIpKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Check if user is activated
            if (!user.activated) {
                LOG.trace("Authentication failed - user is not activated");
                this.passwordHashingService.verifyPassword(password, DUMMY_BCRYPT_HASH);
                this.loginAttemptService.recordFailedAccountAttempt(usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameIpKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            // Verify password using password hashing service
            if (!this.passwordHashingService.verifyPassword(password, user.password)) {
                LOG.trace("Authentication failed - invalid password");
                this.loginAttemptService.recordFailedAccountAttempt(usernameKey);
                this.loginAttemptService.recordFailedAttempt(usernameIpKey);
                if (clientIp != null) {
                    this.loginAttemptService.recordFailedAttempt(clientIp);
                }
                return AuthResultDto.invalidCredentials();
            }

            try {
                this.loginAttemptService.recordSuccessfulAccountLogin(usernameKey);
                this.loginAttemptService.recordSuccessfulLogin(usernameIpKey);
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
                LOG.errorf(e, "Failed to complete login: %s", e.getMessage());
                return AuthResultDto
                        .backendUnavailable("Authentication service temporarily unavailable. Please try again later.");
            }

            LOG.trace("User authenticated successfully");
            return AuthResultDto.success();

        } catch (final PersistenceException e) {
            LOG.errorf(e, "Database error during authentication");
            return AuthResultDto
                    .backendUnavailable("Authentication service temporarily unavailable. Please try again later.");
        }
    }

    @Nullable
    private String extractClientIp() {
        final VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            return null;
        }
        final String remoteAddr = request.getRemoteAddr();
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && this.isTrustedProxy(remoteAddr)) {
            final int commaIdx = forwarded.indexOf(',');
            return (commaIdx >= 0 ? forwarded.substring(0, commaIdx) : forwarded).trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(final String remoteAddr) {
        return this.trustedProxyIps.contains(remoteAddr);
    }

    /**
     * Clears the current user's authentication session. Removes stored username, password, and authentication status
     * from the session.
     */
    public void logout() {
        final var username = this.getUsername();
        LOG.tracef("Logging out user: %s", username);

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
        final long now = System.currentTimeMillis();
        this.globalEvictionTimestamps.entrySet().removeIf(e -> e.getValue() < now - AUTH_CACHE_TTL_MILLIS);
        final var globalEviction = this.globalEvictionTimestamps.get(username.toLowerCase(Locale.ROOT).trim());

        if (lastCheck != null && (now - lastCheck < AUTH_CACHE_TTL_MILLIS)
                && (globalEviction == null || lastCheck > globalEviction)) {
            return true;
        }

        final var user = this.userRepository.findByUsername(username);
        final var result = user != null && user.activated && !user.banned;
        if (result) {
            session.setAttribute(LAST_DB_CHECK_KEY, System.currentTimeMillis());
        } else {
            session.setAttribute(LAST_DB_CHECK_KEY, null);
        }
        LOG.tracef("Checking authentication status (DB hit): %s", result);
        return result;
    }

    /**
     * Retrieves the username of the currently authenticated user.
     *
     * @return the username of the current user, or null if not authenticated
     */
    @Nullable
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
     * @return the ID of the current user, or null if not authenticated or user not found
     */
    @Nullable
    public Long getUserId() {
        final String username = this.getUsername();
        if (username == null) {
            return null;
        }
        final var user = this.userRepository.findByUsername(username);
        return user != null ? user.id : null;
    }

    /**
     * Get the current authenticated user entity (for accessing avatar settings, etc.)
     * 
     * @return UserEntity or null if not authenticated
     */
    @Nullable
    public UserEntity getCurrentUserEntity() {
        final String username = this.getUsername();
        if (username == null) {
            return null;
        }
        return this.userRepository.findByUsername(username);
    }
}
