package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestUserRankFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Integration tests for UserRankService. Tests CRUD operations, searching, and permission management for user ranks.
 */
@QuarkusTest
@DisplayName("UserRankService Tests")
class UserRankServiceTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Inject
    UserRankService userRankService;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserService userService;

    @InjectMock
    PermissionService permissionService;

    @SuppressWarnings("NullAway")
    @BeforeEach
    @Transactional
    void setUp() {
        // The caller is an administrator unless a test says otherwise, so the privilege ceiling never applies
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
        // Clean up test ranks from previous runs
        final List<String> testRankNames =
                List.of("TestRank", "TestRankToUpdate", "TestRankToDelete", "TestAdminRank123", "TestUserRank456");
        for (final String name : testRankNames) {
            final var existing = this.userRankRepository.findByName(name);
            if (existing.isPresent()) {
                this.userRankRepository.deleteByPublicId(existing.get().publicId);
            }
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Create a new rank with default permissions")
    @TestTransaction
    void testCreateRank() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        rankDto.set(Permission.ADMIN_VIEW, true);
        rankDto.set(Permission.EXERCISE_ADD, true);
        rankDto.set(Permission.USER_EDIT, false);

        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        assertNotNull(created);
        assertEquals("TestRank", created.name);
        assertTrue(created.has(Permission.ADMIN_VIEW));
        assertTrue(created.has(Permission.EXERCISE_ADD));
        assertFalse(created.has(Permission.USER_EDIT));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Retrieve rank by ID")
    @TestTransaction
    void testFindById() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        rankDto.set(Permission.ADMIN_VIEW, true);
        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        // Test: Find by ID
        final var rankEntity = this.userRankRepository.findByPublicId(created.publicId).orElseThrow();
        final Optional<UserRankViewDto> found = this.userRankService.findById(rankEntity.id);

        assertTrue(found.isPresent());
        assertEquals("TestRank", found.get().name);
        assertTrue(found.get().has(Permission.ADMIN_VIEW));
    }

    @Test
    @DisplayName("Find rank by name")
    @TestTransaction
    void testFindByName() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        this.userRankService.createRank(rankDto);

        // Test: Find by name
        final Optional<UserRankViewDto> found = this.userRankService.findByName("TestRank");

        assertTrue(found.isPresent());
        assertEquals("TestRank", found.get().name);
    }

    @Test
    @DisplayName("findByName with a blank term returns empty instead of throwing")
    @Transactional
    void testFindByNameBlankReturnsEmpty() {
        // A blank or whitespace-only term cannot match a stored rank, so the lookup yields an empty result
        // rather than a ValidationException (which is reserved for the write paths).
        assertTrue(this.userRankService.findByName("").isEmpty());
        assertTrue(this.userRankService.findByName("   ").isEmpty());
    }

    @Test
    @DisplayName("Get all ranks")
    @Transactional
    void testGetAllRanks() {
        // Setup: Create multiple ranks
        final UserRankDto rank1 = new UserRankDto();
        rank1.name = "TestRank";
        this.userRankService.createRank(rank1);

        final UserRankDto rank2 = new UserRankDto();
        rank2.name = "TestRankToUpdate";
        this.userRankService.createRank(rank2);

        // Test: Get all ranks
        final List<UserRankViewDto> allRanks = this.userRankService.getAllRanks();

        assertFalse(allRanks.isEmpty());
        assertTrue(allRanks.stream().anyMatch(r -> "TestRank".equals(r.name)));
        assertTrue(allRanks.stream().anyMatch(r -> "TestRankToUpdate".equals(r.name)));
    }

    @Test
    @DisplayName("Search ranks by query")
    @TestTransaction
    void testSearchRanks() {
        // Setup: Create ranks with unique names
        final UserRankDto rank1 = new UserRankDto();
        rank1.name = "TestAdminRank123";
        this.userRankService.createRank(rank1);

        final UserRankDto rank2 = new UserRankDto();
        rank2.name = "TestUserRank456";
        this.userRankService.createRank(rank2);

        // Test: Search for "admin" (should find TestAdminRank123)
        final List<UserRankViewDto> results = this.userRankService.searchRanks("admin");

        assertTrue(results.stream().anyMatch(r -> "TestAdminRank123".equals(r.name)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Update rank permissions")
    @TestTransaction
    void testUpdateRank() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRankToUpdate";
        rankDto.set(Permission.ADMIN_VIEW, false);
        rankDto.set(Permission.EXERCISE_ADD, false);
        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        // Test: Update the rank
        final UserRankDto updateDto = new UserRankDto();
        updateDto.name = "TestRankToUpdate";
        updateDto.set(Permission.ADMIN_VIEW, true);
        updateDto.set(Permission.EXERCISE_ADD, true);
        updateDto.set(Permission.EXERCISE_EDIT, true);
        final UserRankViewDto updated = this.userRankService.updateRank(created.publicId, updateDto);

        assertTrue(updated.has(Permission.ADMIN_VIEW));
        assertTrue(updated.has(Permission.EXERCISE_ADD));
        assertTrue(updated.has(Permission.EXERCISE_EDIT));
    }

    @SuppressWarnings("NullAway")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("updateRank turns a permission the DTO leaves out off")
    @TestTransaction
    void updateRankTreatsMissingPermissionAsFalse(final Permission permission) {
        final UserRankDto createDto = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        createDto.set(permission, true);
        final UserRankViewDto created = this.userRankService.createRank(createDto);

        final UserRankDto updateDto = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        final UserRankViewDto updated = this.userRankService.updateRank(created.publicId, updateDto);

        assertFalse(updated.has(permission));
    }

    @SuppressWarnings("NullAway")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("patchRank leaves a permission the DTO leaves out untouched")
    @TestTransaction
    void patchRankLeavesMissingPermissionUntouched(final Permission permission) {
        final UserRankDto createDto = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        createDto.set(permission, true);
        final UserRankViewDto created = this.userRankService.createRank(createDto);

        final UserRankDto patch = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        final UserRankViewDto patched = this.userRankService.patchRank(created.publicId, patch);

        assertTrue(patched.has(permission));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Search with empty query returns all ranks")
    @Transactional
    void testSearchWithEmptyQuery() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        this.userRankService.createRank(rankDto);

        // Test: Search with null/empty query should return all
        final List<UserRankViewDto> allWithNull = this.userRankService.searchRanks(null);
        final List<UserRankViewDto> allWithEmpty = this.userRankService.searchRanks("");

        assertFalse(allWithNull.isEmpty());
        assertFalse(allWithEmpty.isEmpty());
        assertTrue(allWithNull.stream().anyMatch(r -> "TestRank".equals(r.name)));
        assertTrue(allWithEmpty.stream().anyMatch(r -> "TestRank".equals(r.name)));
    }

    @Test
    @DisplayName("Update non-existent rank throws exception")
    @Transactional
    void testUpdateNonExistentRank() {
        final UserRankDto updateDto = new UserRankDto();
        updateDto.name = "NonExistent";

        assertThrows(Exception.class, () -> this.userRankService.updateRank("00000000000000000000000000", updateDto));
    }

    @Test
    @DisplayName("Create rank with null name throws validation exception")
    @Transactional
    void testCreateRankWithNullName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = null;

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }

    @Test
    @DisplayName("Create rank with empty name throws validation exception")
    @Transactional
    void testCreateRankWithEmptyName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "";

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }

    @Test
    @DisplayName("Create rank with blank name throws validation exception")
    @Transactional
    void testCreateRankWithBlankName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "   ";

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }

    @Test
    @DisplayName("findByPublicId returns empty for unknown publicId")
    @Transactional
    void testFindByPublicId_notFound() {
        final var result = this.userRankService.findByPublicId("00000000000000000000000000");
        assertFalse(result.isPresent());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("patchRank updates only provided fields")
    @TestTransaction
    void testPatchRank() {
        final UserRankDto createDto = new UserRankDto();
        createDto.name = "PatchTestRank_" + UUID.randomUUID().toString().substring(0, 8);
        createDto.set(Permission.EXERCISE_ADD, false);
        createDto.set(Permission.ADMIN_VIEW, false);
        createDto.set(Permission.COMMENT_ADD, true);
        final UserRankViewDto created = this.userRankService.createRank(createDto);

        final UserRankDto patch = new UserRankDto();
        patch.name = "PatchedName_" + UUID.randomUUID().toString().substring(0, 8);
        patch.set(Permission.EXERCISE_ADD, true);

        final UserRankViewDto patched = this.userRankService.patchRank(created.publicId, patch);

        assertEquals(patch.name, patched.name, "Name should be updated");
        assertTrue(patched.has(Permission.EXERCISE_ADD), "exerciseAdd should be updated to true");
        assertEquals(created.get(Permission.ADMIN_VIEW), patched.get(Permission.ADMIN_VIEW),
                "adminView should be unchanged after patch");
        assertEquals(created.get(Permission.COMMENT_ADD), patched.get(Permission.COMMENT_ADD),
                "commentAdd should be unchanged after patch");
    }

    @Test
    @DisplayName("deleteRank returns false for unknown publicId")
    @Transactional
    void testDeleteRank_notFound() {
        final boolean deleted = this.userRankService.deleteRank("00000000000000000000000000");
        assertFalse(deleted);
    }

    @Test
    @DisplayName("getCurrentUserRank returns null when no VaadinSession exists")
    void testCurrentUserRank_withNullSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedSession.when(VaadinSession::getCurrent).thenReturn(null);
            assertNull(this.userRankService.getCurrentUserRank());
        }
    }

    @Test
    @DisplayName("getCurrentUserRank returns null when session has no user public ID attribute")
    void testCurrentUserRank_withNullPublicId() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertNull(this.userRankService.getCurrentUserRank());
        }
    }

    @Test
    @DisplayName("getCurrentUserRank returns rank DTO when session has a user public ID and user has a rank")
    @TestTransaction
    void testCurrentUserRank_withValidSessionAndRank() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "CurrentRankTest_" + UUID.randomUUID().toString().substring(0, 8);
        final UserRankViewDto rank = this.userRankService.createRank(rankDto);

        final var admin = this.userRepository.findByUsername("admin");
        assertNotNull(admin, "Seeded admin must exist");
        admin.activated = true;
        admin.banned = false;
        admin.rank = this.userRankRepository.findByPublicId(rank.publicId).orElseThrow();
        this.userRepository.persist(admin);

        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            stubSessionUser(mockSess, admin);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            final UserRankViewDto result = this.userRankService.getCurrentUserRank();
            assertNotNull(result);
            assertEquals(rank.name, result.name);
        }
    }

    @ParameterizedTest(name = "banned={0}, activated={1}")
    @CsvSource({ "true, true", "false, false" })
    @DisplayName("getCurrentUserRankEntity returns null for a banned or deactivated current user")
    @TestTransaction
    void currentRankIsNullForBannedOrInactiveUser(final boolean banned, final boolean activated) {
        final String username = this.createActiveUser(ADMIN_RANK_PUBLIC_ID);
        final var user = this.userRepository.findByUsername(username);
        assertNotNull(user);
        user.banned = banned;
        user.activated = activated;

        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            stubSessionUser(mockSess, user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertNull(this.userRankService.getCurrentUserRankEntity());
        }
    }

    @Test
    @DisplayName("getCurrentUserRankEntity returns null when the session's credential stamp no longer matches")
    @TestTransaction
    void currentRankIsNullForStampMismatch() {
        final var user = this.userRepository.findByUsername(this.createActiveUser(ADMIN_RANK_PUBLIC_ID));
        assertNotNull(user);

        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            stubSessionUser(mockSess, user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertNotNull(this.userRankService.getCurrentUserRankEntity());

            user.password = "$2a$10$aDifferentHashThatIsNotTheOriginalOneAtAllxxxxxxxxxxxxxxxxxx";

            assertNull(this.userRankService.getCurrentUserRankEntity());
        }
    }

    private static void stubSessionUser(final VaadinSession session, final UserEntity user) {
        when(session.getAttribute(AppConstants.SESSION_KEY_USER_PUBLIC_ID)).thenReturn(user.publicId);
        when(session.getAttribute(AppConstants.SESSION_KEY_CREDENTIAL_STAMP))
                .thenReturn(AuthService.credentialStamp(user.password));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("deleteRank throws CONFLICT when users are assigned to the rank")
    @TestTransaction
    void testDeleteRank_withAssignedUsers() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "ConflictRank_" + UUID.randomUUID().toString().substring(0, 8);
        rankDto.set(Permission.COMMENT_ADD, true);
        final UserRankViewDto rank = this.userRankService.createRank(rankDto);
        this.createActiveUser(rank.publicId);

        final var ex =
                assertThrows(WebApplicationException.class, () -> this.userRankService.deleteRank(rank.publicId));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), ex.getResponse().getStatus());
    }

    @ParameterizedTest(name = "{0} without {1}")
    @MethodSource("editsStrippingOneFlag")
    @DisplayName("The rank holding the last active administrators cannot lose a permission")
    @TestTransaction
    void lastAdministratorRankKeepsAdministration(final String method, final Permission permission) {
        final UserRankViewDto rank = this.createAdministratorRankWithUser();

        final var e = assertThrows(ValidationException.class, this.editRank(rank, method, permission));
        assertEquals(AppConstants.LAST_ADMINISTRATOR_MESSAGE, e.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "updateRank", "patchRank" })
    @DisplayName("The rank holding the last active administrators can be edited when it keeps every permission")
    @TestTransaction
    void lastAdministratorRankCanBeEditedWithoutStripping(final String method) {
        final UserRankViewDto rank = this.createAdministratorRankWithUser();

        assertDoesNotThrow(this.editRank(rank, method, null));
        assertTrue(UserRepository
                .grantsAdministration(this.userRankRepository.findByPublicId(rank.publicId).orElseThrow()));
    }

    @ParameterizedTest(name = "{0} without {1}")
    @MethodSource("editsStrippingOneFlag")
    @DisplayName("A rank can lose a permission while an administrator outside it remains")
    @TestTransaction
    void administratorRankCanLoseAdministrationWhileAnotherRemains(final String method, final Permission permission) {
        final UserRankViewDto rank = this.createAdministratorRankWithUser();
        this.createActiveUser(ADMIN_RANK_PUBLIC_ID);

        assertDoesNotThrow(this.editRank(rank, method, permission));
        assertFalse(UserRepository
                .grantsAdministration(this.userRankRepository.findByPublicId(rank.publicId).orElseThrow()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "updateRank", "patchRank" })
    @DisplayName("A rank can lose a permission when there is no active administrator to protect")
    @TestTransaction
    void administratorRankCanLoseAdministrationWithoutActiveAdministrators(final String method) {
        final UserRankViewDto rank = this.createAdministratorRankWithUser();
        this.banActiveAdministrators(null);

        assertDoesNotThrow(this.editRank(rank, method, Permission.USER_EDIT));
        assertFalse(UserRepository
                .grantsAdministration(this.userRankRepository.findByPublicId(rank.publicId).orElseThrow()));
    }

    /** Every rank edit paired with every flag it can strip. */
    static Stream<Arguments> editsStrippingOneFlag() {
        return Stream.of("updateRank", "patchRank")
                .flatMap(method -> Arrays.stream(Permission.values()).map(flag -> Arguments.of(method, flag)));
    }

    /**
     * Creates a rank granting every permission, with one active user, and bans every active administrator outside it,
     * so that user is the only one. The test transaction rolls the bans back.
     */
    @SuppressWarnings("NullAway")
    private UserRankViewDto createAdministratorRankWithUser() {
        final UserRankViewDto rank = this.userRankService.createRank(TestUserRankFactory.rankWithout(null));
        this.createActiveUser(rank.publicId);
        this.banActiveAdministrators(rank.publicId);
        return rank;
    }

    /** Bans every active administrator, except those holding the given rank if it is not null. */
    @SuppressWarnings("NullAway")
    private void banActiveAdministrators(@Nullable final String keptRankPublicId) {
        this.userRepository.findAll().stream().filter(
                u -> UserRepository.isActiveAdministrator(u) && !Objects.equals(keptRankPublicId, u.rank.publicId))
                .forEach(u -> u.banned = true);
    }

    @SuppressWarnings("NullAway")
    private String createActiveUser(final String rankPublicId) {
        final UserDto userDto = new UserDto();
        final String suffix = UUID.randomUUID().toString().substring(0, 8);
        userDto.username = "rankuser_" + suffix;
        userDto.password = "P@ssw0rd1";
        userDto.rankPublicId = rankPublicId;
        userDto.activated = true;
        return this.userService.createUser(userDto).username;
    }

    /**
     * Builds the call that edits a rank granting every permission. The update sends the whole rank under a new name,
     * switching off {@code stripped}; the patch sends only {@code stripped} switched off, or only a new name for null,
     * since the rank already grants every permission.
     */
    @SuppressWarnings("NullAway")
    private Executable editRank(final UserRankViewDto rank, final String method, @Nullable final Permission stripped) {
        if ("patchRank".equals(method)) {
            final UserRankDto dto = new UserRankDto();
            if (stripped == null) {
                dto.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);
            } else {
                dto.set(stripped, false);
            }
            return () -> this.userRankService.patchRank(rank.publicId, dto);
        }
        final UserRankDto dto = TestUserRankFactory.rankWithout(stripped);
        return () -> this.userRankService.updateRank(rank.publicId, dto);
    }
}
