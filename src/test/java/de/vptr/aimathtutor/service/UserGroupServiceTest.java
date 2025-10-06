package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.dto.UserGroupDto;
import de.vptr.aimathtutor.service.UserGroupService;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @InjectMocks
    private UserGroupService userGroupService;

    @Test
    @DisplayName("Should throw ValidationException when creating group with null name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithNullName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = null;

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with empty name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithEmptyName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "";

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with whitespace name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithWhitespaceName() {
        final UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.userGroupService.createGroup(groupDto);
        });
    }
}
