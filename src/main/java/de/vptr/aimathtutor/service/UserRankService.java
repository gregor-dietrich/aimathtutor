package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    public UserRankViewDto createRank(final UserRankDto rankDto) {
        final UserRankEntity rank = new UserRankEntity();

        // Set properties from DTO
        rank.name = rankDto.name;

        // Set permissions from DTO with defaults
        rank.adminView = rankDto.adminView != null ? rankDto.adminView : false;
        rank.exerciseAdd = rankDto.exerciseAdd != null ? rankDto.exerciseAdd : false;
        rank.exerciseDelete = rankDto.exerciseDelete != null ? rankDto.exerciseDelete : false;
        rank.exerciseEdit = rankDto.exerciseEdit != null ? rankDto.exerciseEdit : false;
        rank.lessonAdd = rankDto.lessonAdd != null ? rankDto.lessonAdd : false;
        rank.lessonDelete = rankDto.lessonDelete != null ? rankDto.lessonDelete : false;
        rank.lessonEdit = rankDto.lessonEdit != null ? rankDto.lessonEdit : false;
        rank.commentAdd = rankDto.commentAdd != null ? rankDto.commentAdd : false;
        rank.commentDelete = rankDto.commentDelete != null ? rankDto.commentDelete : false;
        rank.commentEdit = rankDto.commentEdit != null ? rankDto.commentEdit : false;
        rank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        rank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        rank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        rank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        rank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        rank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        rank.userRankAdd = rankDto.userRankAdd != null ? rankDto.userRankAdd : false;
        rank.userRankDelete = rankDto.userRankDelete != null ? rankDto.userRankDelete : false;
        rank.userRankEdit = rankDto.userRankEdit != null ? rankDto.userRankEdit : false;

        this.userRankRepository.persist(rank);
        return new UserRankViewDto(rank);
    }

    /**
     * Updates an existing user rank with new permission values.
     * Performs complete replacement of all permissions (PUT semantics).
     *
     * @param id      the ID of the rank to update
     * @param rankDto the new rank data with updated permissions
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException if rank is not found (NOT_FOUND status)
     */
    @Transactional
    public UserRankViewDto updateRank(final Long id, final UserRankDto rankDto) {
        final UserRankEntity existingRank = this.userRankRepository.findById(id);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingRank.name = rankDto.name;
        existingRank.adminView = rankDto.adminView != null ? rankDto.adminView : false;
        existingRank.exerciseAdd = rankDto.exerciseAdd != null ? rankDto.exerciseAdd : false;
        existingRank.exerciseDelete = rankDto.exerciseDelete != null ? rankDto.exerciseDelete : false;
        existingRank.exerciseEdit = rankDto.exerciseEdit != null ? rankDto.exerciseEdit : false;
        existingRank.lessonAdd = rankDto.lessonAdd != null ? rankDto.lessonAdd : false;
        existingRank.lessonDelete = rankDto.lessonDelete != null ? rankDto.lessonDelete : false;
        existingRank.lessonEdit = rankDto.lessonEdit != null ? rankDto.lessonEdit : false;
        existingRank.commentAdd = rankDto.commentAdd != null ? rankDto.commentAdd : false;
        existingRank.commentDelete = rankDto.commentDelete != null ? rankDto.commentDelete : false;
        existingRank.commentEdit = rankDto.commentEdit != null ? rankDto.commentEdit : false;
        existingRank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        existingRank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        existingRank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        existingRank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        existingRank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        existingRank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        existingRank.userRankAdd = rankDto.userRankAdd != null ? rankDto.userRankAdd : false;
        existingRank.userRankDelete = rankDto.userRankDelete != null ? rankDto.userRankDelete : false;
        existingRank.userRankEdit = rankDto.userRankEdit != null ? rankDto.userRankEdit : false;

        this.userRankRepository.persist(existingRank);
        return new UserRankViewDto(existingRank);
    }

    /**
     * Partially updates an existing user rank (PATCH semantics).
     * Only updates permissions that are explicitly provided in the DTO; null values
     * are ignored.
     *
     * @param id      the ID of the rank to update
     * @param rankDto the partial rank data with selected permissions to update
     * @return the updated {@link UserRankViewDto}
     * @throws WebApplicationException if rank is not found (NOT_FOUND status)
     */
    @Transactional
    public UserRankViewDto patchRank(final Long id, final UserRankDto rankDto) {
        final UserRankEntity existingRank = this.userRankRepository.findById(id);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (rankDto.name != null) {
            existingRank.name = rankDto.name;
        }
        if (rankDto.adminView != null) {
            existingRank.adminView = rankDto.adminView;
        }
        if (rankDto.exerciseAdd != null) {
            existingRank.exerciseAdd = rankDto.exerciseAdd;
        }
        if (rankDto.exerciseDelete != null) {
            existingRank.exerciseDelete = rankDto.exerciseDelete;
        }
        if (rankDto.exerciseEdit != null) {
            existingRank.exerciseEdit = rankDto.exerciseEdit;
        }
        if (rankDto.lessonAdd != null) {
            existingRank.lessonAdd = rankDto.lessonAdd;
        }
        if (rankDto.lessonDelete != null) {
            existingRank.lessonDelete = rankDto.lessonDelete;
        }
        if (rankDto.lessonEdit != null) {
            existingRank.lessonEdit = rankDto.lessonEdit;
        }
        if (rankDto.commentAdd != null) {
            existingRank.commentAdd = rankDto.commentAdd;
        }
        if (rankDto.commentDelete != null) {
            existingRank.commentDelete = rankDto.commentDelete;
        }
        if (rankDto.commentEdit != null) {
            existingRank.commentEdit = rankDto.commentEdit;
        }
        if (rankDto.userAdd != null) {
            existingRank.userAdd = rankDto.userAdd;
        }
        if (rankDto.userDelete != null) {
            existingRank.userDelete = rankDto.userDelete;
        }
        if (rankDto.userEdit != null) {
            existingRank.userEdit = rankDto.userEdit;
        }
        if (rankDto.userGroupAdd != null) {
            existingRank.userGroupAdd = rankDto.userGroupAdd;
        }
        if (rankDto.userGroupDelete != null) {
            existingRank.userGroupDelete = rankDto.userGroupDelete;
        }
        if (rankDto.userGroupEdit != null) {
            existingRank.userGroupEdit = rankDto.userGroupEdit;
        }
        if (rankDto.userRankAdd != null) {
            existingRank.userRankAdd = rankDto.userRankAdd;
        }
        if (rankDto.userRankDelete != null) {
            existingRank.userRankDelete = rankDto.userRankDelete;
        }
        if (rankDto.userRankEdit != null) {
            existingRank.userRankEdit = rankDto.userRankEdit;
        }

        this.userRankRepository.persist(existingRank);
        return new UserRankViewDto(existingRank);
    }

    /**
     * Deletes a user rank by ID.
     * Prevents deletion if users are currently assigned to this rank.
     *
     * @param id the ID of the rank to delete
     * @return {@code true} if deletion succeeded, {@code false} if rank not found
     * @throws WebApplicationException if rank has assigned users (CONFLICT status)
     */
    @Transactional
    public boolean deleteRank(final Long id) {
        final UserRankEntity rank = this.userRankRepository.findById(id);
        if (rank == null) {
            return false;
        }

        // Check if rank has associated users
        final List<UserEntity> usersWithRank = this.userRepository.findByRankId(id);
        if (!usersWithRank.isEmpty()) {
            throw new WebApplicationException(
                    "Cannot delete rank because "
                            + usersWithRank.size()
                            + " user(s) are assigned to this rank. Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }

        return this.userRankRepository.deleteById(id);
    }
}
