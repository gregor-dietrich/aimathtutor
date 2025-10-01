package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.dto.UserDto;
import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordHashingService passwordHashingService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should throw ValidationException when creating user with null username")
    void shouldThrowValidationExceptionWhenCreatingUserWithNullUsername() {
        UserDto userDto = new UserDto();
        userDto.username = null;
        userDto.password = "password";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with empty username")
    void shouldThrowValidationExceptionWhenCreatingUserWithEmptyUsername() {
        UserDto userDto = new UserDto();
        userDto.username = "";
        userDto.password = "password";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with null password")
    void shouldThrowValidationExceptionWhenCreatingUserWithNullPassword() {
        UserDto userDto = new UserDto();
        userDto.username = "testuser";
        userDto.password = null;
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with empty password")
    void shouldThrowValidationExceptionWhenCreatingUserWithEmptyPassword() {
        UserDto userDto = new UserDto();
        userDto.username = "testuser";
        userDto.password = "";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            userService.createUser(userDto);
        });
    }
}
