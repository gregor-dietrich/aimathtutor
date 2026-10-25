package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.SearchPatternUtil;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing user ranks and their associated permissions. Provides operations for querying, creating,
 * updating, and deleting user ranks.
 */
@ApplicationScoped
public class UserRankService {

    /**
     * Quarkus cache name for the rank-list read path. Ranks change rarely (admin action) but are read on most admin
     * page loads, so we cache aggressively and invalidate on every write.
     */
    private static final String RANK_CACHE = "user-ranks";

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    PermissionService permissionService;

    @Inject
    AuthService authService;

    /** Matches runs of whitespace, used to collapse rank names to canonical single-spaced plain text. */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Retrieves the rank of the currently authenticated user.
     *
     * @return a {@link UserRankViewDto} of the current user's rank, or null if not authenticated
     */
    @Transactional
    @Nullable
    public UserRankViewDto getCurrentUserRank() {
        final UserRankEntity rank = this.getCurrentUserRankEntity();
        return rank != null ? new UserRankViewDto(rank) : null;
    }

    /**
     * Retrieves the rank entity of the currently authenticated user. Fails closed: a user who was banned or deactivated
     * while a page was open gets no rank, so the privilege ceiling and the admin views' gating refuse them. A session
     * revoked by a password change gets no rank either. The {@link PermissionService} session checks refuse those cases
     * themselves, through {@link AuthService#requireCurrentUser()}.
     *
     * @return the current user's {@link UserRankEntity}, or null if no user is authenticated, the user no longer
     *         exists, has no rank, is banned or not activated, or the session's credentials are revoked
     */
    @Transactional
    @Nullable
    public UserRankEntity getCurrentUserRankEntity() {
        final var user = this.authService.getCurrentUserEntity();
        return user != null && user.isActive() ? user.rank : null;
    }

    /**
     * Retrieves all available user ranks in the system. Result is cached because ranks are read on most admin requests
     * but rarely written; cache is invalidated by {@link #createRank}, {@link #updateRank}, {@link #patchRank}, and
     * {@link #deleteRank}.
     *
     * @return a list of all {@link UserRankViewDto} objects
     */
    @Transactional
    @CacheResult(cacheName = RANK_CACHE)
    public List<UserRankViewDto> getAllRanks() {
        return this.userRankRepository.findAll().stream().map(UserRankViewDto::new).toList();
    }

    /**
     * Retrieves a user rank by its unique public identifier.
     *
     * @param publicId
     *            the rank public ID to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findByPublicId(final String publicId) {
        return this.userRankRepository.findByPublicId(publicId).map(UserRankViewDto::new);
    }

    /**
     * Retrieves a user rank by its unique identifier.
     *
     * @param id
     *            the rank ID to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findById(final Long id) {
        return this.userRankRepository.findByIdOptional(id).map(UserRankViewDto::new);
    }

    /**
     * Retrieves a user rank by its name. The query term is normalized with the same whitespace routine used when
     * persisting ranks, so lookups match the canonical names stored in the database. A null/blank term cannot match any
     * stored rank, so it yields an empty result rather than an error.
     *
     * @param name
     *            the name of the rank to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findByName(@Nullable final String name) {
        final String normalizedName = this.normalizeRankName(name);
        if (normalizedName.isEmpty()) {
            return Optional.empty();
        }
        return this.userRankRepository.findByName(normalizedName).map(UserRankViewDto::new);
    }

    /**
     * Searches for user ranks matching the given query term.
     *
     * @param query
     *            the search term to match against rank names; if null or empty, returns all ranks
     * @return a list of matching {@link UserRankViewDto} objects
     */
    @Transactional
    public List<UserRankViewDto> searchRanks(final String query) {
        if (query == null || query.isBlank()) {
            return this.getAllRanks();
        }
        final var searchTerm = SearchPatternUtil.containsPattern(query.trim().toLowerCase(Locale.ROOT));
        final List<UserRankEntity> ranks = this.userRankRepository.search(searchTerm);
        return ranks.stream().map(UserRankViewDto::new).toList();
    }

