package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import de.vptr.aimathtutor.security.PasswordHashingService;
import io.quarkus.test.InjectMock;
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

    @Inject
    private PasswordHashingService passwordHashingService;

    @InjectMock
    private PermissionService permissionService;

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
        assertNotNull(created.publicId);
        assertEquals(dto.username, created.username);
        assertEquals(dto.email, created.email);
        assertNotNull(created.rankPublicId);
    }

    @Test
    @DisplayName("Should find user by id after creating")
    @TestTransaction
    void shouldFindUserById() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var userEntity = this.userRepository.findByPublicId(created.publicId).orElseThrow();

        final var found = this.userService.findById(userEntity.id);

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

        final UserEntity entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();
        assertNotNull(entity);
        assertNotNull(entity.password);
        assertTrue(entity.password.startsWith("$2"), "Password should be a bcrypt hash, was: " + entity.password);
        assertNotEquals(VALID_PASSWORD, entity.password);
    }

    @Test
    @DisplayName("Should return all users including seeded accounts")
    @TestTransaction
    void shouldGetAllUsersIncludingSeeded() {
        final var users = this.userService.getAllUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 4, "Expected ≥4 seeded users, got " + users.size());
    }

    @Test
    @DisplayName("patchUser updates only the provided field")
    @TestTransaction
    void testPatchUser_updatesProvidedField() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserDto patch = new UserDto();
        patch.email = "patched_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        final UserViewDto patched = this.userService.patchUser(created.publicId, patch);

        assertEquals(dto.username, patched.username, "Username should be unchanged after patch");
        assertEquals(patch.email, patched.email);
    }

    @Test
    @DisplayName("searchUsers with blank query returns all users")
    @TestTransaction
    void testSearchUsers_blank() {
        final var results = this.userService.searchUsers("");
        assertNotNull(results);
        assertTrue(results.size() >= 4, "Blank search should return all users");
    }

    @Test
    @DisplayName("searchUsers with username prefix returns matching users")
    @TestTransaction
    void testSearchUsers_byUsername() {
        final var results = this.userService.searchUsers("admin");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(u -> "admin".equals(u.username)));
    }

    @Test
    @DisplayName("searchUsers with non-matching term returns empty list")
    @TestTransaction
    void testSearchUsers_noMatch() {
        final var results = this.userService.searchUsers("zzz_no_match_xyz_999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findActiveUsers returns non-empty list of activated users")
    @TestTransaction
    void testFindActiveUsers() {
        final var activeUsers = this.userService.findActiveUsers();
        assertNotNull(activeUsers);
        assertFalse(activeUsers.isEmpty(), "Seeded activated users should be in the active list");
    }

    @Test
    @DisplayName("findByEmail returns user with matching email")
    @TestTransaction
    void testFindByEmail_found() {
        final UserDto dto = this.buildValidDto();
        this.userService.createUser(dto);

        final var found = this.userService.findByEmail(dto.email);
        assertTrue(found.isPresent());
        assertEquals(dto.username, found.get().username);
    }

    @Test
    @DisplayName("findByEmail returns empty for unknown email")
    @TestTransaction
    void testFindByEmail_notFound() {
        final var found = this.userService.findByEmail("nobody@nowheredomain.invalid");
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("changePassword succeeds with correct current password")
    @TestTransaction
    void testChangePassword_success() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();
        final String originalHash = entity.password;
        final String newPassword = "N3wP@ssword!";

        this.userService.changePassword(entity.id, VALID_PASSWORD, newPassword);

        final var updated = this.userRepository.findById(entity.id);
        assertNotNull(updated);
        assertTrue(updated.password.startsWith("$2"), "Password should still be a bcrypt hash");
        assertFalse(originalHash.equals(updated.password), "Password hash should have changed");
        assertTrue(this.passwordHashingService.verifyPassword(newPassword, updated.password),
                "New password should verify against stored hash");
    }

    @Test
    @DisplayName("changePassword throws ValidationException for wrong current password")
    @TestTransaction
    void testChangePassword_wrongCurrentPassword() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();

        assertThrows(ValidationException.class,
                () -> this.userService.changePassword(entity.id, "WrongP@ss1", "N3wP@ssword!"));
    }

    @Test
    @DisplayName("updateAvatars persists emoji values")
    @TestTransaction
    void testUpdateAvatars_success() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();

        this.userService.updateAvatars(entity.id, "🧑", "🤖");

        final var updated = this.userRepository.findById(entity.id);
        assertNotNull(updated);
        assertEquals("🧑", updated.userAvatarEmoji);
        assertEquals("🤖", updated.tutorAvatarEmoji);
    }

    @Test
    @DisplayName("updateAvatars throws ValidationException for blank user emoji")
    @TestTransaction
    void testUpdateAvatars_blankUserEmoji() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();

        assertThrows(ValidationException.class,
                () -> this.userService.updateAvatars(entity.id, "", "🤖"));
    }

    @Test
    @DisplayName("getSettings returns non-null DTO with defaults")
    @TestTransaction
    void testGetSettings_returnsDefaults() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();

        final var settings = this.userService.getSettings(entity.id);
        assertNotNull(settings);
        assertNotNull(settings.userAvatarEmoji);
        assertNotNull(settings.tutorAvatarEmoji);
    }

    @Test
    @DisplayName("findByPublicId returns empty for unknown publicId")
    @TestTransaction
    void testFindByPublicId_notFound() {
        final var result = this.userService.findByPublicId("00000000000000000000000000");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("deleteUser succeeds and makes the user unfindable")
    @TestTransaction
    void testDeleteUser_success() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final boolean deleted = this.userService.deleteUser(created.publicId);

        assertTrue(deleted);
        assertFalse(this.userService.findByPublicId(created.publicId).isPresent(),
                "User should not be findable after deletion");
    }

    @Test
    @DisplayName("deleteUser returns false for unknown publicId")
    @TestTransaction
    void testDeleteUser_notFound() {
        final boolean deleted = this.userService.deleteUser("00000000000000000000000000");
        assertFalse(deleted);
    }

    @Test
    @DisplayName("updateUser replaces username and email")
    @TestTransaction
    void testUpdateUser_replacesFields() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserDto update = new UserDto();
        final String newSuffix = UUID.randomUUID().toString().substring(0, 8);
        update.username = "updated_" + newSuffix;
        update.email = "updated_" + newSuffix + "@example.com";
        update.password = VALID_PASSWORD;
        update.activated = true;

        final UserViewDto updated = this.userService.updateUser(created.publicId, update);

        assertEquals(update.username, updated.username);
        assertEquals(update.email, updated.email);
        assertTrue(updated.activated);
    }
}
