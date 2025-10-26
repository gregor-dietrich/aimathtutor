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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserGroupService {

    @Transactional
    public List<UserGroupViewDto> getAllGroups() {
        return UserGroupEntity.find("ORDER BY id DESC").list().stream()
                .map(entity -> new UserGroupViewDto((UserGroupEntity) entity))
                .toList();
    }

    @Transactional
    public Optional<UserGroupViewDto> findById(final Long id) {
        return UserGroupEntity.findByIdOptional(id)
                .map(entity -> new UserGroupViewDto((UserGroupEntity) entity));
    }

    @Transactional
    public Optional<UserGroupViewDto> findByName(final String name) {
        return Optional.ofNullable(UserGroupEntity.findByName(name))
                .map(UserGroupViewDto::new);
    }

    @Transactional
    public List<UserViewDto> getUsersInGroup(final Long groupId) {
        final UserGroupEntity group = UserGroupEntity.findById(groupId);
        if (group == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }
        return group.getUsers().stream()
                .map(UserViewDto::new)
                .toList();
    }

    @Transactional
    public List<UserGroupViewDto> getGroupsForUser(final Long userId) {
        final var metas = UserGroupMetaEntity.findByUserId(userId);
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
        group.persist();

        return new UserGroupViewDto(group);
    }

    @Transactional
    public UserGroupViewDto updateGroup(final Long id, final UserGroupDto groupDto) {
        if (groupDto.name == null || groupDto.name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        final UserGroupEntity existingGroup = UserGroupEntity.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingGroup.name = groupDto.name;
        existingGroup.persist();

        return new UserGroupViewDto(existingGroup);
    }

    @Transactional
    public UserGroupViewDto patchGroup(final Long id, final UserGroupDto groupDto) {
        final UserGroupEntity existingGroup = UserGroupEntity.findById(id);
        if (existingGroup == null) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (groupDto.name != null && !groupDto.name.trim().isEmpty()) {
            existingGroup.name = groupDto.name;
        }

        existingGroup.persist();
        return new UserGroupViewDto(existingGroup);
    }

    @Transactional
    public boolean deleteGroup(final Long id) {
        return UserGroupEntity.deleteById(id);
    }

    @Transactional
    public UserGroupMetaEntity addUserToGroup(final Long userId, final Long groupId) {
        // Check if association already exists
        if (UserGroupMetaEntity.isUserInGroup(userId, groupId)) {
            throw new WebApplicationException("User is already in this group", Response.Status.CONFLICT);
        }

        final UserEntity user = UserEntity.findById(userId);
        final UserGroupEntity group = UserGroupEntity.findById(groupId);

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
        meta.persist();

        return meta;
    }

    @Transactional
    public boolean removeUserFromGroup(final Long userId, final Long groupId) {
        final var meta = UserGroupMetaEntity.findByUserAndGroup(userId, groupId);
        if (meta == null) {
            return false;
        }
        meta.delete();
        return true;
    }

    public boolean isUserInGroup(final Long userId, final Long groupId) {
        return UserGroupMetaEntity.isUserInGroup(userId, groupId);
    }

    @Transactional
    public List<UserGroupViewDto> searchGroups(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllGroups();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        return UserGroupEntity.find("LOWER(name) LIKE ?1 ORDER BY id DESC", searchTerm)
                .stream()
                .map(entity -> new UserGroupViewDto((UserGroupEntity) entity))
                .toList();
    }
}
