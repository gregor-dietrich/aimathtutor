package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing user ranks and their associated permissions.
 * Provides operations for querying, creating, updating, and deleting user
 * ranks.
 */
@ApplicationScoped
public class UserRankService {

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    PermissionService permissionService;

    private static final String USERNAME_KEY = "authenticated.username";

    /**
     * Retrieves the rank of the currently authenticated user.
     *
     * @return a {@link UserRankViewDto} of the current user's rank, or null if not
     *         authenticated
     */
    @Transactional
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
     * Retrieves all available user ranks in the system.
     *
     * @return a list of all {@link UserRankViewDto} objects
     */
    @Transactional
    public List<UserRankViewDto> getAllRanks() {
        return this.userRankRepository.findAll().stream()
                .map(UserRankViewDto::new)
                .toList();
    }

    /**
     * Retrieves a user rank by its unique public identifier.
     *
     * @param publicId the rank public ID to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findByPublicId(final String publicId) {
        return this.userRankRepository.findByPublicId(publicId)
                .map(UserRankViewDto::new);
    }

    /**
     * Retrieves a user rank by its unique identifier.
     *
     * @param id the rank ID to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findById(final Long id) {
        return this.userRankRepository.findByIdOptional(id)
                .map(UserRankViewDto::new);
    }

    /**
     * Retrieves a user rank by its name.
     *
     * @param name the name of the rank to search for
     * @return an {@link Optional} containing the rank if found, empty otherwise
     */
    @Transactional
    public Optional<UserRankViewDto> findByName(final String name) {
        return this.userRankRepository.findByName(name)
                .map(UserRankViewDto::new);
    }

