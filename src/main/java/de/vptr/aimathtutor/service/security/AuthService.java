package de.vptr.aimathtutor.service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.UserAccountChangedEvent;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
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

    private static final String USER_PUBLIC_ID_KEY = AppConstants.SESSION_KEY_USER_PUBLIC_ID;
    private static final String CREDENTIAL_STAMP_KEY = AppConstants.SESSION_KEY_CREDENTIAL_STAMP;
    private static final String SESSION_TOKEN_KEY = "authenticated.sessionToken";
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
     * Maps user public ID to the timestamp of the most recent eviction request.
     */
    private final Map<String, Long> globalEvictionTimestamps = new ConcurrentHashMap<>();

    /**
     * Evicts the authentication cache for the specified user. This forces the next {@link #isAuthenticated()} call for
     * this user (in any session) to re-validate against the database, regardless of the TTL.
     *
     * @param publicId
     *            the public ID of the user to evict
     */
    public void evictCache(final String publicId) {
        if (publicId != null) {
            final long now = System.currentTimeMillis();
            this.globalEvictionTimestamps.entrySet().removeIf(e -> e.getValue() < now - AUTH_CACHE_TTL_MILLIS);
            this.globalEvictionTimestamps.put(publicId, now);
        }
    }

    /**
     * Computes the credential stamp of a stored password hash: the Base64 of its SHA-256 digest. The stamp changes
     * exactly when the password changes (bcrypt salts every hash, so even re-setting the same password yields a new
     * one), so a session holding an older stamp is revoked. The hash itself never goes into the session.
     *
     * @param passwordHash
     *            the stored bcrypt hash, or null
     * @return the credential stamp, or null if there is no hash
     */
    @Nullable
    public static String credentialStamp(@Nullable final String passwordHash) {
        if (passwordHash == null) {
            return null;
        }
        try {
            final var digest =
                    MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every Java platform implementation.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Tells whether a credential stamp is the current one of the user, comparing in constant time.
     *
     * @param user
     *            the user
     * @param stamp
     *            the stamp a session holds, or null
     * @return true if the stamp equals the stamp of the user's current password hash; false if either is null
     */
    public static boolean holdsStamp(final UserEntity user, @Nullable final String stamp) {
        final var current = credentialStamp(user.password);
        return current != null && stamp != null && MessageDigest.isEqual(current.getBytes(StandardCharsets.UTF_8),
                stamp.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reacts to a committed change of a user account: evicts the cached authentication state of the account in every
     * session, and re-stamps the acting session on any change of its own account that carries a stamp (equal to the
     * current one unless the password changed), so the session that changed its own password stays signed in. It runs
     * after the transaction commits: an eviction recorded before the commit lets a concurrent
     * {@link #isAuthenticated()} read the old state and cache it again for up to the TTL, and a rolled-back change must
     * evict nothing.
     *
     * @param event
     *            the committed account change
     */
    void onUserAccountChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) final UserAccountChangedEvent event) {
        this.evictCache(event.publicId());
        final var session = VaadinSession.getCurrent();
        // The session that changed its own account's password stays signed in; it passed the permission checks with
        // its old stamp.
        if (event.credentialStamp() != null && session != null
                && event.publicId().equals(session.getAttribute(USER_PUBLIC_ID_KEY))) {
            session.setAttribute(CREDENTIAL_STAMP_KEY, event.credentialStamp());
        }
    }

    /**
     * Gives the current session the new credential stamp after a self-service password change, which runs off the UI
     * thread where {@link #onUserAccountChanged} sees no session. It is called through {@code VaadinSession.access}
     * after {@code UserService.changePassword}. This is safe because it only installs a stamp equal to the account's
     * current one, which cannot be derived without the stored hash, and the DB check refuses a stamp that a concurrent
     * change has already replaced; otherwise nothing happens.
     *
     * @param credentialStamp
     *            the stamp returned by {@code UserService.changePassword}
     */
    @Transactional
    public void renewCredentialStamp(final String credentialStamp) {
        final var session = VaadinSession.getCurrent();
        if (session == null || !(session.getAttribute(USER_PUBLIC_ID_KEY) instanceof final String publicId)) {
            return;
        }
        this.userRepository.findByPublicId(publicId).filter(user -> holdsStamp(user, credentialStamp))
                .ifPresent(ignored -> session.setAttribute(CREDENTIAL_STAMP_KEY, credentialStamp));
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
                    session.setAttribute(USER_PUBLIC_ID_KEY, user.publicId);
                    session.setAttribute(CREDENTIAL_STAMP_KEY, credentialStamp(user.password));
                    session.setAttribute(SESSION_TOKEN_KEY, UUID.randomUUID().toString());
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

    private static void clearAuthAttributes(final VaadinSession session) {
        session.setAttribute(USER_PUBLIC_ID_KEY, null);
        session.setAttribute(CREDENTIAL_STAMP_KEY, null);
        session.setAttribute(SESSION_TOKEN_KEY, null);
        session.setAttribute(AUTHENTICATED_KEY, false);
        session.setAttribute(LAST_DB_CHECK_KEY, null);
    }

    /**
     * Clears the current user's authentication session. Removes the stored user public ID, credential stamp, and
     * authentication status from the session.
     */
    public void logout() {
        LOG.trace("Logging out user");

        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }
        clearAuthAttributes(session);

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
        final var publicId = (String) session.getAttribute(USER_PUBLIC_ID_KEY);
        if (publicId == null || publicId.isBlank()) {
            return false;
        }

        // Skip the DB lookup when we re-validated within the cache window.
        // Vaadin navigation calls beforeEnter on every route change, and the
        // user lookup otherwise dominates page-to-page latency.
        final var lastCheck = (Long) session.getAttribute(LAST_DB_CHECK_KEY);
        final long now = System.currentTimeMillis();
        this.globalEvictionTimestamps.entrySet().removeIf(e -> e.getValue() < now - AUTH_CACHE_TTL_MILLIS);
        final var globalEviction = this.globalEvictionTimestamps.get(publicId);

        if (lastCheck != null && (now - lastCheck < AUTH_CACHE_TTL_MILLIS)
                && (globalEviction == null || lastCheck > globalEviction)) {
            return true;
        }

        final var user = this.getCurrentUserEntity();
        final var result = user != null && user.activated && !user.banned;
        // Store the time taken before the read: a read that began before a commit must not outlive an eviction
        // recorded after that commit.
        if (result) {
            session.setAttribute(LAST_DB_CHECK_KEY, now);
        } else {
            // A revoked session stays revoked: a later unban or re-activation must not revive it.
            clearAuthAttributes(session);
        }
        LOG.tracef("Checking authentication status (DB hit): %s", result);
        return result;
    }

    /**
     * Captures the credentials of the current session for work that runs off the UI thread. Reads the session only, no
     * database access; the receiver verifies them in its own transaction.
     *
     * @return the session's credentials, or null if there is no session or it is not signed in
     */
    @Nullable
    public SessionCredentials currentSessionCredentials() {
        final var session = VaadinSession.getCurrent();
        if (session != null && session.getAttribute(USER_PUBLIC_ID_KEY) instanceof final String publicId
                && session.getAttribute(CREDENTIAL_STAMP_KEY) instanceof final String stamp
                && session.getAttribute(SESSION_TOKEN_KEY) instanceof final String token) {
            return new SessionCredentials(publicId, stamp, token);
        }
        return null;
    }

    /**
     * Retrieves the username of the currently authenticated user. Resolved from the database on every call, so a rename
     * shows up immediately.
     *
     * @return the current username of the user, or null if not authenticated
     */
    @Nullable
    public String getUsername() {
        final var user = this.getCurrentUserEntity();
        return user != null ? user.username : null;
    }

    /**
     * Retrieves the user ID of the currently authenticated user.
     *
     * @return the ID of the current user, or null if not authenticated or user not found
     */
    @Nullable
    public Long getUserId() {
        final var user = this.getCurrentUserEntity();
        return user != null ? user.id : null;
    }

    /**
     * Resolves the session's user: the single resolver every other lookup goes through. The user is looked up by the
     * public ID in the session and only returned if the credential stamp of their current password hash equals the
     * session's stamp, so a session that predates a password change resolves to no one. It does not check activation or
     * ban status; callers do.
     *
     * @return the current {@link UserEntity}, or null if there is no session, the user no longer exists, or the
     *         session's credentials are revoked
     */
    @Nullable
    public UserEntity getCurrentUserEntity() {
        // VaadinSession.getCurrent() can return null outside UI request context.
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        final var publicId = (String) session.getAttribute(USER_PUBLIC_ID_KEY);
        if (publicId == null) {
            return null;
        }
        final var stamp = (String) session.getAttribute(CREDENTIAL_STAMP_KEY);
        return this.userRepository.findByPublicId(publicId).filter(user -> holdsStamp(user, stamp)).orElse(null);
    }
}
