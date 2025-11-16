package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserGroupDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class UserGroupServiceTest {

    @Inject
    private UserGroupService userGroupService;

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
}