    /**
     * Creates a new user rank with the provided permissions. Initializes all permissions from the DTO with false
     * defaults for unspecified values.
     *
     * @param rankDto
     *            the rank data including name and permissions
     * @return the newly created {@link UserRankViewDto}
     * @throws ValidationException
     *             if the rank name is invalid, or the rank grants a permission the caller's rank lacks, or the session
     *             has ended
     * @throws PermissionDeniedException
     *             if the caller may not add ranks
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto createRank(final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankAdd();
        final Set<Permission> ceiling = this.requireCallerPermissions();

        final UserRankEntity rank = new UserRankEntity();

        // Store the name as canonical plain text: it is used for equality lookups (findByName) and
        // search, and is rendered as a Vaadin text node (escaped at output). HTML-encoding it here
        // would corrupt those lookups and the displayed value.
        rank.name = this.normalizeAndValidateRankName(rankDto.name);

        applyPermissions(rank, rankDto, false);
        requireWithin(rank, ceiling);

        this.userRankRepository.persist(rank);
        return new UserRankViewDto(rank);
    }

    /**
     * Updates an existing user rank with new permission values. Performs complete replacement of all permissions (PUT
     * semantics). A non-null {@code rankDto.version} must equal the stored version, or the update is refused as stale;
     * the check runs after the target is loaded and the privilege ceiling is checked, before anything is modified. Null
     * skips the check.
     *
     * @param publicId
     *            the public ID of the rank to update
     * @param rankDto
     *            the new rank data with updated permissions
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException
     *             if rank is not found (NOT_FOUND status)
     * @throws ValidationException
     *             if the rank, before or after the change, grants a permission the caller's rank lacks, the change
     *             would leave no active Administrator, or the DTO's version is stale, or the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not edit ranks
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto updateRank(final String publicId, final @Valid UserRankDto rankDto) {
        final var target = this.loadRankForEdit(publicId, rankDto.version);
        final UserRankEntity existingRank = target.rank();
        final Set<Permission> ceiling = target.ceiling();
        final boolean holdsLastAdministrators = this.holdsLastAdministrators(existingRank);

        // Complete replacement (PUT semantics)
        existingRank.name = this.normalizeAndValidateRankName(rankDto.name);
        applyPermissions(existingRank, rankDto, false);
        requireWithin(existingRank, ceiling);
        requireAdministrationKept(existingRank, holdsLastAdministrators);

        this.userRankRepository.persist(existingRank);
        // @Version advances at flush: flush so the returned DTO carries the new version
        this.userRankRepository.flush();
        return new UserRankViewDto(existingRank);
    }

    /**
     * Partially updates an existing user rank (PATCH semantics). Only updates permissions that are explicitly provided
     * in the DTO; null values are ignored. A non-null {@code rankDto.version} is checked like a full update's; null
     * skips the check.
     *
     * @param publicId
     *            the public ID of the rank to update
     * @param rankDto
     *            the partial rank data with selected permissions to update
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException
     *             if rank is not found (NOT_FOUND status)
     * @throws ValidationException
     *             if the rank, before or after the change, grants a permission the caller's rank lacks, or the change
     *             would leave no active Administrator, or the DTO's version is stale, or the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not edit ranks
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto patchRank(final String publicId, final @Valid UserRankDto rankDto) {
        final var target = this.loadRankForEdit(publicId, rankDto.version);
        final UserRankEntity existingRank = target.rank();
        final Set<Permission> ceiling = target.ceiling();
        final boolean holdsLastAdministrators = this.holdsLastAdministrators(existingRank);

        // Partial update (PATCH semantics) - only update provided fields. A provided name is still
        // normalized and rejected when blank; a null name leaves the existing value untouched.
        if (rankDto.name != null) {
            existingRank.name = this.normalizeAndValidateRankName(rankDto.name);
        }
        applyPermissions(existingRank, rankDto, true);
        requireWithin(existingRank, ceiling);
        requireAdministrationKept(existingRank, holdsLastAdministrators);

        this.userRankRepository.persist(existingRank);
        // @Version advances at flush: flush so the returned DTO carries the new version
        this.userRankRepository.flush();
        return new UserRankViewDto(existingRank);
    }

    /**
     * Deletes a user rank by public ID. Prevents deletion if users are currently assigned to this rank.
     *
     * @param publicId
     *            the public ID of the rank to delete
     * @return {@code true} if deletion succeeded, {@code false} if rank not found
     * @throws WebApplicationException
     *             if rank has assigned users (CONFLICT status)
     * @throws ValidationException
     *             if the rank grants a permission the caller's rank lacks, or the session has ended
     * @throws PermissionDeniedException
     *             if the caller may not delete ranks
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public boolean deleteRank(final String publicId) {
        this.permissionService.requireUserRankDelete();

        final UserRankEntity rank = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (rank == null) {
            return false;
        }
        requireWithin(rank, this.requireCallerPermissions());

        // Check if rank has associated users using COUNT query
        final long userCount = this.userRepository.countByRankPublicId(publicId);
        if (userCount > 0) {
            throw new WebApplicationException(
                    "Cannot delete rank because " + userCount + " user(s) are assigned to this rank. "
                            + "Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }

        try {
            final boolean deleted = this.userRankRepository.deleteByPublicId(publicId);
            this.userRankRepository.flush();
            return deleted;
        } catch (final PersistenceException e) {
            throw new WebApplicationException(
                    "Cannot delete rank because users are assigned to this rank. "
                            + "Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }
    }

    /**
     * Returns the current user's permissions: the ceiling for the users and ranks they may change, assign or grant.
     * Callers editing a rank must read it before the edit, because the rank may be the caller's own.
     *
     * @return the current user's rank permissions, as returned by {@link UserRankEntity#granted()}
     * @throws PermissionDeniedException
     *             if there is no current user with a rank; the permission check that runs first makes that unexpected
     */
    public Set<Permission> requireCallerPermissions() {
        return this.requireCallerRank().granted();
    }

