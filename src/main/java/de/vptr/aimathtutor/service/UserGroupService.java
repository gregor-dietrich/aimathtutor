package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserGroupService {

    @Inject
    UserGroupRepository userGroupRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserGroupMetaRepository userGroupMetaRepository;

    @Transactional
    public List<UserGroupViewDto> getAllGroups() {
        return this.userGroupRepository.findAll().stream()
                .map(UserGroupViewDto::new)
                .toList();
    }

    @Transactional
    public Optional<UserGroupViewDto> findById(final Long id) {
        return this.userGroupRepository.findByIdOptional(id)
                .map(UserGroupViewDto::new);
    }

    @Transactional
    public Optional<UserGroupViewDto> findByName(final String name) {
        final var group = this.userGroupRepository.findByName(name);
        return Optional.ofNullable(group).map(UserGroupViewDto::new);
    }

    @Transactional
    public List<UserViewDto> getUsersInGroup(final Long groupId) {
        final UserGroupEntity group = this.userGroupRepository.findById(groupId);
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }
        return group.getUsers().stream()
                .map(UserViewDto::new)
                .toList();
    }

    @Transactional
    public List<UserGroupViewDto> getGroupsForUser(final Long userId) {
        final var metas = this.userGroupMetaRepository.findByUserId(userId);
        return metas.stream()
                .map(meta -> new UserGroupViewDto(meta.group))
                .toList();
    }

    @Transactional
    public UserGroupViewDto createGroup(final UserGroupDto groupDto) {
        if (groupDto.name == null || groupDto.name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        final UserGroupEntity group = new UserGroupEntity();
        group.name = groupDto.name;
        this.userGroupRepository.persist(group);

        return new UserGroupViewDto(group);
    }

    @Transactional
    public UserGroupViewDto updateGroup(final Long id, final UserGroupDto groupDto) {
        if (groupDto.name == null || groupDto.name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        final UserGroupEntity existingGroup = this.userGroupRepository.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingGroup.name = groupDto.name;
        this.userGroupRepository.persist(existingGroup);

        return new UserGroupViewDto(existingGroup);
    }

    @Transactional
    public UserGroupViewDto patchGroup(final Long id, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = this.userGroupRepository.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (groupDto.name != null && !groupDto.name.trim().isEmpty()) {
            existingGroup.name = groupDto.name;
        }

        this.userGroupRepository.persist(existingGroup);
        return new UserGroupViewDto(existingGroup);
    }

    @Transactional
    public boolean deleteGroup(final Long id) {
        return this.userGroupRepository.deleteById(id);
    }

    @Transactional
    public UserGroupMetaEntity addUserToGroup(final Long userId, final Long groupId) {
        // Check if association already exists
        if (this.userGroupMetaRepository.isUserInGroup(userId, groupId)) {
            throw new WebApplicationException("User is already in this group", Response.Status.CONFLICT);
        }

        final UserEntity user = this.userRepository.findById(userId);
        final UserGroupEntity group = this.userGroupRepository.findById(groupId);

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        final var meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;
        meta.timestamp = LocalDateTime.now();
        this.userGroupMetaRepository.persist(meta);

        return meta;
    }

    @Transactional
    public boolean removeUserFromGroup(final Long userId, final Long groupId) {
        final var meta = this.userGroupMetaRepository.findByUserAndGroup(userId, groupId);
        if (meta == null) {
            return false;
        }
        this.userGroupMetaRepository.delete(meta);
        return true;
    }

    public boolean isUserInGroup(final Long userId, final Long groupId) {
        return this.userGroupMetaRepository.isUserInGroup(userId, groupId);
    }

    @Transactional
    public List<UserGroupViewDto> searchGroups(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllGroups();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        return this.userGroupRepository.search(searchTerm).stream()
                .map(UserGroupViewDto::new)
                .toList();
    }
}
