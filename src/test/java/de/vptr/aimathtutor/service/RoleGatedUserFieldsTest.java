package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

/**
 * The user fields gated by the caller's rank: {@code activated} and {@code banned} by {@code adminView}, the rank of an
 * existing user by {@code userRankEdit}. Without the permission the field is ignored, not refused.
 */
@QuarkusTest
class RoleGatedUserFieldsTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String VALID_PASSWORD = "P@ssw0rd1";
    private static final String INVISIBLE_USERNAME = "admin\u200b";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager em;

    // require* checks do nothing; the gates read the caller's rank from findCurrentUserRank
    @InjectMock
    PermissionService permissionService;

    @BeforeEach
    @Transactional
    void actAsAdmin() {
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    // adminView: activated and banned

    @Test
    @DisplayName("Create without adminView ignores activated and banned")
    @TestTransaction
    void createWithoutAdminViewIgnoresStatus() {
        final String rank = this.createRank(false, true);
        this.actAs(rank);
        final UserDto dto = userDto(rank);
        dto.activated = true;
        dto.banned = true;

        final UserViewDto created = this.userService.createUser(dto);

        final UserEntity stored = this.stored(created);
        assertFalse(stored.activated);
        assertFalse(stored.banned);
    }

    @Test
    @DisplayName("Create with adminView stores activated and banned as asked")
    @TestTransaction
    void createWithAdminViewStoresStatus() {
        final String rank = this.createRank(true, true);
        this.actAs(rank);
        final UserDto dto = userDto(rank);
        dto.activated = true;
        dto.banned = true;

        final UserEntity stored = this.stored(this.userService.createUser(dto));
        assertTrue(stored.activated);
        assertTrue(stored.banned);
    }

    @SuppressWarnings("NullAway")
    @ParameterizedTest(name = "adminView={0}")
    @ValueSource(booleans = { false, true })
    @DisplayName("Update changes a stored ban and activation only with adminView")
    @TestTransaction
    void updateChangesStatusOnlyWithAdminView(final boolean adminView) {
        final String rank = this.createRank(adminView, true);
        final UserViewDto target = this.createStatusTarget(rank);
        this.actAs(rank);
        final UserDto dto = userDto(rank);
        dto.username = target.username;
        dto.password = null;
        dto.banned = false;
        dto.activated = false;

        this.userService.updateUser(target.publicId, dto);

        final UserEntity stored = this.stored(target);
        assertEquals(!adminView, stored.banned);
        assertEquals(!adminView, stored.activated);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Patch without adminView keeps a stored ban")
    @TestTransaction
    void patchWithoutAdminViewKeepsStatus() {
        final String rank = this.createRank(false, true);
        final UserViewDto target = this.createStatusTarget(rank);
        this.actAs(rank);
        final UserDto dto = new UserDto();
        dto.banned = false;
        dto.activated = false;

        this.userService.patchUser(target.publicId, dto);

        final UserEntity stored = this.stored(target);
        assertTrue(stored.banned);
        assertTrue(stored.activated);
    }

    // userRankEdit: the rank of an existing user

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Update without userRankEdit keeps the rank, whether the DTO names another or none")
    @TestTransaction
    void updateWithoutUserRankEditKeepsRank() {
        final String rank = this.createRank(true, false);
        final String other = this.createLowerRank();
        final UserViewDto target = this.userService.createUser(userDto(rank));
        this.actAs(rank);
        final UserDto dto = userDto(other);
        dto.username = target.username;
        dto.password = null;

        this.userService.updateUser(target.publicId, dto);
        assertEquals(rank, this.stored(target).rank.publicId);

        dto.rankPublicId = null;
        this.userService.updateUser(target.publicId, dto);
        assertEquals(rank, this.stored(target).rank.publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Patch without userRankEdit keeps the rank")
    @TestTransaction
    void patchWithoutUserRankEditKeepsRank() {
        final String rank = this.createRank(true, false);
        final String other = this.createLowerRank();
        final UserViewDto target = this.userService.createUser(userDto(rank));
        this.actAs(rank);
        final UserDto dto = new UserDto();
        dto.rankPublicId = other;

        this.userService.patchUser(target.publicId, dto);

        assertEquals(rank, this.stored(target).rank.publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Update and patch with userRankEdit change the rank within the ceiling")
    @TestTransaction
    void withUserRankEditChangesRank() {
        final String rank = this.createRank(true, true);
        final String other = this.createLowerRank();
        final UserViewDto target = this.userService.createUser(userDto(rank));
        this.actAs(rank);
        final UserDto put = userDto(other);
        put.username = target.username;
        put.password = null;
        this.userService.updateUser(target.publicId, put);
        assertEquals(other, this.stored(target).rank.publicId);

        final UserDto patch = new UserDto();
        patch.rankPublicId = rank;
        this.userService.patchUser(target.publicId, patch);
        assertEquals(rank, this.stored(target).rank.publicId);

        final UserDto above = new UserDto();
        above.rankPublicId = ADMIN_RANK_PUBLIC_ID;
        final var e = assertThrows(ValidationException.class, () -> this.userService.patchUser(target.publicId, above));
        assertEquals(AppConstants.PRIVILEGE_CEILING_MESSAGE, e.getMessage());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("Create without userRankEdit applies the requested rank and still requires one")
    @TestTransaction
    void createWithoutUserRankEditStillAppliesRank() {
        final String rank = this.createRank(true, false);
        final String other = this.createLowerRank();
        this.actAs(rank);

        assertEquals(other, this.stored(this.userService.createUser(userDto(other))).rank.publicId);

        final var e = assertThrows(ValidationException.class, () -> this.userService.createUser(userDto(null)));
        assertEquals("Rank is required", e.getMessage());
    }

    // Usernames with invisible characters, through every entry

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("createUser, updateUser and patchUser refuse a username with an invisible character")
    @TestTransaction
    void serviceRefusesInvisibleUsername() {
        final UserViewDto target = this.userService.createUser(userDto(ADMIN_RANK_PUBLIC_ID));
        final UserDto create = userDto(ADMIN_RANK_PUBLIC_ID);
        create.username = INVISIBLE_USERNAME;
        final UserDto put = userDto(ADMIN_RANK_PUBLIC_ID);
        put.username = INVISIBLE_USERNAME;
        final UserDto patch = new UserDto();
        patch.username = INVISIBLE_USERNAME;

        this.assertPatternRefused(() -> this.userService.createUser(create));
        this.assertPatternRefused(() -> this.userService.updateUser(target.publicId, put));
        this.assertPatternRefused(() -> this.userService.patchUser(target.publicId, patch));
    }

    @Test
    @DisplayName("createInitialAdmin refuses a username with an invisible character")
    @TestTransaction
    void initialAdminRefusesInvisibleUsername() {
        // It returns before validating while any user exists, so empty the table inside this rolled-back transaction
        this.em.createQuery("DELETE FROM UserEntity").executeUpdate();
        assertFalse(this.userService.hasUsers());

        this.assertPatternRefused(() -> this.userService.createInitialAdmin(INVISIBLE_USERNAME, VALID_PASSWORD));
    }

    @SuppressWarnings("NullAway")
    private void assertPatternRefused(final Executable call) {
        final var e = assertThrows(ValidationException.class, call);
        assertTrue(e.getMessage().contains(AppConstants.USERNAME_PATTERN_MESSAGE), e::getMessage);
    }

    /** A rank holding userAdd and userEdit, plus adminView and userRankEdit as asked. Created while acting as admin. */
    @SuppressWarnings("NullAway")
    private String createRank(final boolean adminView, final boolean userRankEdit) {
        final UserRankDto dto = new UserRankDto();
        dto.name = "Rank_" + UUID.randomUUID().toString().substring(0, 8);
        dto.set(Permission.USER_ADD, true);
        dto.set(Permission.USER_EDIT, true);
        dto.set(Permission.ADMIN_VIEW, adminView);
        dto.set(Permission.USER_RANK_EDIT, userRankEdit);
        return this.userRankService.createRank(dto).publicId;
    }

    /** A rank below any {@link #createRank} rank: no permissions at all. */
    @SuppressWarnings("NullAway")
    private String createLowerRank() {
        final UserRankDto dto = new UserRankDto();
        dto.name = "Lower_" + UUID.randomUUID().toString().substring(0, 8);
        return this.userRankService.createRank(dto).publicId;
    }

    /** A banned, activated user, created while acting as admin. */
    private UserViewDto createStatusTarget(final String rankPublicId) {
        final UserDto dto = userDto(rankPublicId);
        dto.banned = true;
        dto.activated = true;
        return this.userService.createUser(dto);
    }

    private void actAs(final String rankPublicId) {
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(rankPublicId).orElseThrow());
    }

    @SuppressWarnings("NullAway")
    private UserEntity stored(final UserViewDto user) {
        return this.userRepository.findByPublicId(user.publicId).orElseThrow();
    }

    private static UserDto userDto(final String rankPublicId) {
        final UserDto dto = new UserDto();
        dto.username = "gated_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = VALID_PASSWORD;
        dto.rankPublicId = rankPublicId;
        return dto;
    }
}