    /**
     * Returns the current user's rank, for callers that need more of it than its permissions.
     *
     * @return the current user's rank
     * @throws ValidationException
     *             if the session has ended, e.g. the caller was deleted after the permission check that runs first
     * @throws PermissionDeniedException
     *             if the current user has no rank; the permission check that runs first makes that unexpected
     */
    public UserRankEntity requireCallerRank() {
        final UserRankEntity rank = this.permissionService.findCurrentUserRank();
        if (rank == null) {
            // Without the administrator lock, the caller may have been deleted since the permission check read it
            this.authService.requireCurrentUser();
            throw new PermissionDeniedException("You do not have permission to perform this action");
        }
        return rank;
    }

    /**
     * Refuses a rank that grants a permission outside the caller's {@code ceiling}: one the caller may not create,
     * edit, delete or assign.
     *
     * @param rank
     *            the rank to check; null passes
     * @param ceiling
     *            the caller's permissions, from {@link #requireCallerPermissions()}
     * @throws ValidationException
     *             if the rank grants a permission the caller's rank lacks
     */
    public static void requireWithin(@Nullable final UserRankEntity rank, final Set<Permission> ceiling) {
        if (rank != null && rank.grantsBeyond(ceiling)) {
            throw new ValidationException(AppConstants.PRIVILEGE_CEILING_MESSAGE);
        }
    }

    /**
     * Whether every active Administrator holds this rank, and there is at least one, so that taking any permission from
     * the rank (see {@link UserRepository#grantsAdministration}) would leave none. Must run before the rank is
     * modified: Hibernate flushes pending changes before the count queries.
     *
     * @param rank
     *            the unmodified rank
     * @return true if the rank's users are all the active Administrators there are
     */
    private boolean holdsLastAdministrators(final UserRankEntity rank) {
        return UserRepository.grantsAdministration(rank) && rank.id != null
                && this.userRepository.countActiveAdministratorsOutsideRank(rank.id) == 0
                && this.userRepository.countActiveAdministrators() > 0;
    }

    /**
     * Refuses a rank change that takes administration from the rank holding the last active Administrators. The
     * caller's transaction rolls back on the exception, discarding the changes already applied to the rank.
     *
     * @param rank
     *            the modified rank
     * @param holdsLastAdministrators
     *            the result of {@link #holdsLastAdministrators} before the modification
     * @throws ValidationException
     *             if the rank held the last active Administrators and no longer grants administration
     */
    private static void requireAdministrationKept(final UserRankEntity rank, final boolean holdsLastAdministrators) {
        if (holdsLastAdministrators && !UserRepository.grantsAdministration(rank)) {
            throw new ValidationException(AppConstants.LAST_ADMINISTRATOR_MESSAGE);
        }
    }

