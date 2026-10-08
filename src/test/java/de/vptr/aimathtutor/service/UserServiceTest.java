package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PasswordHashingService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestUserRankFactory;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
@SuppressWarnings({ "NullAway", "PMD.TooManyMethods" })
class UserServiceTest {

    private static final String VALID_PASSWORD = "P@ssw0rd1";
    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String TEACHER_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAW";
    private static final String STUDENT_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAX";
    // No rank has this ID, like a rank deleted while an admin's edit dialog still offered it
    private static final String UNKNOWN_RANK_PUBLIC_ID = "00000000000000000000000000";

    @Inject
    private UserService userService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserRankService userRankService;

    @Inject
    private UserRankRepository userRankRepository;

    @InjectSpy
    private PasswordHashingService passwordHashingService;

    @Inject
    private AuthService authService;

    @InjectMock
    private PermissionService permissionService;

    @BeforeEach
    @Transactional
    void setUpPermissionService() {
        Mockito.doNothing().when(this.permissionService).requireUserAdd();
        Mockito.doNothing().when(this.permissionService).requireUserEdit();
        Mockito.doNothing().when(this.permissionService).requireUserDelete();
        // The caller is an administrator unless a test says otherwise, so the privilege ceiling never applies
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    private UserDto buildValidDto() {
        final var dto = new UserDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.username = "user_" + suffix;
        dto.password = VALID_PASSWORD;
        dto.email = "user_" + suffix + "@example.com";
        dto.rankPublicId = STUDENT_RANK_PUBLIC_ID;
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
        assertEquals(STUDENT_RANK_PUBLIC_ID, created.rankPublicId);
    }

    @ParameterizedTest(name = "{0} with {1} rank")
    @CsvSource({ "createUser, missing, true", "createUser, unknown, true", "updateUser, missing, true",
            "updateUser, unknown, true", "patchUser, missing, false", "patchUser, unknown, true" })
    @DisplayName("No create or update path falls back to the Admin rank")
    @TestTransaction
    void noWritePathYieldsAdminRank(final String method, final String rank, final boolean rejected) {
        final String rankPublicId = "missing".equals(rank) ? null : UNKNOWN_RANK_PUBLIC_ID;
        final UserDto dto = this.buildValidDto();
        if ("createUser".equals(method)) {
            dto.rankPublicId = rankPublicId;
            assertThrows(ValidationException.class, () -> this.userService.createUser(dto));
            assertTrue(this.userRepository.findByUsernameOptional(dto.username).isEmpty());
            return;
        }
        final UserViewDto student = this.userService.createUser(dto);
        final UserDto change = new UserDto();
        change.username = student.username;
        change.rankPublicId = rankPublicId;
        final Executable write =
                "updateUser".equals(method) ? () -> this.userService.updateUser(student.publicId, change)
                        : () -> this.userService.patchUser(student.publicId, change);
        if (rejected) {
            assertThrows(ValidationException.class, write);
        } else {
            assertDoesNotThrow(write);
        }
        assertEquals(STUDENT_RANK_PUBLIC_ID,
                this.userRepository.findByPublicId(student.publicId).orElseThrow().rank.publicId);
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
    @DisplayName("searchUsers with an email-like term containing '!' does not fail the query")
    @TestTransaction
    void testSearchUsers_emailWithEscapeChar() {
        // "@"-containing terms go to the blind-index equality lookup, which must receive the raw term;
        // a '!' in the local-part must neither fail the query nor be escaped away.
        final var results = assertDoesNotThrow(() -> this.userService.searchUsers("foo!bar@example.com"));
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("searchUsers finds a user by exact email containing LIKE metacharacters")
    @TestTransaction
    void testSearchUsers_byEmailWithUnderscore() {
        // Regression: the email branch previously received a LIKE-escaped term (john!_doe@...), which
        // corrupted the blind-index lookup so emails containing '_', '!' or '%' were unfindable.
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        final UserDto dto = this.buildValidDto();
        dto.email = "john_doe_" + suffix + "@example.com";
        this.userService.createUser(dto);

        final var results = this.userService.searchUsers(dto.email);
        assertEquals(1, results.size(), "Exact email search should find the user");
        assertEquals(dto.username, results.get(0).username);
    }

    @Test
    @DisplayName("createUser normalizes mixed-case usernames to lower case")
    @TestTransaction
    void testCreateUser_normalizesUsernameCase() {
        // AuthService.authenticate lower-cases the login name before its exact-match lookup, so
        // usernames must be stored lower-cased or the account could never log in.
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        final UserDto dto = this.buildValidDto();
        dto.username = "  MixedCase_" + suffix + "  ";

        final UserViewDto created = this.userService.createUser(dto);

        assertEquals("mixedcase_" + suffix, created.username);
    }

    @Test
    @DisplayName("createUser rejects duplicate usernames regardless of case")
    @TestTransaction
    void testCreateUser_duplicateUsernameCaseInsensitive() {
        final UserDto dto = this.buildValidDto();
        this.userService.createUser(dto);

        final UserDto duplicate = this.buildValidDto();
        duplicate.username = dto.username.toUpperCase(Locale.ROOT);

        assertThrows(ValidationException.class, () -> this.userService.createUser(duplicate));
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
        dto.activated = true;
        final UserViewDto created = this.userService.createUser(dto);
        final var entity = this.userRepository.findByPublicId(created.publicId).orElseThrow();
        final String originalHash = entity.password;
        final String newPassword = "N3wP@ssword!";

        this.userService.changePassword(credentialsFor(entity), VALID_PASSWORD, newPassword);

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
                () -> this.userService.changePassword(credentialsFor(entity), "WrongP@ss1", "N3wP@ssword!"));
    }

    @Test
    @DisplayName("changePassword rejects a 73-character password with the 72-character limit")
    @TestTransaction
    void testChangePassword_73Characters_namesLimit() {
        final var entity = this.createAndFetchUser();

        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentialsFor(entity), VALID_PASSWORD, "A1!" + "a".repeat(70)));
        assertEquals("Password must be between 8 and 72 characters", e.getMessage());
    }

