package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

import de.vptr.aimathtutor.dto.UserGroupDto;
import de.vptr.aimathtutor.dto.UserGroupViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserGroupEntity;
import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import de.vptr.aimathtutor.repository.UserGroupMetaRepository;
import de.vptr.aimathtutor.repository.UserGroupRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for managing user groups and group membership.
 * Provides group CRUD operations and user membership management via
 * {@link UserGroupMetaEntity}.
 * Maintains many-to-many relationships between users and groups.
 */
@ApplicationScoped
public class UserGroupService {

    @Inject
    UserGroupRepository userGroupRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserGroupMetaRepository userGroupMetaRepository;

    @Inject
    PermissionService permissionService;

    /**
     * Retrieves all user groups.
     *
     * @return a list of all {@link UserGroupViewDto}s
     */
    @Transactional
    public List<UserGroupViewDto> getAllGroups() {
        return this.userGroupRepository.findAll().stream()
                .map(UserGroupViewDto::new)
                .toList();
    }

    /**
     * Finds a user group by ID.
     *
     * @param id the group ID
     * @return an {@link Optional} containing the {@link UserGroupViewDto}, or empty
     *         if not found
     */
    @Transactional
    public Optional<UserGroupViewDto> findById(final Long id) {
        return this.userGroupRepository.findByIdOptional(id)
                .map(UserGroupViewDto::new);
    }

    /**
     * Finds a user group by name.
     *
     * @param name the group name
     * @return an {@link Optional} containing the {@link UserGroupViewDto}, or empty
     *         if not found
     */
    @Transactional
    public Optional<UserGroupViewDto> findByName(final String name) {
        final var group = this.userGroupRepository.findByName(name);
        return Optional.ofNullable(group).map(UserGroupViewDto::new);
    }

