package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PasswordHashingService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
@SuppressWarnings({ "NullAway", "PMD.TooManyMethods" })
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

    @BeforeEach
    void setUpPermissionService() {
        Mockito.doNothing().when(this.permissionService).requireUserAdd();
        Mockito.doNothing().when(this.permissionService).requireUserEdit();
        Mockito.doNothing().when(this.permissionService).requireUserDelete();
    }

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
        final var entity = this.createAndFetchUser();

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
        final var entity = this.createAndFetchUser();

        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, "", "🤖"));
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

    @Test
    @DisplayName("updateUser throws WebApplicationException for unknown publicId")
    @TestTransaction
    void testUpdateUser_notFound() {
        final UserDto update = new UserDto();
        update.username = "nobody";
        update.password = VALID_PASSWORD;

        assertThrows(WebApplicationException.class,
                () -> this.userService.updateUser("00000000000000000000000000", update));
    }

    @Test
    @DisplayName("updateUser throws ValidationException for duplicate username")
    @TestTransaction
    void testUpdateUser_duplicateUsername() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);

        final UserDto second = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(second);

        final UserDto update = new UserDto();
        update.username = first.username;
        update.password = VALID_PASSWORD;

        assertThrows(ValidationException.class, () -> this.userService.updateUser(created.publicId, update));
    }

    @Test
    @DisplayName("patchUser throws WebApplicationException for unknown publicId")
    @TestTransaction
    void testPatchUser_notFound() {
        final UserDto patch = new UserDto();
        patch.email = "test@example.com";

        assertThrows(WebApplicationException.class,
                () -> this.userService.patchUser("00000000000000000000000000", patch));
    }

    @Test
    @DisplayName("patchUser with null email is handled gracefully")
    @TestTransaction
    void testPatchUser_nullEmail() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserDto patch = new UserDto();
        patch.email = null;

        final UserViewDto patched = this.userService.patchUser(created.publicId, patch);
        assertEquals(dto.email, patched.email, "Email should remain unchanged when patch has null email");
    }

    @Test
    @DisplayName("changePassword throws WebApplicationException for unknown user")
    @TestTransaction
    void testChangePassword_userNotFound() {
        assertThrows(WebApplicationException.class,
                () -> this.userService.changePassword(-999L, "old", "N3wP@ssword!"));
    }

    @Test
    @DisplayName("updateAvatars throws ValidationException for too long emoji")
    @TestTransaction
    void testUpdateAvatars_tooLongEmoji() {
        final var entity = this.createAndFetchUser();

        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, "a".repeat(11), "🤖"));
        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, "🧑", "a".repeat(11)));
    }

    @Test
    @DisplayName("getSettings throws WebApplicationException for unknown user id")
    @TestTransaction
    void testGetSettings_notFound() {
        assertThrows(WebApplicationException.class, () -> this.userService.getSettings(-999L));
    }

    @Test
    @DisplayName("findByPublicId returns user when found")
    @TestTransaction
    void testFindByPublicId_found() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final var found = this.userService.findByPublicId(created.publicId);
        assertTrue(found.isPresent());
        assertEquals(dto.username, found.get().username);
    }

    @Test
    @DisplayName("createUser with blank password throws ValidationException")
    @TestTransaction
    void testCreateUser_blankPassword() {
        final UserDto userDto = this.buildValidDto();
        userDto.password = "   ";

        assertThrows(ValidationException.class, () -> this.userService.createUser(userDto));
    }

    @Test
    @DisplayName("createUser with password below minimum length throws ValidationException")
    @TestTransaction
    void testCreateUser_shortPassword() {
        final UserDto userDto = this.buildValidDto();
        userDto.password = "Ab1!";

        assertThrows(ValidationException.class, () -> this.userService.createUser(userDto));
    }

    @Test
    @DisplayName("createUser with missing uppercase throws ValidationException")
    @TestTransaction
    void testCreateUser_passwordMissingUppercase() {
        final UserDto userDto = this.buildValidDto();
        userDto.password = "lowercase1!";

        assertThrows(ValidationException.class, () -> this.userService.createUser(userDto));
    }

    @Test
    @DisplayName("createUser with missing special char throws ValidationException")
    @TestTransaction
    void testCreateUser_passwordMissingSpecialChar() {
        final UserDto userDto = this.buildValidDto();
        userDto.password = "Lowercas3";

        assertThrows(ValidationException.class, () -> this.userService.createUser(userDto));
    }

    @Test
    @DisplayName("getCurrentUser throws UNAUTHORIZED when no active session exists")
    void testGetCurrentUser_noSession_throwsUnauthorized() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedSession.when(VaadinSession::getCurrent).thenReturn(null);
            final var ex = assertThrows(WebApplicationException.class, () -> this.userService.getCurrentUser());
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("getCurrentUser throws UNAUTHORIZED when session has no username attribute")
    void testGetCurrentUser_nullUsername_throwsUnauthorized() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USERNAME)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            final var ex = assertThrows(WebApplicationException.class, () -> this.userService.getCurrentUser());
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("getCurrentUser returns user DTO when session has valid username")
    void testGetCurrentUser_validSession_returnsUser() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USERNAME)).thenReturn("admin");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            final UserViewDto result = this.userService.getCurrentUser();
            assertNotNull(result);
            assertEquals("admin", result.username);
        }
    }

    @Test
    @DisplayName("getCurrentUser throws NOT_FOUND when session username has no matching user")
    void testGetCurrentUser_userNotFound_throwsNotFound() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USERNAME)).thenReturn("no_such_user_xyz999");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            final var ex = assertThrows(WebApplicationException.class, () -> this.userService.getCurrentUser());
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("updateUser throws ValidationException when new email is already in use")
    @TestTransaction
    void testUpdateUser_duplicateEmail_throwsValidationException() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);
        final UserDto second = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(second);

        final UserDto update = new UserDto();
        update.username = second.username;
        update.email = first.email;
        update.password = VALID_PASSWORD;

        assertThrows(ValidationException.class, () -> this.userService.updateUser(created.publicId, update));
    }

    @Test
    @DisplayName("patchUser throws ValidationException when new username is already taken")
    @TestTransaction
    void testPatchUser_duplicateUsername_throwsValidationException() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);
        final UserDto second = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(second);

        final UserDto patch = new UserDto();
        patch.username = first.username;

        assertThrows(ValidationException.class, () -> this.userService.patchUser(created.publicId, patch));
    }

    @Test
    @DisplayName("patchUser throws ValidationException when new email is already in use")
    @TestTransaction
    void testPatchUser_duplicateEmail_throwsValidationException() {
        final UserDto first = this.buildValidDto();
        this.userService.createUser(first);
        final UserDto second = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(second);

        final UserDto patch = new UserDto();
        patch.email = first.email;

        assertThrows(ValidationException.class, () -> this.userService.patchUser(created.publicId, patch));
    }

    @Test
    @DisplayName("patchUser with blank email normalizes to null and clears the stored email")
    @TestTransaction
    void testPatchUser_blankEmail_normalizesToNull() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserDto patch = new UserDto();
        patch.email = "";

        final UserViewDto patched = this.userService.patchUser(created.publicId, patch);
        assertNull(patched.email);
    }

    @Test
    @DisplayName("createUser with explicit banned and activated applies those values")
    @TestTransaction
    void testCreateUser_withExplicitBooleans_setBannedAndActivated() {
        final UserDto dto = this.buildValidDto();
        dto.banned = true;
        dto.activated = true;

        final UserViewDto created = this.userService.createUser(dto);
        assertNotNull(created);
        assertTrue(created.activated != null && created.activated);
        assertTrue(created.banned != null && created.banned);

        final var saved = this.userRepository.findByPublicId(created.publicId).orElseThrow();
        assertTrue(saved.activated);
        assertTrue(saved.banned);
        assertNotNull(saved.activationKey);
        assertFalse(saved.activationKey.isBlank());
    }

    @Test
    @DisplayName("createUser with unknown rankPublicId throws ValidationException")
    @TestTransaction
    void testCreateUser_unknownRankPublicId_throwsValidationException() {
        final UserDto dto = this.buildValidDto();
        dto.rankPublicId = "00000000000000000000000000";
        assertThrows(ValidationException.class, () -> this.userService.createUser(dto));
    }

    @Test
    @DisplayName("createUser with password exceeding maximum length throws ValidationException")
    @TestTransaction
    void testCreateUser_passwordTooLong_throwsValidationException() {
        final UserDto dto = this.buildValidDto();
        dto.password = "A1!" + "a".repeat(100);
        assertThrows(ValidationException.class, () -> this.userService.createUser(dto));
    }

    @Test
    @DisplayName("updateAvatars throws ValidationException for blank tutor emoji")
    @TestTransaction
    void testUpdateAvatars_blankTutorEmoji_throwsValidationException() {
        final var entity = this.createAndFetchUser();
        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, "🧑", ""));
    }

    @Test
    @DisplayName("updateAvatars throws ValidationException for null user emoji")
    @TestTransaction
    void testUpdateAvatars_nullUserEmoji_throwsValidationException() {
        final var entity = this.createAndFetchUser();
        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, null, "🤖"));
    }

    @Test
    @DisplayName("updateAvatars throws ValidationException for null tutor emoji")
    @TestTransaction
    void testUpdateAvatars_nullTutorEmoji_throwsValidationException() {
        final var entity = this.createAndFetchUser();
        assertThrows(ValidationException.class, () -> this.userService.updateAvatars(entity.id, "🧑", null));
    }

    @Test
    @DisplayName("patchUser with invalid rankPublicId throws ValidationException")
    @TestTransaction
    void testPatchUser_invalidRankPublicId_throwsValidationException() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);

        final UserDto patch = new UserDto();
        patch.rankPublicId = "00000000000000000000000000";

        assertThrows(ValidationException.class, () -> this.userService.patchUser(created.publicId, patch));
    }

    private UserEntity createAndFetchUser() {
        final UserDto dto = this.buildValidDto();
        final UserViewDto created = this.userService.createUser(dto);
        return this.userRepository.findByPublicId(created.publicId).orElseThrow();
    }
}
