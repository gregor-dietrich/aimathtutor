package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@QuarkusTest
class UserServiceTest {

    private static final String VALID_PASSWORD = "P@ssw0rd1";

    @Inject
    private UserService userService;

    @Inject
    private UserRepository userRepository;

    private UserDto buildValidDto() {
        final var dto = new UserDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.username = "user_" + suffix;
        dto.password = VALID_PASSWORD;
        dto.email = "user_" + suffix + "@example.com";
        return dto;
    }

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

    @Test
    @DisplayName("Should reject password missing complexity requirements")
    @Transactional
    void shouldRejectPasswordMissingComplexity() {
        final UserDto userDto = this.buildValidDto();
        userDto.password = "alllowercase1";
        assertThrows(ValidationException.class, () -> this.userService.createUser(userDto));
    }

    @Test
    @DisplayName("Should create user with valid data")
    @TestTransaction
    void shouldCreateUserWithValidData() {
        final UserDto dto = this.buildValidDto();

        final UserViewDto created = this.userService.createUser(dto);

        assertNotNull(created);
        assertNotNull(created.id);
        assertEquals(dto.username, created.username);
        assertEquals(dto.email, created.email);
        assertNotNull(created.rankId);
    }

    @Test
    @DisplayName("Should find user by id after creating")
    @TestTransaction
    void shouldFindUserById() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final var found = this.userService.findById(created.id);

        assertTrue(found.isPresent());
        assertEquals(dto.username, found.get().username);
    }

    @Test
    @DisplayName("Should find seeded admin user by username")
    @TestTransaction
    void shouldFindSeededUserByUsername() {
        final var found = this.userService.findByUsername("admin");
        assertTrue(found.isPresent(), "Seeded admin user should exist");
        assertEquals("admin", found.get().username);
    }

    @Test
    @DisplayName("Should reject duplicate username")
    @TestTransaction
    void shouldRejectDuplicateUsername() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);

        final UserDto duplicate = this.buildValidDto();
        duplicate.username = first.username;

        assertThrows(ValidationException.class, () -> this.userService.createUser(duplicate));
    }

    @Test
    @DisplayName("Should reject duplicate email")
    @TestTransaction
    void shouldRejectDuplicateEmail() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);

        final UserDto duplicate = this.buildValidDto();
        duplicate.email = first.email;

        assertThrows(ValidationException.class, () -> this.userService.createUser(duplicate));
    }

    @Test
    @DisplayName("Should hash password on create rather than store plaintext")
    @TestTransaction
    void shouldHashPasswordOnCreate() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserEntity entity = this.userRepository.findById(created.id);
        assertNotNull(entity);
        assertNotNull(entity.password);
        assertTrue(entity.password.startsWith("$2"), "Password should be a bcrypt hash, was: " + entity.password);
        assertFalse(VALID_PASSWORD.equals(entity.password));
    }

    @Test
    @DisplayName("Should return all users including seeded accounts")
    @TestTransaction
    void shouldGetAllUsersIncludingSeeded() {
        final var users = this.userService.getAllUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 4, "Expected ≥4 seeded users, got " + users.size());
    }
}
