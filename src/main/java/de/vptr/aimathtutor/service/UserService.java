package de.vptr.aimathtutor.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserSettingsDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.event.UserAccountChangedEvent;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.LoginAttemptService;
import de.vptr.aimathtutor.service.security.PasswordHashingService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing user accounts and authentication. Provides CRUD operations with password hashing, email
 * normalization, and rank assignment. Handles username/email uniqueness validation and password verification.
 */
@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    /** The Admin rank seeded by V1. */
    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    /** The admin account seeded by releases before 5.0.0. Every seeded account's published password is its username. */
    private static final String SEEDED_ADMIN_USERNAME = "admin";

    /** The demo accounts seeded by releases before 5.0.0. */
    private static final List<String> SEEDED_DEMO_USERNAMES = List.of("teacher", "student1", "student2");

    private static final Pattern USERNAME_PATTERN = Pattern.compile(AppConstants.USERNAME_PATTERN);

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRankService userRankService;

    @Inject
    PermissionService permissionService;

    @Inject
    AuthService authService;

    @Inject
    Validator validator;

    @Inject
    LoginAttemptService loginAttemptService;

    @Inject
    Event<UserAccountChangedEvent> accountChangedEvent;

    /**
     * Retrieves all users in the system.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @return a list of all {@link UserViewDto}s
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<UserViewDto> getAllUsers(@Nullable final SessionCredentials credentials) {
        this.permissionService.requireAdminView(credentials);
        return this.userRepository.findAll().stream().map(UserViewDto::new).toList();
    }

    /**
     * Finds a user by username.
     *
     * @param username
     *            the username to search for
     * @return an {@link Optional} containing the {@link UserViewDto}, or empty if not found
     */
    private Optional<UserViewDto> findByUsername(final String username) {
        return this.userRepository.findByUsernameOptional(username).map(UserViewDto::new);
    }

    /**
     * Finds a user by email address.
     *
     * @param email
     *            the email address to search for
     * @return an {@link Optional} containing the {@link UserViewDto}, or empty if not found
     */
    private Optional<UserViewDto> findByEmail(final String email) {
        return this.userRepository.findByEmailOptional(email).map(UserViewDto::new);
    }

    /**
     * Validates password strength: minimum length, and complexity (uppercase, lowercase, digit, symbol).
     */
    private void validatePassword(final String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < AppConstants.PASSWORD_MIN_LENGTH
                || password.length() > AppConstants.PASSWORD_MAX_LENGTH) {
            throw new ValidationException("Password must be between " + AppConstants.PASSWORD_MIN_LENGTH + " and "
                    + AppConstants.PASSWORD_MAX_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > AppConstants.PASSWORD_MAX_LENGTH) {
            throw new ValidationException("Password must not exceed " + AppConstants.PASSWORD_MAX_LENGTH
                    + " bytes when encoded as UTF-8 (some Unicode characters use multiple bytes)");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$")) {
            throw new ValidationException("Password must contain at least one uppercase letter, one lowercase letter, "
                    + "one digit, and one special character");
        }
    }

    /**
     * Normalizes email field by converting empty/blank strings to null. This ensures that only null or valid email
     * addresses are stored in the database, preventing unique constraint violations from multiple empty strings.
     */
    @Nullable
    private String normalizeEmail(@Nullable final String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    /**
     * Normalizes a username to the canonical form stored in the database: lower-cased. A name
     * {@link AppConstants#USERNAME_PATTERN} refuses is rejected here as well as in the DTO.
     * {@code AuthService.authenticate} lower-cases the login name before its exact-match lookup, so usernames must be
     * persisted in lower case or the account can never authenticate.
     */
    private String normalizeUsername(final String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(AppConstants.USERNAME_PATTERN_MESSAGE);
        }
        return username.toLowerCase(Locale.ROOT);
    }

    /**
     * Creates a new user account with provided information. Validates required fields (username, password), checks for
     * duplicate username/email, hashes password with bcrypt, and requires an existing rank. Without the admin view
     * permission the caller cannot activate or ban: {@code activated} and {@code banned} in the DTO are ignored and the
     * new user is neither. The rank is not gated by {@code userRankEdit}; it stays required and bounded by the caller's
     * own permissions.
     *
     * @param userDto
     *            the user data transfer object with creation details
     * @return the created {@link UserViewDto}
     * @throws ValidationException
     *             if username/email is duplicate, required fields are missing, or the rank grants a permission the
     *             caller's rank lacks
     * @throws WebApplicationException
     *             if password hashing fails
     */
    @Transactional
    public UserViewDto createUser(final @Valid UserDto userDto) {
        this.permissionService.requireUserAdd();
        return this.insertUser(userDto, this.userRankService.requireCallerRank());
    }

    /**
     * Tells whether any user account exists.
     *
     * @return true if at least one user exists
     */
    @Transactional
    public boolean hasUsers() {
        return this.userRepository.countAll() > 0;
    }

    /**
     * Tells whether an activated, unbanned user holds every permission.
     *
     * @return true if at least one active administrator exists
     */
    @Transactional
    public boolean hasActiveAdministrator() {
        return this.userRepository.countActiveAdministrators() > 0;
    }

    /**
     * Creates the initial admin account, but only while no user exists. It needs no permission check because there is
     * nobody yet who could hold one; once any account exists, it does nothing.
     *
     * @param username
     *            the admin username
     * @param password
     *            the admin password; must satisfy the regular password rules
     * @return true if the admin was created, false if users already exist
     * @throws ValidationException
     *             if the username or password is invalid
     */
    @Transactional
    public boolean createInitialAdmin(final String username, final String password) {
        if (this.hasUsers()) {
            return false;
        }
        final var admin = new UserDto(username, password, null, ADMIN_RANK_PUBLIC_ID, false, true);
        final var violations = this.validator.validate(admin);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        // A startup action has no caller whose rank could cap the new account, so no ceiling applies
        this.insertUser(admin, null);
        return true;
    }

    /**
     * Tells whether the admin account seeded by releases before 5.0.0 still accepts its published password.
     *
     * @return true if the seeded admin exists and its password is still {@code admin}
     */
    @Transactional
    public boolean hasSeededAdminPassword() {
        return this.findWithPublishedPassword(SEEDED_ADMIN_USERNAME).isPresent();
    }

    /**
     * Replaces the published password of the admin account seeded by releases before 5.0.0. It needs no permission
     * check because the replaced password is public; accounts with any other password are left alone.
     *
     * @param password
     *            the new password; must satisfy the regular password rules
     * @return true if the password was replaced, false if the seeded admin is gone or already has another password
     * @throws ValidationException
     *             if the password is invalid
     */
    @Transactional
    public boolean replaceSeededAdminPassword(final String password) {
        final var admin = this.findWithPublishedPassword(SEEDED_ADMIN_USERNAME);
        admin.ifPresent(user -> {
            this.validatePassword(password);
            user.password = this.passwordHashingService.hashPassword(password);
        });
        return admin.isPresent();
    }

    /**
     * Deactivates the demo accounts seeded by releases before 5.0.0 that still accept their published passwords. An
     * admin can set new passwords and reactivate them.
     *
     * @return the usernames of the accounts deactivated by this call
     */
    @Transactional
    public List<String> deactivateSeededDemoAccounts() {
        final var deactivated = new ArrayList<String>();
        for (final String username : SEEDED_DEMO_USERNAMES) {
            this.findWithPublishedPassword(username).filter(user -> user.activated).ifPresent(user -> {
                user.activated = false;
                deactivated.add(username);
            });
        }
        return deactivated;
    }

    private Optional<UserEntity> findWithPublishedPassword(final String seededUsername) {
        return this.userRepository.findByUsernameOptional(seededUsername)
                .filter(user -> this.passwordHashingService.verifyPassword(seededUsername, user.password));
    }

    private UserViewDto insertUser(final UserDto userDto, @Nullable final UserRankEntity caller) {
        // Validate required fields for POST
        if (userDto.username == null || userDto.username.isBlank()) {
            throw new ValidationException("Username is required for creating a user");
        }
        final String password = userDto.password;
        if (password == null) {
            throw new ValidationException("Password is required");
        }
        this.validatePassword(password);

        // Check for duplicate username
        final String normalizedUsername = this.normalizeUsername(userDto.username);
        if (this.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("Username '" + normalizedUsername + "' is already taken");
        }

        // Normalize email and check for duplicate email only if email is provided
        final String normalizedEmail = this.normalizeEmail(userDto.email);
        if (normalizedEmail != null && this.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
        }

        final UserEntity user = new UserEntity();
        user.username = normalizedUsername;
        user.email = normalizedEmail;
        final boolean setsStatus = caller == null || caller.has(Permission.ADMIN_VIEW);
        user.banned = setsStatus && Boolean.TRUE.equals(userDto.banned);
        user.activated = setsStatus && Boolean.TRUE.equals(userDto.activated);
        user.activationKey = UUID.randomUUID().toString();

        // Hash password with bcrypt
        final var hashedPassword = this.passwordHashingService.hashPassword(password);
        user.password = hashedPassword;

        this.applyRankToUser(user, userDto.rankPublicId, caller == null ? null : caller.granted());

        // Ensure avatar emoji defaults are set so Hibernate doesn't insert NULL
        if (user.userAvatarEmoji == null) {
            user.userAvatarEmoji = AppConstants.AVATAR_DEFAULT_USER;
        }
        if (user.tutorAvatarEmoji == null) {
            user.tutorAvatarEmoji = AppConstants.AVATAR_DEFAULT_TUTOR;
        }

        this.userRepository.persist(user);
        return new UserViewDto(user);
    }

    /**
     * Completely replaces an existing user account (PUT semantics). Updates username, email, banned/activated status,
     * rank, and password if provided. Validates duplicate username/email (skipping current values) and hashes new
     * passwords. Fields the caller's rank does not entitle them to change are ignored and keep their stored value:
     * {@code activated} and {@code banned} without the admin view permission, the rank (so {@code rankPublicId} may
     * then be absent) without the user rank edit permission. A non-null {@code userDto.version} must equal the stored
     * version, or the update is refused as stale; the check runs after the target is loaded and the privilege ceiling
     * is checked, before anything is modified. Null skips the check.
     *
     * @param publicId
     *            the user public ID to update
     * @param userDto
     *            the new user data
     * @return the updated {@link UserViewDto}
     * @throws WebApplicationException
     *             if user not found (NOT_FOUND status)
     * @throws ValidationException
     *             if username/email is duplicate, required fields missing, the user's current or new rank grants a
     *             permission the caller's rank lacks, the change would leave no active Administrator, it sets the
     *             caller's own password, or the DTO's version is stale
     */
    @Transactional
    public UserViewDto updateUser(final String publicId, final @Valid UserDto userDto) {
        this.userRepository.lockAdministrators();
        this.permissionService.requireUserEdit();

        // Validate required fields for PUT
        if (userDto.username == null || userDto.username.isBlank()) {
            throw new ValidationException("Username is required for updating a user");
        }

        final UserEntity existingUser = this.userRepository.findByPublicId(publicId).orElse(null);
        if (existingUser == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        final UserRankEntity caller = this.requireWithinCaller(existingUser);
        existingUser.requireVersion(userDto.version);

        // Check for duplicate username (only if username is different from current)
        final String normalizedUsername = this.normalizeUsername(userDto.username);
        if (!normalizedUsername.equals(existingUser.username) && this.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("Username '" + normalizedUsername + "' is already taken");
        }

        // Normalize email and check for duplicate email (only if email is different
        // from current)
        final String normalizedEmail = this.normalizeEmail(userDto.email);
        if (normalizedEmail != null && !Objects.equals(normalizedEmail, existingUser.email)
                && this.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
        }

        final boolean wasAdministrator = UserRepository.isActiveAdministrator(existingUser);
        // Complete replacement (PUT semantics)
        existingUser.username = normalizedUsername;
        existingUser.email = normalizedEmail;
        if (caller.has(Permission.ADMIN_VIEW)) {
            existingUser.banned = Boolean.TRUE.equals(userDto.banned);
            existingUser.activated = Boolean.TRUE.equals(userDto.activated);
        }

        // Handle password and rank updates
        this.applyPasswordToUser(existingUser, userDto.password != null ? userDto.password : "");
        if (caller.has(Permission.USER_RANK_EDIT)) {
            this.applyRankToUser(existingUser, userDto.rankPublicId, caller.granted());
        }
        return this.saveUpdatedUser(existingUser, wasAdministrator);
    }

    /**
     * Partially updates an existing user account (PATCH semantics). Only updates user properties that are explicitly
     * provided in the DTO; null values are ignored. Validates duplicate username/email if being changed, and hashes new
     * passwords if provided. Fields the caller's rank does not entitle them to change are ignored as well:
     * {@code activated} and {@code banned} without the admin view permission, {@code rankPublicId} without the user
     * rank edit permission. A non-null {@code userDto.version} is checked like a full update's; null skips the check.
     *
     * @param publicId
     *            the user public ID to update
     * @param userDto
     *            the partial user data with selected fields to update
     * @return the updated {@link UserViewDto}
     * @throws WebApplicationException
     *             if user not found (NOT_FOUND status)
     * @throws ValidationException
     *             if username/email is duplicate, the user's current or new rank grants a permission the caller's rank
     *             lacks, the change would leave no active Administrator, it sets the caller's own password, or the
     *             DTO's version is stale
     */
    @Transactional
    public UserViewDto patchUser(final String publicId, final @Valid UserDto userDto) {
        this.userRepository.lockAdministrators();
        this.permissionService.requireUserEdit();

        final UserEntity existingUser = this.userRepository.findByPublicId(publicId).orElse(null);
        if (existingUser == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        final UserRankEntity caller = this.requireWithinCaller(existingUser);
        existingUser.requireVersion(userDto.version);

        // Check for duplicate username if username is being updated
        final String normalizedUsername = userDto.username != null && !userDto.username.isBlank()
                ? this.normalizeUsername(userDto.username) : null;
        if (normalizedUsername != null && !normalizedUsername.equals(existingUser.username)
                && this.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("Username '" + normalizedUsername + "' is already taken");
        }

        // Check for duplicate email if email is being updated
        if (userDto.email != null) {
            final String normalizedEmail = this.normalizeEmail(userDto.email);
            if (normalizedEmail != null && !Objects.equals(normalizedEmail, existingUser.email)
                    && this.findByEmail(normalizedEmail).isPresent()) {
                throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
            }
        }

        final boolean wasAdministrator = UserRepository.isActiveAdministrator(existingUser);
        // Partial update (PATCH semantics) - only update provided fields
        if (normalizedUsername != null) {
            existingUser.username = normalizedUsername;
        }
        if (userDto.email != null) {
            existingUser.email = this.normalizeEmail(userDto.email);
        }
        if (caller.has(Permission.ADMIN_VIEW) && userDto.banned != null) {
            existingUser.banned = userDto.banned;
        }
        if (caller.has(Permission.ADMIN_VIEW) && userDto.activated != null) {
            existingUser.activated = userDto.activated;
        }

        // Handle password and rank updates (PATCH: only if provided)
        this.applyPasswordToUser(existingUser, userDto.password != null ? userDto.password : "");
        if (caller.has(Permission.USER_RANK_EDIT) && userDto.rankPublicId != null) {
            this.applyRankToUser(existingUser, userDto.rankPublicId, caller.granted());
        }
        return this.saveUpdatedUser(existingUser, wasAdministrator);
    }

    /**
     * Deletes a user account by public ID.
     *
     * @param publicId
     *            the user public ID to delete
     * @return {@code true} if deletion succeeded, {@code false} if user not found
     * @throws ValidationException
     *             if the user's rank grants a permission the caller's rank lacks, or the user is the last active
     *             Administrator
     */
    @Transactional
    public boolean deleteUser(final String publicId) {
        this.userRepository.lockAdministrators();
        this.permissionService.requireUserDelete();
        final var user = this.userRepository.findByPublicId(publicId).orElse(null);
        if (user == null) {
            return false;
        }
        this.requireWithinCaller(user);
        this.requireAdministratorRemains(user, UserRepository.isActiveAdministrator(user), false);
        final var deleted = this.userRepository.deleteByPublicId(publicId);
        this.fireAccountChanged(user);
        return deleted;
    }

    /**
     * Finishes an update: refuses it if it would leave no active Administrator, persists the user, and fires a
     * {@link UserAccountChangedEvent}, which evicts the authentication cache after the transaction commits.
     *
     * @param user
     *            the modified user
     * @param wasAdministrator
     *            whether the user was an active Administrator before the modification
     * @return the updated {@link UserViewDto}
     * @throws ValidationException
     *             if the user was the last active Administrator and no longer is
     */
    private UserViewDto saveUpdatedUser(final UserEntity user, final boolean wasAdministrator) {
        this.requireAdministratorRemains(user, wasAdministrator, UserRepository.isActiveAdministrator(user));
        this.userRepository.persist(user);
        // @Version advances at flush: flush so the returned DTO carries the new version
        this.userRepository.flush();
        this.fireAccountChanged(user);
        return new UserViewDto(user);
    }

    /**
     * Fires a {@link UserAccountChangedEvent} for the user; observers run after the transaction commits.
     */
    private void fireAccountChanged(final UserEntity user) {
        if (user.publicId != null) {
            this.accountChangedEvent.fire(new UserAccountChangedEvent(user.publicId));
        }
    }

    /**
     * Refuses a change that would leave no active Administrator (see {@link UserRepository#isActiveAdministrator}): one
     * that takes the status from a user while no other user holds it. The caller's transaction rolls back on the
     * exception, discarding changes already applied to the entity.
     *
     * @param user
     *            the user being changed or deleted
     * @param wasAdministrator
     *            whether the user was an active Administrator before the change
     * @param isAdministrator
     *            whether the user is one after it; false for a deletion
     * @throws ValidationException
     *             if the user was the last active Administrator and no longer is
     */
    private void requireAdministratorRemains(final UserEntity user, final boolean wasAdministrator,
            final boolean isAdministrator) {
        if (wasAdministrator && !isAdministrator && user.id != null
                && this.userRepository.countOtherActiveAdministrators(user.id) == 0) {
            throw new ValidationException(AppConstants.LAST_ADMINISTRATOR_MESSAGE);
        }
    }

    /**
     * Searches users by username or email using the provided query string (case-insensitive). Returns all users if
     * query is null or empty.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param query
     *            the search query string (username/email match)
     * @return a list of matching {@link UserViewDto}s
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not view the administration
     */
    @Transactional
    public List<UserViewDto> searchUsers(@Nullable final SessionCredentials credentials, final String query) {
        this.permissionService.requireAdminView(credentials);
        if (query == null || query.isBlank()) {
            return this.userRepository.findAll().stream().map(UserViewDto::new).toList();
        }
        // The repository takes the RAW term: it routes "@"-containing terms to the blind-index email
        // lookup (which needs the unmodified plaintext) and escapes/wraps everything else itself.
        final List<UserEntity> users = this.userRepository.search(query.trim());
        return users.stream().map(UserViewDto::new).toList();
    }

    /**
     * Returns the user the current session belongs to.
     *
     * @return the current user as a {@link UserViewDto}
     * @throws WebApplicationException
     *             with status UNAUTHORIZED if there is no current user: the session's credentials were revoked, the
     *             user is gone, or there is no session
     */
    @Transactional
    public UserViewDto getCurrentUser() {
        final var user = this.authService.getCurrentUserEntity();
        if (user == null) {
            throw new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED);
        }
        return new UserViewDto(user);
    }

    /**
     * Change user password after verifying current password. The caller's session is verified inside the transaction
     * (the user still exists, is active and the session's credential stamp is current), so a session revoked while a
     * page stayed open cannot change the password. The current-password check is throttled per session, apart from the
     * login throttles, so guessing it through a hijacked session is slowed down without locking the account out of the
     * login form.
     * 
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param currentPassword
     *            The current password for verification
     * @param newPassword
     *            The new password to set
     * @return the account's new credential stamp, to hand to {@link AuthService#renewCredentialStamp}
     * @throws ValidationException
     *             if the session has ended, too many wrong guesses were made recently, the current password is wrong,
     *             or the new password is invalid
     */
    @Transactional
    public String changePassword(final SessionCredentials credentials, final String currentPassword,
            final String newPassword) {
        final UserEntity user = this.authService.requireSessionUser(credentials);
        // Validated before the current password is checked: otherwise the new password's rejection would confirm a
        // correct guess without changing anything.
        this.validatePassword(newPassword);

        if (!this.loginAttemptService.tryRecordPasswordChangeAttempt(credentials.throttleKey())) {
            LOG.warnf("Refused a password change for user %s: too many wrong current passwords in this session",
                    user.publicId);
            throw new ValidationException("Too many failed attempts. Try again later.");
        }

        // Verify current password
        if (user.password == null || !this.passwordHashingService.verifyPassword(currentPassword, user.password)) {
            throw new ValidationException("Current password is incorrect");
        }
        this.loginAttemptService.clearPasswordChangeAttempts(credentials.throttleKey());

        // Hash new password with bcrypt
        final var hashedPassword = this.passwordHashingService.hashPassword(newPassword);
        user.password = hashedPassword;
        this.userRepository.persist(user);

        final var stamp = Objects.requireNonNull(AuthService.credentialStamp(hashedPassword));
        this.fireAccountChanged(user);
        return stamp;
    }

    /**
     * Update the calling user's appearance. The session is verified like in {@link #changePassword}. An appearance
     * change does not end any session, so no {@link UserAccountChangedEvent} is fired.
     * <p>
     * A null argument leaves that field as stored, so each control writes only the field it owns and cannot revert a
     * change made elsewhere.
     *
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param design
     *            the design to store, or null to keep the stored one
     * @param colorMode
     *            the colour mode to store, or null to keep the stored one
     * @return the appearance the row now holds
     * @throws ValidationException
     *             if the session has ended
     */
    @Transactional
    public Appearance updateAppearance(final SessionCredentials credentials, @Nullable final Appearance.Design design,
            @Nullable final Appearance.ColorMode colorMode) {
        final UserEntity user = this.authService.requireSessionUser(credentials);
        if (design != null) {
            user.design = design;
        }
        if (colorMode != null) {
            user.colorMode = colorMode;
        }
        this.userRepository.persist(user);
        return new Appearance(user.design, user.colorMode);
    }

    /**
     * Update the calling user's avatar emojis. The session is verified like in {@link #changePassword}.
     * 
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @param userEmoji
     *            The emoji for the user
     * @param tutorEmoji
     *            The emoji for the AI tutor
     * @throws ValidationException
     *             if the session has ended or an emoji is invalid
     */
    @Transactional
    public void updateAvatars(@Nullable final SessionCredentials credentials, final String userEmoji,
            final String tutorEmoji) {
        final UserEntity user = this.authService.requireSessionUser(credentials);

        // Validate emojis
        if (userEmoji == null || userEmoji.isBlank()) {
            throw new ValidationException("User avatar emoji cannot be empty");
        }
        if (tutorEmoji == null || tutorEmoji.isBlank()) {
            throw new ValidationException("Tutor avatar emoji cannot be empty");
        }
        if (userEmoji.length() > 10) {
            throw new ValidationException("User avatar emoji is too long");
        }
        if (tutorEmoji.length() > 10) {
            throw new ValidationException("Tutor avatar emoji is too long");
        }

        user.userAvatarEmoji = userEmoji;
        user.tutorAvatarEmoji = tutorEmoji;
        this.userRepository.persist(user);
    }

    /**
     * Get user settings (avatars only, no passwords).
     * 
     * @param credentials
     *            the credentials of the calling session, captured on the UI thread
     * @return UserSettingsDto with avatar settings
     * @throws ValidationException
     *             if the session has ended
     */
    @Transactional
    public UserSettingsDto getSettings(@Nullable final SessionCredentials credentials) {
        final UserEntity user = this.authService.requireSessionUser(credentials);

        return new UserSettingsDto(
                user.userAvatarEmoji != null ? user.userAvatarEmoji : AppConstants.AVATAR_DEFAULT_USER,
                user.tutorAvatarEmoji != null ? user.tutorAvatarEmoji : AppConstants.AVATAR_DEFAULT_TUTOR);
    }

    /**
     * Applies a new password to a user if provided and non-blank. The caller's own password is refused: only
     * {@link #changePassword} changes it, after checking the current one with a per-session attempt limit, so a
     * hijacked session can't reset its own account's password through the admin path.
     *
     * @param user
     *            the user to update
     * @param password
     *            the new password; null or blank leaves it unchanged
     * @throws ValidationException
     *             if a password is given and the user is the caller, or the password is invalid
     */
    private void applyPasswordToUser(final UserEntity user, final String password) {
        if (password != null && !password.isBlank()) {
            if (Objects.equals(user.id, this.authService.getUserId())) {
                throw new ValidationException(AppConstants.OWN_PASSWORD_MESSAGE);
            }
            this.validatePassword(password);
            user.password = this.passwordHashingService.hashPassword(password);
        }
    }

    /**
     * Applies a rank to a user by public ID. There is deliberately no default rank: the old fallback was row 1, the
     * Admin rank, so a caller that omitted the rank, or sent one deleted in the meantime, created an admin.
     *
     * @param user
     *            the user to update
     * @param rankPublicId
     *            the rank public ID
     * @param ceiling
     *            the caller's permissions, which the rank may not exceed; null only for a startup action, which has no
     *            caller
     * @throws ValidationException
     *             if {@code rankPublicId} is null, no rank has it, or the rank grants a permission the caller lacks
     */
    private void applyRankToUser(final UserEntity user, @Nullable final String rankPublicId,
            @Nullable final Set<Permission> ceiling) {
        if (rankPublicId == null) {
            throw new ValidationException("Rank is required");
        }
        final UserRankEntity rank = this.userRankRepository.findByPublicId(rankPublicId)
                .orElseThrow(() -> new ValidationException("Rank with public ID " + rankPublicId + " not found"));
        if (ceiling != null) {
            UserRankService.requireWithin(rank, ceiling);
        }
        user.rank = rank;
    }

    /**
     * Refuses to act on a user whose current rank grants a permission the caller's rank lacks. A user's own rank never
     * exceeds itself, so this never blocks changes to one's own account.
     *
     * @param user
     *            the user about to be changed or deleted
     * @return the caller's rank facts, for checking a newly assigned rank and gating fields
     * @throws ValidationException
     *             if the user's rank grants a permission the caller's rank lacks
     */
    private UserRankEntity requireWithinCaller(final UserEntity user) {
        final UserRankEntity caller = this.userRankService.requireCallerRank();
        UserRankService.requireWithin(user.rank, caller.granted());
        return caller;
    }
}