    /**
     * Searches for user ranks matching the given query term.
     *
     * @param query the search term to match against rank names;
     *              if null or empty, returns all ranks
     * @return a list of matching {@link UserRankViewDto} objects
     */
    @Transactional
    public List<UserRankViewDto> searchRanks(final String query) {
        if (query == null || query.isBlank()) {
            return this.getAllRanks();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<UserRankEntity> ranks = this.userRankRepository.search(searchTerm);
        return ranks.stream()
                .map(UserRankViewDto::new)
                .toList();
    }

    /**
     * Creates a new user rank with the provided permissions.
     * Initializes all permissions from the DTO with false defaults for unspecified
     * values.
     *
     * @param rankDto the rank data including name and permissions
     * @return the newly created {@link UserRankViewDto}
     * @throws IllegalArgumentException if rank name is invalid
     */
    @Transactional
    public UserRankViewDto createRank(final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankAdd();

        final UserRankEntity rank = new UserRankEntity();

        // Set properties from DTO
        rank.name = rankDto.name;

        this.applyAllPermissions(rank, rankDto);

        this.userRankRepository.persist(rank);
        return new UserRankViewDto(rank);
    }

    /**
     * Updates an existing user rank with new permission values.
     * Performs complete replacement of all permissions (PUT semantics).
     *
     * @param publicId the public ID of the rank to update
     * @param rankDto  the new rank data with updated permissions
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException if rank is not found (NOT_FOUND status)
     */
    @Transactional
    public UserRankViewDto updateRank(final String publicId, final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankEdit();

        final UserRankEntity existingRank = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingRank.name = rankDto.name;
        this.applyAllPermissions(existingRank, rankDto);

        this.userRankRepository.persist(existingRank);
        return new UserRankViewDto(existingRank);
    }

    /**
     * Partially updates an existing user rank (PATCH semantics).
     * Only updates permissions that are explicitly provided in the DTO; null values
     * are ignored.
     *
     * @param publicId the public ID of the rank to update
     * @param rankDto  the partial rank data with selected permissions to update
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException if rank is not found (NOT_FOUND status)
     */
    @Transactional
    public UserRankViewDto patchRank(final String publicId, final @Valid UserRankDto rankDto) {
        this.permissionService.requireUserRankEdit();

        final UserRankEntity existingRank = this.userRankRepository.findByPublicId(publicId).orElse(null);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (rankDto.name != null) {
            existingRank.name = rankDto.name;
        }
        this.applyProvidedPermissions(existingRank, rankDto);

        this.userRankRepository.persist(existingRank);
        return new UserRankViewDto(existingRank);
    }

    /**
     * Deletes a user rank by public ID.
     * Prevents deletion if users are currently assigned to this rank.
     *
     * @param publicId the public ID of the rank to delete
     * @return {@code true} if deletion succeeded, {@code false} if rank not found
     * @throws WebApplicationException if rank has assigned users (CONFLICT status)
     */
    @Transactional
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
                    "Cannot delete rank because "
                            + userCount
                            + " user(s) are assigned to this rank. Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }

        try {
            final boolean deleted = this.userRankRepository.deleteByPublicId(publicId);
            this.userRankRepository.flush();
            return deleted;
        } catch (final PersistenceException e) {
            throw new WebApplicationException(
                    "Cannot delete rank because users are assigned to this rank. Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }
    }

    /**
     * Applies all permission booleans from the DTO to the entity,
     * treating null DTO values as false.
     *
     * @param target the entity to update
     * @param source the DTO to read from
     */
    private void applyAllPermissions(final UserRankEntity target, final UserRankDto source) {
        target.adminView = source.adminView != null ? source.adminView : false;
        target.exerciseAdd = source.exerciseAdd != null ? source.exerciseAdd : false;
        target.exerciseDelete = source.exerciseDelete != null ? source.exerciseDelete : false;
        target.exerciseEdit = source.exerciseEdit != null ? source.exerciseEdit : false;
        target.lessonAdd = source.lessonAdd != null ? source.lessonAdd : false;
        target.lessonDelete = source.lessonDelete != null ? source.lessonDelete : false;
        target.lessonEdit = source.lessonEdit != null ? source.lessonEdit : false;
        target.commentAdd = source.commentAdd != null ? source.commentAdd : false;
        target.commentDelete = source.commentDelete != null ? source.commentDelete : false;
        target.commentEdit = source.commentEdit != null ? source.commentEdit : false;
        target.userAdd = source.userAdd != null ? source.userAdd : false;
        target.userDelete = source.userDelete != null ? source.userDelete : false;
        target.userEdit = source.userEdit != null ? source.userEdit : false;
        target.userGroupAdd = source.userGroupAdd != null ? source.userGroupAdd : false;
        target.userGroupDelete = source.userGroupDelete != null ? source.userGroupDelete : false;
        target.userGroupEdit = source.userGroupEdit != null ? source.userGroupEdit : false;
        target.userRankAdd = source.userRankAdd != null ? source.userRankAdd : false;
        target.userRankDelete = source.userRankDelete != null ? source.userRankDelete : false;
        target.userRankEdit = source.userRankEdit != null ? source.userRankEdit : false;
    }

    /**
     * Applies only the permission booleans that are explicitly provided
     * (non-null) in the DTO to the entity. Used for PATCH semantics.
     *
     * @param target the entity to update
     * @param source the DTO to read from
     */
    private void applyProvidedPermissions(final UserRankEntity target, final UserRankDto source) {
        if (source.adminView != null) {
            target.adminView = source.adminView;
        }
        if (source.exerciseAdd != null) {
            target.exerciseAdd = source.exerciseAdd;
        }
        if (source.exerciseDelete != null) {
            target.exerciseDelete = source.exerciseDelete;
        }
        if (source.exerciseEdit != null) {
            target.exerciseEdit = source.exerciseEdit;
        }
        if (source.lessonAdd != null) {
            target.lessonAdd = source.lessonAdd;
        }
        if (source.lessonDelete != null) {
            target.lessonDelete = source.lessonDelete;
        }
        if (source.lessonEdit != null) {
            target.lessonEdit = source.lessonEdit;
        }
        if (source.commentAdd != null) {
            target.commentAdd = source.commentAdd;
        }
        if (source.commentDelete != null) {
            target.commentDelete = source.commentDelete;
        }
        if (source.commentEdit != null) {
            target.commentEdit = source.commentEdit;
        }
        if (source.userAdd != null) {
            target.userAdd = source.userAdd;
        }
        if (source.userDelete != null) {
            target.userDelete = source.userDelete;
        }
        if (source.userEdit != null) {
            target.userEdit = source.userEdit;
        }
        if (source.userGroupAdd != null) {
            target.userGroupAdd = source.userGroupAdd;
        }
        if (source.userGroupDelete != null) {
            target.userGroupDelete = source.userGroupDelete;
        }
        if (source.userGroupEdit != null) {
            target.userGroupEdit = source.userGroupEdit;
        }
        if (source.userRankAdd != null) {
            target.userRankAdd = source.userRankAdd;
        }
        if (source.userRankDelete != null) {
            target.userRankDelete = source.userRankDelete;
        }
        if (source.userRankEdit != null) {
            target.userRankEdit = source.userRankEdit;
        }
    }
}
