package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.dto.UserGroupDto;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @InjectMocks
    private UserGroupService userGroupService;

    @Test
    @DisplayName("Should throw ValidationException when creating group with null name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithNullName() {
        UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = null;

        assertThrows(ValidationException.class, () -> {
            userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with empty name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithEmptyName() {
        UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "";

        assertThrows(ValidationException.class, () -> {
            userGroupService.createGroup(groupDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating group with whitespace name")
    void shouldThrowValidationExceptionWhenCreatingGroupWithWhitespaceName() {
        UserGroupDto groupDto = new UserGroupDto();
        groupDto.name = "   ";

        assertThrows(ValidationException.class, () -> {
            userGroupService.createGroup(groupDto);
        });
    }
}
