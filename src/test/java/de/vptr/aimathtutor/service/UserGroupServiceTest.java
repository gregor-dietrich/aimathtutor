package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserGroupDto;
import de.vptr.aimathtutor.dto.UserGroupViewDto;
import de.vptr.aimathtutor.repository.UserGroupRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class UserGroupServiceTest {

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserGroupRepository userGroupRepository;

    @Inject
    private UserRepository userRepository;

    @InjectMock
    private PermissionService permissionService;

    private UserGroupDto buildDto() {
        final var dto = new UserGroupDto();
        dto.name = "group_" + UUID.randomUUID().toString().substring(0, 8);
        return dto;
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with null name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingGroupWithNullName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = null;

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with empty name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingGroupWithEmptyName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "";

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with whitespace name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingGroupWithWhitespaceName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should create group with valid name")
    @TestTransaction
    void shouldCreateGroupWithValidName() {
        final UserGroupDto dto = this.buildDto();

        final UserGroupViewDto created = this.userGroupService.createGroup(dto);

        assertNotNull(created);
        assertNotNull(created.publicId);
        assertEquals(dto.name, created.name);
    }

    @Test
    @DisplayName("Should find group by id")
    @TestTransaction
    void shouldFindGroupById() {
        final UserGroupViewDto created = this.userGroupService.createGroup(this.buildDto());
        final var groupEntity = this.userGroupRepository.findByPublicId(created.publicId).orElseThrow();

        final var found = this.userGroupService.findById(groupEntity.id);

        assertTrue(found.isPresent());
        assertEquals(created.name, found.get().name);
    }

    @Test
    @DisplayName("Should find group by name")
    @TestTransaction
    void shouldFindGroupByName() {
        final UserGroupViewDto created = this.userGroupService.createGroup(this.buildDto());

        final var found = this.userGroupService.findByName(created.name);

        assertTrue(found.isPresent());
        assertEquals(created.publicId, found.get().publicId);
    }

    @Test
    @DisplayName("Should add user to group and list them")
    @TestTransaction
    void shouldAddUserToGroupAndListMembership() {
        final UserGroupViewDto group = this.userGroupService.createGroup(this.buildDto());
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "Seeded student1 should exist");

        this.userGroupService.addUserToGroup(student.publicId, group.publicId);

        final var groupEntity = this.userGroupRepository.findByPublicId(group.publicId).orElseThrow();
        final var members = this.userGroupService.getUsersInGroup(group.publicId);
        assertEquals(1, members.size());
        assertEquals("student1", members.get(0).username);
        assertTrue(this.userGroupService.isUserInGroup(student.id, groupEntity.id));
    }

    @Test
    @DisplayName("Should remove user from group")
    @TestTransaction
    void shouldRemoveUserFromGroup() {
        final UserGroupViewDto group = this.userGroupService.createGroup(this.buildDto());
        final var student = this.userRepository.findByUsername("student1");
        this.userGroupService.addUserToGroup(student.publicId, group.publicId);

        final boolean removed = this.userGroupService.removeUserFromGroup(student.publicId, group.publicId);

        assertTrue(removed);
        final var groupEntity = this.userGroupRepository.findByPublicId(group.publicId).orElseThrow();
        assertFalse(this.userGroupService.isUserInGroup(student.id, groupEntity.id));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND for unknown group")
    @TestTransaction
    void shouldThrowNotFoundForUnknownGroup() {
        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.userGroupService.getUsersInGroup("00000000000000000000000000"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should delete group by id")
    @TestTransaction
    void shouldDeleteGroup() {
        final UserGroupViewDto group = this.userGroupService.createGroup(this.buildDto());

        final boolean deleted = this.userGroupService.deleteGroup(group.publicId);
        final var groupEntity = this.userGroupRepository.findByPublicId(group.publicId).orElse(null);

        assertTrue(deleted);
        assertNull(groupEntity);
    }
}
