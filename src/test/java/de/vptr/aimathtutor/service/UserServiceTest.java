package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class UserServiceTest {

    @Inject
    private UserService userService;

    @Test
    @DisplayName("Should throw ValidationException when creating user with null username")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingUserWithNullUsername() {
        final UserDto userDto = new UserDto();
        userDto.username = null;
        userDto.password = "password";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            this.userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with empty username")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingUserWithEmptyUsername() {
        final UserDto userDto = new UserDto();
        userDto.username = "";
        userDto.password = "password";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            this.userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with null password")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingUserWithNullPassword() {
        final UserDto userDto = new UserDto();
        userDto.username = "testuser";
        userDto.password = null;
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            this.userService.createUser(userDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating user with empty password")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingUserWithEmptyPassword() {
        final UserDto userDto = new UserDto();
        userDto.username = "testuser";
        userDto.password = "";
        userDto.email = "test@example.com";

        assertThrows(ValidationException.class, () -> {
            this.userService.createUser(userDto);
        });
    }
}