    /**
     * Retrieves all users in a specific group.
     *
     * @param groupPublicId the group public ID
     * @return a list of {@link UserViewDto}s in the group
     * @throws WebApplicationException if group not found (NOT_FOUND status)
     */
    @Transactional
    public List<UserViewDto> getUsersInGroup(final String groupPublicId) {
        final UserGroupEntity group = this.userGroupRepository.findByPublicId(groupPublicId).orElse(null);
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }
        final var metas = this.userGroupMetaRepository.findByGroupPublicIdWithUsers(groupPublicId);
        return metas.stream()
                .map(meta -> new UserViewDto(meta.user))
                .toList();
    }

    /**
     * Retrieves all groups that a specific user belongs to.
     *
     * @param userPublicId the user public ID
     * @return a list of {@link UserGroupViewDto}s the user is in
     */
    @Transactional
    public List<UserGroupViewDto> getGroupsForUser(final String userPublicId) {
        final var metas = this.userGroupMetaRepository.findByUserPublicId(userPublicId);
        return metas.stream()
                .map(meta -> new UserGroupViewDto(meta.group))
                .toList();
    }

    /**
     * Creates a new user group.
     *
     * @param groupDto the group data transfer object with name
     * @return the created {@link UserGroupViewDto}
     * @throws ValidationException if name is missing or empty
     */
    @Transactional
    public UserGroupViewDto createGroup(final @Valid UserGroupDto groupDto) {
        this.permissionService.requireUserGroupAdd();

        if (groupDto.name == null || groupDto.name.isBlank()) {
            throw new ValidationException("Name is required");
        }

        final UserGroupEntity group = new UserGroupEntity();
        group.name = groupDto.name;
        this.userGroupRepository.persist(group);

        return new UserGroupViewDto(group);
    }

    /**
     * Completely replaces a user group (PUT semantics).
     *
     * @param publicId the group public ID to update
     * @param groupDto the new group data with name
     * @return the updated {@link UserGroupViewDto}
     * @throws WebApplicationException if group not found (NOT_FOUND status)
     * @throws ValidationException     if name is missing or empty
     */
    @Transactional
    public UserGroupViewDto updateGroup(final String publicId, final @Valid UserGroupDto groupDto) {
        this.permissionService.requireUserGroupEdit();

        if (groupDto.name == null || groupDto.name.isBlank()) {
            throw new ValidationException("Name is required");
        }

        final UserGroupEntity existingGroup = this.userGroupRepository.findByPublicId(publicId).orElse(null);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingGroup.name = groupDto.name;
        this.userGroupRepository.persist(existingGroup);

        return new UserGroupViewDto(existingGroup);
    }

    /**
     * Partially updates a user group (PATCH semantics).
     * Only updates group properties that are explicitly provided in the DTO; null
     * values are ignored.
     *
     * @param id       the group ID to update
     * @param groupDto the partial group data with selected fields to update
     * @return the updated {@link UserGroupViewDto}
     * @throws WebApplicationException if group not found (NOT_FOUND status)
     */
    @Transactional
    public UserGroupViewDto patchGroup(final Long id, final @Valid UserGroupDto groupDto) {
        this.permissionService.requireUserGroupEdit();

        final UserGroupEntity existingGroup = this.userGroupRepository.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (groupDto.name != null && !groupDto.name.isBlank()) {
            existingGroup.name = groupDto.name;
        }

        this.userGroupRepository.persist(existingGroup);
        return new UserGroupViewDto(existingGroup);
    }

    /**
     * Deletes a user group by public ID.
     *
     * @param publicId the group public ID to delete
     * @return {@code true} if deletion succeeded, {@code false} if group not found
     */
    @Transactional
    public boolean deleteGroup(final String publicId) {
        this.permissionService.requireUserGroupDelete();
        return this.userGroupRepository.deleteByPublicId(publicId);
    }

    /**
     * Adds a user to a group membership.
     * Creates a {@link UserGroupMetaEntity} association between user and group.
     *
     * @param userPublicId  the user public ID to add
     * @param groupPublicId the group public ID to add user to
     * @return the created {@link UserGroupMetaEntity} membership record
     * @throws WebApplicationException if user/group not found or user already in
     *                                 group (CONFLICT)
     */
    @Transactional
    public UserGroupMetaEntity addUserToGroup(final String userPublicId, final String groupPublicId) {
        this.permissionService.requireUserGroupEdit();

        final UserEntity user = this.userRepository.findByPublicId(userPublicId).orElse(null);
        final UserGroupEntity group = this.userGroupRepository.findByPublicId(groupPublicId).orElse(null);

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        final var meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;

        try {
            this.userGroupMetaRepository.persist(meta);
            this.userGroupMetaRepository.flush();
        } catch (final PersistenceException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new WebApplicationException("User is already in this group", Response.Status.CONFLICT);
            }
            throw e;
        }

        return meta;
    }

    /**
     * Removes a user from a group membership.
     *
     * @param userPublicId  the user public ID to remove
     * @param groupPublicId the group public ID to remove user from
     * @return {@code true} if removal succeeded, {@code false} if membership not
     *         found
     */
    @Transactional
    public boolean removeUserFromGroup(final String userPublicId, final String groupPublicId) {
        this.permissionService.requireUserGroupEdit();

        final var meta = this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(userPublicId, groupPublicId);
        if (meta == null) {
            return false;
        }
        this.userGroupMetaRepository.delete(meta);
        return true;
    }

    /**
     * Checks if a user is a member of a specific group.
     *
     * @param userId  the user ID
     * @param groupId the group ID
     * @return {@code true} if user is in group, {@code false} otherwise
     */
    public boolean isUserInGroup(final Long userId, final Long groupId) {
        return this.userGroupMetaRepository.isUserInGroup(userId, groupId);
    }

    /**
     * Searches groups by name using the provided query string (case-insensitive).
     * Returns all groups if query is null or empty.
     *
     * @param query the search query string (group name match)
     * @return a list of matching {@link UserGroupViewDto}s
     */
    @Transactional
    public List<UserGroupViewDto> searchGroups(final String query) {
        if (query == null || query.isBlank()) {
            return this.getAllGroups();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        return this.userGroupRepository.search(searchTerm).stream()
                .map(UserGroupViewDto::new)
                .toList();
    }
}