    @Test
    @DisplayName("changePassword rejects a multi-byte password under 72 characters but over 72 bytes")
    @TestTransaction
    void testChangePassword_over72Bytes_rejected() {
        final var entity = this.createAndFetchUser();

        // 38 characters, 73 UTF-8 bytes
        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentialsFor(entity), VALID_PASSWORD, "A1!" + "ä".repeat(35)));
        assertTrue(e.getMessage().startsWith("Password must not exceed 72 bytes"), e.getMessage());
    }

    @Test
    @DisplayName("changePassword returns the credential stamp of the new hash")
    @TestTransaction
    void testChangePassword_returnsStamp() {
        final var entity = this.createAndFetchUser();

        final var stamp = this.userService.changePassword(credentialsFor(entity), VALID_PASSWORD, "N3wP@ssword!");

        assertEquals(AuthService.credentialStamp(entity.password), stamp);
    }

    private static SessionCredentials credentialsFor(final UserEntity user) {
        return new SessionCredentials(user.publicId, AuthService.credentialStamp(user.password),
                UUID.randomUUID().toString());
    }

    private void failToChangePassword(final SessionCredentials credentials) {
        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentials, "WrongP@ss1", "N3wP@ssword!"));
        assertEquals("Current password is incorrect", e.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "stale stamp", "banned", "deactivated" })
    @DisplayName("changePassword refuses a revoked session before checking any password")
    @TestTransaction
    void testChangePassword_refusesRevokedSession(final String revocation) {
        final var entity = this.createAndFetchUser();
        final var credentials = credentialsFor(entity);
        switch (revocation) {
            case "banned" -> entity.banned = true;
            case "deactivated" -> entity.activated = false;
            default -> entity.password = this.passwordHashingService.hashPassword(VALID_PASSWORD);
        }
        Mockito.clearInvocations(this.passwordHashingService);

        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentials, VALID_PASSWORD, "N3wP@ssword!"));

        assertEquals("Your session has ended. Please sign in again.", e.getMessage());
        verify(this.passwordHashingService, never()).verifyPassword(any(), any());
    }

    @Test
    @DisplayName("changePassword refuses credentials of an unknown user")
    @TestTransaction
    void testChangePassword_unknownUser() {
        final var credentials = new SessionCredentials("no-such-public-id", "stamp", UUID.randomUUID().toString());
        assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentials, "old", "N3wP@ssword!"));
    }

    @Test
    @DisplayName("changePassword refuses the 6th attempt after 5 wrong guesses without checking the password")
    @TestTransaction
    void testChangePassword_throttledAfterFiveWrongGuesses() {
        final var credentials = credentialsFor(this.createAndFetchUser());
        for (int i = 0; i < 5; i++) {
            this.failToChangePassword(credentials);
        }
        verify(this.passwordHashingService, times(5)).verifyPassword(eq("WrongP@ss1"), any());

        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(credentials, VALID_PASSWORD, "N3wP@ssword!"));

        assertEquals("Too many failed attempts. Try again later.", e.getMessage());
        verify(this.passwordHashingService, never()).verifyPassword(eq(VALID_PASSWORD), any());
    }

    @Test
    @DisplayName("two sessions of one account have independent changePassword throttles")
    @TestTransaction
    void testChangePassword_throttleIsPerSession() {
        final var entity = this.createAndFetchUser();
        final var locked = credentialsFor(entity);
        final var other = credentialsFor(entity);
        for (int i = 0; i < 5; i++) {
            this.failToChangePassword(locked);
        }

        this.failToChangePassword(other);
        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(locked, VALID_PASSWORD, "N3wP@ssword!"));
        assertEquals("Too many failed attempts. Try again later.", e.getMessage());
    }

    @Test
    @DisplayName("a correct current password clears only its own session's failed attempts")
    @TestTransaction
    void testChangePassword_successClearsOnlyOwnSession() {
        final var entity = this.createAndFetchUser();
        final var changing = credentialsFor(entity);
        final var bystander = credentialsFor(entity);
        for (int i = 0; i < 4; i++) {
            this.failToChangePassword(changing);
            this.failToChangePassword(bystander);
        }
        this.userService.changePassword(changing, VALID_PASSWORD, "N3wP@ssword!");
        // the password changed, so the bystander's stamp is stale now; its bucket is what is under test, so renew it
        final var renewed = new SessionCredentials(entity.publicId, AuthService.credentialStamp(entity.password),
                bystander.throttleKey());

        // the changing session got its attempts back: 4 more wrong guesses are not yet throttled
        final var again = new SessionCredentials(entity.publicId, AuthService.credentialStamp(entity.password),
                changing.throttleKey());
        for (int i = 0; i < 4; i++) {
            this.failToChangePassword(again);
        }
        // the bystander still had 4 used: one more wrong guess is the 5th, the next is refused
        this.failToChangePassword(renewed);
        final var e = assertThrows(ValidationException.class,
                () -> this.userService.changePassword(renewed, "N3wP@ssword!", "An0ther!Passw"));
        assertEquals("Too many failed attempts. Try again later.", e.getMessage());
    }

    @Test
    @DisplayName("failed password changes leave the login unaffected")
    @TestTransaction
    void testChangePassword_throttleLeavesLoginAlone() {
        final var entity = this.createAndFetchUser();
        entity.activated = true;
        final var credentials = credentialsFor(entity);
        for (int i = 0; i < 5; i++) {
            this.failToChangePassword(credentials);
        }

        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mock(VaadinSession.class));

            final var result = this.authService.authenticate(entity.username, VALID_PASSWORD);

            assertTrue(result.isSuccess(), result.getMessage());
        }
    }

    /** Runs the work in a committed transaction, like a UI request would, so after-commit observers fire. */
    private static void inCommittedTransaction(final Runnable work) {
        QuarkusTransaction.requiringNew().run(work);
    }

    /**
     * Whether an eviction forced {@code isAuthenticated} to re-check the DB for a session that cached its check just
     * before {@code before}, a timestamp the caller took before doing the work.
     */
    private boolean isEvicted(final UserEntity user, final long before) {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn(user.publicId);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_CREDENTIAL_STAMP))
                    .thenReturn(AuthService.credentialStamp(user.password));
            when(mockSess.getAttribute("authenticated.status")).thenReturn(true);
            when(mockSess.getAttribute("authenticated.lastDbCheck")).thenReturn(before - 1);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            this.authService.isAuthenticated();

            return Mockito.mockingDetails(mockSess).getInvocations().stream()
                    .anyMatch(i -> "setAttribute".equals(i.getMethod().getName())
                            && "authenticated.lastDbCheck".equals(i.getArgument(0)));
        }
    }

    private UserEntity createCommittedUser() {
        final var created = new UserEntity[1];
        inCommittedTransaction(() -> {
            final var dto = this.buildValidDto();
            dto.activated = true;
            final var publicId = this.userService.createUser(dto).publicId;
            created[0] = this.userRepository.findByPublicId(publicId).orElseThrow();
        });
        return created[0];
    }

    private void deleteCommittedUser(final UserEntity user) {
        inCommittedTransaction(() -> this.userRepository.deleteByPublicId(user.publicId));
    }

    /**
     * Runs the work in a committed transaction and tells whether the eviction was already visible before the commit.
     */
    private boolean evictedBeforeCommit(final UserEntity user, final long before, final Runnable work) {
        final var evicted = new boolean[1];
        inCommittedTransaction(() -> {
            work.run();
            evicted[0] = this.isEvicted(user, before);
        });
        return evicted[0];
    }

    @Test
    @DisplayName("patchUser evicts the account's cached authentication only after commit")
    void testPatchUser_evictsAfterCommit() {
        final var user = this.createCommittedUser();
        final long before = System.currentTimeMillis();
        try {
            final var dto = new UserDto();
            dto.banned = true;

            assertFalse(this.evictedBeforeCommit(user, before, () -> this.userService.patchUser(user.publicId, dto)),
                    "must not be evicted before the commit");
            assertTrue(this.isEvicted(user, before), "must be evicted after the commit");
        } finally {
            this.deleteCommittedUser(user);
        }
    }

    @Test
    @DisplayName("deleteUser evicts only after commit, and a rolled-back deleteUser evicts nothing")
    void testDeleteUser_evictsOnlyAfterCommit() {
        final var user = this.createCommittedUser();
        final long before = System.currentTimeMillis();
        try {
            assertThrows(IllegalStateException.class, () -> inCommittedTransaction(() -> {
                this.userService.deleteUser(user.publicId);
                throw new IllegalStateException("roll back");
            }));
            assertFalse(this.isEvicted(user, before), "a rolled-back delete must evict nothing");

            assertFalse(this.evictedBeforeCommit(user, before, () -> this.userService.deleteUser(user.publicId)),
                    "must not be evicted before the commit");
            assertTrue(this.isEvicted(user, before), "must be evicted after the commit");
        } finally {
            this.deleteCommittedUser(user);
        }
    }

    @Test
    @DisplayName("a committed changePassword evicts only after commit")
    void testChangePassword_evictsAfterCommit() {
        final var user = this.createCommittedUser();
        final long before = System.currentTimeMillis();
        try {
            final var credentials = credentialsFor(user);

            assertFalse(
                    this.evictedBeforeCommit(user, before,
                            () -> this.userService.changePassword(credentials, VALID_PASSWORD, "N3wP@ssword!")),
                    "must not be evicted before the commit");
            assertTrue(this.isEvicted(user, before), "must be evicted after the commit");
        } finally {
            this.deleteCommittedUser(user);
        }
    }

    /** Mocks a current session signed in as the user, so the services see them as the caller. */
    private static MockedStatic<VaadinSession> signedInAs(final UserEntity user) {
        final MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class);
        final VaadinSession mockSess = mock(VaadinSession.class);
        when(mockSess.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn(user.publicId);
        when(mockSess.getAttribute(AppConstants.SESSION_KEY_CREDENTIAL_STAMP))
                .thenReturn(AuthService.credentialStamp(user.password));
        mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
        return mockedSession;
    }

    @Test
    @DisplayName("a committed password patch of another account leaves the acting session's stamp alone")
    void testPatchPassword_leavesActingSessionStampAlone() {
        final var user = this.createCommittedUser();
        final var other = this.createCommittedUser();
        try (var _ = signedInAs(other)) {
            final var dto = new UserDto();
            dto.password = "N3wP@ssword!";

            inCommittedTransaction(() -> this.userService.patchUser(user.publicId, dto));

            verify(VaadinSession.getCurrent(), never()).setAttribute(eq(AppConstants.SESSION_KEY_CREDENTIAL_STAMP),
                    any());
        } finally {
            this.deleteCommittedUser(user);
            this.deleteCommittedUser(other);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "patchUser", "updateUser" })
    @DisplayName("the admin path refuses to change the caller's own password, which needs the current one")
    @TestTransaction
    void testAdminPath_refusesOwnPassword(final String method) {
        final var dto = this.buildValidDto();
        final var user = this.userRepository.findByPublicId(this.userService.createUser(dto).publicId).orElseThrow();
        final String hash = user.password;
        dto.password = "N3wP@ssword!";
        try (var _ = signedInAs(user)) {
            final Executable write = "updateUser".equals(method) ? () -> this.userService.updateUser(user.publicId, dto)
                    : () -> this.userService.patchUser(user.publicId, dto);
            final var e = assertThrows(ValidationException.class, write);
            assertEquals(AppConstants.OWN_PASSWORD_MESSAGE, e.getMessage());
        }
        assertEquals(hash, user.password);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "patchUser", "updateUser" })
    @DisplayName("the admin path still saves the caller's own account when no password is given")
    @TestTransaction
    void testAdminPath_savesOwnAccountWithoutPassword(final String method) {
        final var dto = this.buildValidDto();
        final var user = this.userRepository.findByPublicId(this.userService.createUser(dto).publicId).orElseThrow();
        dto.username = "renamed_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = null;
        try (var _ = signedInAs(user)) {
            final Executable write = "updateUser".equals(method) ? () -> this.userService.updateUser(user.publicId, dto)
                    : () -> this.userService.patchUser(user.publicId, dto);
            assertDoesNotThrow(write);
        }
        assertEquals(dto.username, user.username);
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
        update.rankPublicId = STUDENT_RANK_PUBLIC_ID;

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
    @DisplayName("getCurrentUser throws UNAUTHORIZED when session has no user public ID attribute")
    void testGetCurrentUser_nullPublicId_throwsUnauthorized() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            final var ex = assertThrows(WebApplicationException.class, () -> this.userService.getCurrentUser());
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("getCurrentUser returns user DTO when session has a valid user public ID and stamp")
    @TestTransaction
    void testGetCurrentUser_validSession_returnsUser() {
        try (var _ = signedInAs(this.userRepository.findByUsername("admin"))) {
            final UserViewDto result = this.userService.getCurrentUser();
            assertNotNull(result);
            assertEquals("admin", result.username);
        }
    }

    @Test
    @DisplayName("getCurrentUser throws UNAUTHORIZED when the session's public ID has no matching user")
    void testGetCurrentUser_userNotFound_throwsUnauthorized() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn("no-such-public-id");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            final var ex = assertThrows(WebApplicationException.class, () -> this.userService.getCurrentUser());
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
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

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({ "deleteUser, delete", "updateUser, ban", "updateUser, deactivate", "updateUser, demote",
            "patchUser, ban", "patchUser, deactivate", "patchUser, demote" })
    @DisplayName("The last active administrator cannot be deleted, banned, deactivated or demoted")
    @TestTransaction
    void lastAdministratorCannotBeRemoved(final String method, final String change) {
        final UserViewDto admin = this.createSoleAdministrator();

        final var e = assertThrows(ValidationException.class, this.removeAdministrator(admin, method, change));
        assertEquals(AppConstants.LAST_ADMINISTRATOR_MESSAGE, e.getMessage());
    }

    @ParameterizedTest(name = "other user's rank without {0}")
    @MethodSource("permissionFlags")
    @DisplayName("An active user whose rank lacks one permission does not count as an administrator")
    @TestTransaction
    void userMissingOnePermissionIsNoAdministrator(final String missing) {
        final UserViewDto admin = this.createSoleAdministrator();
        final UserDto other = this.buildValidDto();
        other.rankPublicId = this.createRankWithout(missing);
        other.activated = true;
        this.userService.createUser(other);
        final UserDto ban = new UserDto();
        ban.banned = true;

        final var e = assertThrows(ValidationException.class, () -> this.userService.patchUser(admin.publicId, ban));
        assertEquals(AppConstants.LAST_ADMINISTRATOR_MESSAGE, e.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "updateUser", "patchUser" })
    @DisplayName("The last active administrator can move to another rank granting every permission")
    @TestTransaction
    void lastAdministratorCanMoveToAnotherAdministratorRank(final String method) {
        final UserViewDto admin = this.createSoleAdministrator();
        final UserDto dto = new UserDto();
        dto.rankPublicId = this.createRankWithout("none");
        if ("updateUser".equals(method)) {
            dto.username = admin.username;
            dto.activated = true;
        }

        assertDoesNotThrow("updateUser".equals(method) ? () -> this.userService.updateUser(admin.publicId, dto)
                : () -> this.userService.patchUser(admin.publicId, dto));
        final UserEntity moved = this.userRepository.findByPublicId(admin.publicId).orElseThrow();
        assertEquals(dto.rankPublicId, moved.rank.publicId);
        assertTrue(UserRepository.isActiveAdministrator(moved));
    }

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({ "deleteUser, delete", "updateUser, ban", "updateUser, deactivate", "updateUser, demote",
            "patchUser, ban", "patchUser, deactivate", "patchUser, demote" })
    @DisplayName("An administrator can be removed while another active administrator remains")
    @TestTransaction
    void administratorCanBeRemovedWhileAnotherRemains(final String method, final String change) {
        final UserViewDto admin = this.createSoleAdministrator();
        this.createAdministrator();

        assertDoesNotThrow(this.removeAdministrator(admin, method, change));
        assertFalse(this.userRepository.findByPublicId(admin.publicId).filter(UserRepository::isActiveAdministrator)
                .isPresent());
    }

    @Test
    @DisplayName("The last active administrator can still be edited when the change keeps them one")
    @TestTransaction
    void lastAdministratorCanBeEditedWithoutLosingStatus() {
        final UserViewDto admin = this.createSoleAdministrator();
        final UserDto patch = new UserDto();
        patch.email = "admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        assertDoesNotThrow(() -> this.userService.patchUser(admin.publicId, patch));
    }

    /**
     * Builds the call that takes administrator status from {@code admin}: deleting, banning, deactivating, or moving
     * them to the Teacher rank, which lacks user and rank editing.
     */
    private Executable removeAdministrator(final UserViewDto admin, final String method, final String change) {
        if ("deleteUser".equals(method)) {
            return () -> this.userService.deleteUser(admin.publicId);
        }
        final UserDto dto = new UserDto();
        if ("updateUser".equals(method)) {
            dto.username = admin.username;
            dto.rankPublicId = ADMIN_RANK_PUBLIC_ID;
            dto.activated = true;
            dto.banned = false;
        }
        switch (change) {
            case "ban" -> dto.banned = true;
            case "deactivate" -> dto.activated = false;
            default -> dto.rankPublicId = TEACHER_RANK_PUBLIC_ID;
        }
        return "updateUser".equals(method) ? () -> this.userService.updateUser(admin.publicId, dto)
                : () -> this.userService.patchUser(admin.publicId, dto);
    }

    static Stream<String> permissionFlags() {
        return UserRankEntity.PERMISSION_FIELDS.stream();
    }

    /** Creates a rank granting every permission except {@code missing}, or all of them for {@code "none"}. */
    private String createRankWithout(final String missing) {
        return this.userRankService.createRank(TestUserRankFactory.rankWithout(missing)).publicId;
    }

    private UserViewDto createAdministrator() {
        final UserDto dto = this.buildValidDto();
        dto.rankPublicId = ADMIN_RANK_PUBLIC_ID;
        dto.activated = true;
        return this.userService.createUser(dto);
    }

    /**
     * Creates an active administrator and bans every other one, so the test does not depend on which administrators the
     * seed data or other tests left behind. The test transaction rolls the bans back.
     */
    private UserViewDto createSoleAdministrator() {
        final UserViewDto admin = this.createAdministrator();
        this.userRepository.findAll().stream()
                .filter(u -> !admin.publicId.equals(u.publicId) && UserRepository.isActiveAdministrator(u))
                .forEach(u -> u.banned = true);
        return admin;
    }

    private UserEntity createAndFetchUser() {
        final UserDto dto = this.buildValidDto();
        dto.activated = true;
        final UserViewDto created = this.userService.createUser(dto);
        return this.userRepository.findByPublicId(created.publicId).orElseThrow();
    }
}
