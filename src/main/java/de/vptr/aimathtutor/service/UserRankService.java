package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
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

    private static final String USERNAME_KEY = AppConstants.SESSION_KEY_USERNAME;

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
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return null; // Return null instead of throwing when no session
        }

        final var username = (String) session.getAttribute(USERNAME_KEY);
        if (username == null) {
            return null; // Return null instead of throwing when not authenticated
        }
        // Use UserRepository to look up the user by username
        final var user = this.userRepository.findByUsername(username);
        if (user == null || user.rank == null) {
            return null; // Return null instead of throwing when user or rank not found
        }

        return new UserRankViewDto(user.rank);
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
        return this.getAllRanks(0, 100);
    }

    /**
     * Retrieves available user ranks with pagination. Cached per (page, pageSize) tuple; invalidated alongside the
     * no-arg overload on any rank mutation.
     *
     * @param page
     *            the page number (0-indexed)
     * @param pageSize
     *            the number of ranks per page
     * @return a paginated list of {@link UserRankViewDto} objects
     */
    @Transactional
    @CacheResult(cacheName = RANK_CACHE)
    public List<UserRankViewDto> getAllRanks(final int page, final int pageSize) {
        return this.userRankRepository.findAll(page, pageSize).stream().map(UserRankViewDto::new).toList();
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
     * Retrieves a user rank by its name. The query term is normalized with the same routine used when persisting ranks,
     * so lookups match the canonical names stored in the database.
     *
     * @param name
     *            the name of the rank to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     * @throws ValidationException
     *             if the name is null/blank or outside the allowed length bounds after normalization
     */
    @Transactional
    public Optional<UserRankViewDto> findByName(final String name) {
        final String normalizedName = this.normalizeAndValidateRankName(name);
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
     * @throws IllegalArgumentException
     *             if rank name is invalid
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto createRank(final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankAdd();

        final UserRankEntity rank = new UserRankEntity();

        // Store the name as canonical plain text: it is used for equality lookups (findByName) and
        // search, and is rendered as a Vaadin text node (escaped at output). HTML-encoding it here
        // would corrupt those lookups and the displayed value.
        rank.name = this.normalizeAndValidateRankName(rankDto.name);

        this.applyAllPermissions(rank, rankDto);

        this.userRankRepository.persist(rank);
        return new UserRankViewDto(rank);
    }

    /**
     * Updates an existing user rank with new permission values. Performs complete replacement of all permissions (PUT
     * semantics).
     *
     * @param publicId
     *            the public ID of the rank to update
     * @param rankDto
     *            the new rank data with updated permissions
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException
     *             if rank is not found (NOT_FOUND status)
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto updateRank(final String publicId, final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankEdit();

        final UserRankEntity existingRank = this.requireRankFound(publicId);

        // Complete replacement (PUT semantics)
        existingRank.name = this.normalizeAndValidateRankName(rankDto.name);
        this.applyAllPermissions(existingRank, rankDto);

        this.userRankRepository.persist(existingRank);
        return new UserRankViewDto(existingRank);
    }

    /**
     * Partially updates an existing user rank (PATCH semantics). Only updates permissions that are explicitly provided
     * in the DTO; null values are ignored.
     *
     * @param publicId
     *            the public ID of the rank to update
     * @param rankDto
     *            the partial rank data with selected permissions to update
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException
     *             if rank is not found (NOT_FOUND status)
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public UserRankViewDto patchRank(final String publicId, final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankEdit();

        final UserRankEntity existingRank = this.requireRankFound(publicId);

        // Partial update (PATCH semantics) - only update provided fields. A provided name is still
        // normalized and rejected when blank; a null name leaves the existing value untouched.
        if (rankDto.name != null) {
            existingRank.name = this.normalizeAndValidateRankName(rankDto.name);
        }
        this.applyProvidedPermissions(existingRank, rankDto);

        this.userRankRepository.persist(existingRank);
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
     */
    @Transactional
    @CacheInvalidateAll(cacheName = RANK_CACHE)
    public boolean deleteRank(final String publicId) {
        this.permissionService.requireUserRankDelete();

        final UserRankEntity rank = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (rank == null) {
            return false;
        }

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
     * Applies all permission booleans from the DTO to the entity, treating null DTO values as false.
     *
     * @param target
     *            the entity to update
     * @param source
     *            the DTO to read from
     */
    private void applyAllPermissions(final UserRankEntity target, final UserRankDto source) {
        target.adminView = Boolean.TRUE.equals(source.adminView);
        this.applyExercisePermissions(target, source, false);
        this.applyLessonPermissions(target, source, false);
        this.applyCommentPermissions(target, source, false);
        this.applyUserPermissions(target, source, false);
        this.applyGroupPermissions(target, source, false);
        this.applyRankPermissions(target, source, false);
        target.aiConfigEdit = Boolean.TRUE.equals(source.aiConfigEdit);
    }

    /**
     * Applies only the permission booleans that are explicitly provided (non-null) in the DTO to the entity. Used for
     * PATCH semantics.
     *
     * @param target
     *            the entity to update
     * @param source
     *            the DTO to read from
     */
    private void applyProvidedPermissions(final UserRankEntity target, final UserRankDto source) {
        if (source.adminView != null) {
            target.adminView = source.adminView;
        }
        this.applyExercisePermissions(target, source, true);
        this.applyLessonPermissions(target, source, true);
        this.applyCommentPermissions(target, source, true);
        this.applyUserPermissions(target, source, true);
        this.applyGroupPermissions(target, source, true);
        this.applyRankPermissions(target, source, true);
        if (source.aiConfigEdit != null) {
            target.aiConfigEdit = source.aiConfigEdit;
        }
    }

    private void applyExercisePermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.exerciseAdd != null) {
            target.exerciseAdd = Boolean.TRUE.equals(source.exerciseAdd);
        }
        if (!patch || source.exerciseEdit != null) {
            target.exerciseEdit = Boolean.TRUE.equals(source.exerciseEdit);
        }
        if (!patch || source.exerciseDelete != null) {
            target.exerciseDelete = Boolean.TRUE.equals(source.exerciseDelete);
        }
    }

    private void applyLessonPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.lessonAdd != null) {
            target.lessonAdd = Boolean.TRUE.equals(source.lessonAdd);
        }
        if (!patch || source.lessonEdit != null) {
            target.lessonEdit = Boolean.TRUE.equals(source.lessonEdit);
        }
        if (!patch || source.lessonDelete != null) {
            target.lessonDelete = Boolean.TRUE.equals(source.lessonDelete);
        }
    }

    private void applyCommentPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.commentAdd != null) {
            target.commentAdd = Boolean.TRUE.equals(source.commentAdd);
        }
        if (!patch || source.commentEdit != null) {
            target.commentEdit = Boolean.TRUE.equals(source.commentEdit);
        }
        if (!patch || source.commentDelete != null) {
            target.commentDelete = Boolean.TRUE.equals(source.commentDelete);
        }
    }

    private void applyUserPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.userAdd != null) {
            target.userAdd = Boolean.TRUE.equals(source.userAdd);
        }
        if (!patch || source.userEdit != null) {
            target.userEdit = Boolean.TRUE.equals(source.userEdit);
        }
        if (!patch || source.userDelete != null) {
            target.userDelete = Boolean.TRUE.equals(source.userDelete);
        }
    }

    private void applyGroupPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.userGroupAdd != null) {
            target.userGroupAdd = Boolean.TRUE.equals(source.userGroupAdd);
        }
        if (!patch || source.userGroupEdit != null) {
            target.userGroupEdit = Boolean.TRUE.equals(source.userGroupEdit);
        }
        if (!patch || source.userGroupDelete != null) {
            target.userGroupDelete = Boolean.TRUE.equals(source.userGroupDelete);
        }
    }

    private void applyRankPermissions(final UserRankEntity target, final UserRankDto source, final boolean patch) {
        if (!patch || source.userRankAdd != null) {
            target.userRankAdd = Boolean.TRUE.equals(source.userRankAdd);
        }
        if (!patch || source.userRankEdit != null) {
            target.userRankEdit = Boolean.TRUE.equals(source.userRankEdit);
        }
        if (!patch || source.userRankDelete != null) {
            target.userRankDelete = Boolean.TRUE.equals(source.userRankDelete);
        }
    }

    /**
     * Normalizes a rank name to canonical plain text and validates it: surrounding whitespace is trimmed and internal
     * whitespace runs are collapsed to single spaces, then null/blank and out-of-bounds lengths are rejected. The
     * length check enforces the same {@code @Size} bounds declared on {@link UserRankDto#name} against the normalized
     * value, so persisted names always satisfy them even though normalization can shrink the input.
     *
     * @param name
     *            the raw rank name from the DTO
     * @return the normalized, non-blank rank name
     * @throws ValidationException
     *             if the name is null/blank after normalization, or outside the allowed length bounds
     */
    private String normalizeAndValidateRankName(@Nullable final String name) {
        if (name == null) {
            throw new ValidationException("Name is required");
        }
        final String normalized = WHITESPACE_PATTERN.matcher(name).replaceAll(" ").trim();
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

    private UserRankEntity requireRankFound(final String publicId) {
        final var existing = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (existing == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }
        return existing;
    }
}