    /**
     * The one writer of a rank's permission flags. A constant without a case here fails {@code PermissionTest}.
     *
     * @param rank
     *            the rank to change
     * @param permission
     *            the permission to write
     * @param value
     *            true to grant it
     */
    public static void setPermission(final UserRankEntity rank, final Permission permission, final boolean value) {
        switch (permission) {
            case ADMIN_VIEW -> {
                rank.adminView = value;
            }
            case EXERCISE_ADD -> {
                rank.exerciseAdd = value;
            }
            case EXERCISE_EDIT -> {
                rank.exerciseEdit = value;
            }
            case EXERCISE_DELETE -> {
                rank.exerciseDelete = value;
            }
            case LESSON_ADD -> {
                rank.lessonAdd = value;
            }
            case LESSON_EDIT -> {
                rank.lessonEdit = value;
            }
            case LESSON_DELETE -> {
                rank.lessonDelete = value;
            }
            case COMMENT_ADD -> {
                rank.commentAdd = value;
            }
            case COMMENT_EDIT -> {
                rank.commentEdit = value;
            }
            case COMMENT_DELETE -> {
                rank.commentDelete = value;
            }
            case USER_ADD -> {
                rank.userAdd = value;
            }
            case USER_EDIT -> {
                rank.userEdit = value;
            }
            case USER_DELETE -> {
                rank.userDelete = value;
            }
            case USER_GROUP_ADD -> {
                rank.userGroupAdd = value;
            }
            case USER_GROUP_EDIT -> {
                rank.userGroupEdit = value;
            }
            case USER_GROUP_DELETE -> {
                rank.userGroupDelete = value;
            }
            case USER_RANK_ADD -> {
                rank.userRankAdd = value;
            }
            case USER_RANK_EDIT -> {
                rank.userRankEdit = value;
            }
            case USER_RANK_DELETE -> {
                rank.userRankDelete = value;
            }
            case AI_CONFIG_EDIT -> {
                rank.aiConfigEdit = value;
            }
            default -> throw new IllegalArgumentException("No field for " + permission);
        }
    }

    /**
     * Applies the DTO's permissions to the entity. A permission the DTO does not provide becomes false in a full update
     * and stays as it is in a partial one.
     *
     * @param target
     *            the entity to update
     * @param source
     *            the DTO to read from
     * @param patch
     *            true for PATCH semantics
     */
    private static void applyPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        for (final var permission : Permission.values()) {
            if (!patch || source.get(permission) != null) {
                setPermission(target, permission, source.has(permission));
            }
        }
    }

    /**
     * Normalizes a rank name to canonical plain text: surrounding whitespace is trimmed and internal whitespace runs
     * are collapsed to single spaces. A null input yields an empty string. Does not validate length or blankness — the
     * write paths use {@link #normalizeAndValidateRankName(String)} for that.
     *
     * @param name
     *            the raw rank name
     * @return the normalized name, or an empty string if {@code name} is null or whitespace-only
     */
    private String normalizeRankName(@Nullable final String name) {
        if (name == null) {
            return "";
        }
        return WHITESPACE_PATTERN.matcher(name).replaceAll(" ").trim();
    }

    /**
     * Normalizes a rank name with {@link #normalizeRankName(String)} and validates it: null/blank and out-of-bounds
     * lengths are rejected. The length check enforces the same {@code @Size} bounds declared on
     * {@link UserRankDto#name} against the normalized value, so persisted names always satisfy them even though
     * normalization can shrink the input.
     *
     * @param name
     *            the raw rank name from the DTO
     * @return the normalized, non-blank rank name
     * @throws ValidationException
     *             if the name is null/blank after normalization, or outside the allowed length bounds
     */
    private String normalizeAndValidateRankName(@Nullable final String name) {
        final String normalized = this.normalizeRankName(name);
        if (normalized.isEmpty()) {
            throw new ValidationException("Name is required");
        }
        if (normalized.length() < AppConstants.USERRANK_NAME_MIN_LENGTH
                || normalized.length() > AppConstants.USERRANK_NAME_MAX_LENGTH) {
            throw new ValidationException("Name must be between " + AppConstants.USERRANK_NAME_MIN_LENGTH + " and "
                    + AppConstants.USERRANK_NAME_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    /** A rank loaded for an edit and the caller's permissions, read before the edit. */
    private record RankEdit(UserRankEntity rank, Set<Permission> ceiling) {
    }

    /**
     * Takes the administrator lock, checks the edit permission, loads the rank, and refuses it above the caller's
     * ceiling or when {@code version} is stale, all before anything is modified.
     */
    private RankEdit loadRankForEdit(final String publicId, @Nullable final Long version) {
        this.userRepository.lockAdministrators();
        this.permissionService.requireUserRankEdit();

        final UserRankEntity rank = this.requireRankFound(publicId);
        // Read before the edit: the caller's own rank may be the one being edited
        final Set<Permission> ceiling = this.requireCallerPermissions();
        requireWithin(rank, ceiling);
        rank.requireVersion(version);
        return new RankEdit(rank, ceiling);
    }

    private UserRankEntity requireRankFound(final String publicId) {
        final var existing = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (existing == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }
        return existing;
    }
}
