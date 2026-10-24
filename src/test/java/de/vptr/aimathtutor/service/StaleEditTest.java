package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

/**
 * A full update (PUT) of a user or rank carrying the version the caller loaded is refused when the stored version has
 * moved on; a null version skips the check.
 */
@QuarkusTest
class StaleEditTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String VALID_PASSWORD = "P@ssw0rd1";
    private static final String NEW_PASSWORD = "N3w-P@ssw0rd";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    // require* checks do nothing; the gates read the caller's rank from findCurrentUserRank
    @InjectMock
    PermissionService permissionService;

    @BeforeEach
    @Transactional
    void actAsAdmin() {
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A stale user update is refused and the other change stays")
    @TestTransaction
    void staleUserUpdateIsRefused() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final UserDto stale = loaded.toUserDto();
        assertEquals(loaded.version, stale.version);
        final UserDto ban = new UserDto();
        ban.banned = true;
        this.userService.patchUser(loaded.publicId, ban);
        stale.banned = false;

        final var ex =
                assertThrows(ValidationException.class, () -> this.userService.updateUser(loaded.publicId, stale));

        assertEquals(AppConstants.STALE_EDIT_MESSAGE, ex.getMessage());
        assertTrue(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().banned);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("The user's own appearance and avatar changes do not make an update stale")
    @TestTransaction
    void ownSettingsKeepTheVersion() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final var stored = this.userRepository.findByPublicId(loaded.publicId).orElseThrow();
        stored.colorMode = Appearance.ColorMode.DARK;
        stored.design = Appearance.Design.CHALKBOARD;
        stored.userAvatarEmoji = "🦊";
        stored.tutorAvatarEmoji = "🦉";
        this.userRepository.flush();
        assertEquals(loaded.version, stored.version);

        final UserDto dto = loaded.toUserDto();
        dto.password = null;
        dto.banned = true;
        this.userService.updateUser(loaded.publicId, dto);

        assertTrue(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().banned);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A user update with the current version succeeds and returns the stored, newer version")
    @TestTransaction
    void currentUserVersionSucceeds() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final UserDto dto = loaded.toUserDto();
        dto.password = null;
        dto.email = null;
        dto.banned = true;

        final UserViewDto updated = this.userService.updateUser(loaded.publicId, dto);

        assertNotNull(loaded.version);
        assertNotNull(updated.version);
        assertTrue(updated.version > loaded.version);
        assertEquals(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().version, updated.version);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A password change through patchUser returns the new version, which a following update accepts")
    @TestTransaction
    void passwordPatchThenUpdate() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final UserDto patch = new UserDto();
        patch.password = NEW_PASSWORD;

        final UserViewDto patched = this.userService.patchUser(loaded.publicId, patch);

        assertNotNull(loaded.version);
        assertNotNull(patched.version);
        assertTrue(patched.version > loaded.version);
        assertEquals(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().version, patched.version);
        final UserDto dto = patched.toUserDto();
        dto.password = null;
        dto.email = null;
        dto.banned = true;
        final UserViewDto updated = this.userService.updateUser(loaded.publicId, dto);
        assertNotNull(updated.version);
        assertTrue(updated.version > patched.version);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A user update without a version skips the check")
    @TestTransaction
    void nullUserVersionSkipsCheck() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final UserDto ban = new UserDto();
        ban.banned = true;
        this.userService.patchUser(loaded.publicId, ban);
        final UserDto dto = loaded.toUserDto();
        dto.version = null;
        dto.password = null;
        dto.email = null;
        dto.banned = true;
        dto.banned = false;

        this.userService.updateUser(loaded.publicId, dto);

        assertFalse(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().banned);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A stale rank update is refused and the other change stays")
    @TestTransaction
    void staleRankUpdateIsRefused() {
        final UserRankViewDto loaded = this.createRank();
        final UserRankDto stale = loaded.toUserRankDto();
        assertEquals(loaded.version, stale.version);
        final UserRankDto removal = new UserRankDto();
        removal.userAdd = false;
        this.userRankService.patchRank(loaded.publicId, removal);
        stale.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);

        final var ex =
                assertThrows(ValidationException.class, () -> this.userRankService.updateRank(loaded.publicId, stale));

        assertEquals(AppConstants.STALE_EDIT_MESSAGE, ex.getMessage());
        final var stored = this.userRankRepository.findByPublicId(loaded.publicId).orElseThrow();
        assertFalse(stored.userAdd);
        assertEquals(loaded.name, stored.name);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A rank update with the current version succeeds and returns the stored, newer version")
    @TestTransaction
    void currentRankVersionSucceeds() {
        final UserRankViewDto loaded = this.createRank();
        final UserRankDto dto = loaded.toUserRankDto();
        dto.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);

        final UserRankViewDto updated = this.userRankService.updateRank(loaded.publicId, dto);

        assertNotNull(loaded.version);
        assertNotNull(updated.version);
        assertTrue(updated.version > loaded.version);
        assertEquals(this.userRankRepository.findByPublicId(loaded.publicId).orElseThrow().version, updated.version);
        assertEquals(dto.name, updated.name);
    }

    private UserRankViewDto createRank() {
        final UserRankDto dto = new UserRankDto();
        dto.name = "Rank_" + UUID.randomUUID().toString().substring(0, 8);
        dto.userAdd = true;
        dto.userEdit = true;
        return this.userRankService.createRank(dto);
    }

    private static UserDto newUser() {
        final UserDto dto = new UserDto();
        dto.username = "stale_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = VALID_PASSWORD;
        dto.rankPublicId = ADMIN_RANK_PUBLIC_ID;
        return dto;
    }
}
