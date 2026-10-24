package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

    @Inject
    EntityManager entityManager;

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
        this.entityManager.clear();

        final var reloaded = this.userRepository.findByPublicId(loaded.publicId).orElseThrow();
        assertEquals(Appearance.ColorMode.DARK, reloaded.colorMode);
        assertEquals(Appearance.Design.CHALKBOARD, reloaded.design);
        assertEquals("🦊", reloaded.userAvatarEmoji);
        assertEquals("🦉", reloaded.tutorAvatarEmoji);
        assertEquals(loaded.version, reloaded.version);

        final UserDto dto = loaded.toUserDto();
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
        dto.banned = false;

        this.userService.updateUser(loaded.publicId, dto);

        assertFalse(this.userRepository.findByPublicId(loaded.publicId).orElseThrow().banned);
    }

    @Test
    @DisplayName("A stale rank update is refused and the other change stays")
    @TestTransaction
    void staleRankUpdateIsRefused() {
        this.assertStaleRankWriteRefused(this.userRankService::updateRank);
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

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A stale password patch is refused: the ban stays and the password is unchanged")
    @TestTransaction
    void stalePasswordPatchIsRefused() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final String hash = this.userRepository.findByPublicId(loaded.publicId).orElseThrow().password;
        final UserDto ban = new UserDto();
        ban.banned = true;
        this.userService.patchUser(loaded.publicId, ban);
        final UserDto stale = new UserDto();
        stale.password = NEW_PASSWORD;
        stale.version = loaded.version;

        final var ex =
                assertThrows(ValidationException.class, () -> this.userService.patchUser(loaded.publicId, stale));

        assertEquals(AppConstants.STALE_EDIT_MESSAGE, ex.getMessage());
        final var stored = this.userRepository.findByPublicId(loaded.publicId).orElseThrow();
        assertTrue(stored.banned);
        assertEquals(hash, stored.password);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A password patch with the current version succeeds and returns a newer version")
    @TestTransaction
    void currentVersionPasswordPatchSucceeds() {
        final UserViewDto loaded = this.userService.createUser(newUser());
        final UserDto patch = new UserDto();
        patch.password = NEW_PASSWORD;
        patch.version = loaded.version;

        final UserViewDto patched = this.userService.patchUser(loaded.publicId, patch);

        assertNotNull(loaded.version);
        assertNotNull(patched.version);
        assertTrue(patched.version > loaded.version);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A stale DTO for a user above the caller's ceiling gets the ceiling message")
    @TestTransaction
    void ceilingBeatsStaleVersion() {
        final UserViewDto admin = this.userService.createUser(newUser());
        final UserDto stale = admin.toUserDto();
        final UserDto ban = new UserDto();
        ban.banned = true;
        this.userService.patchUser(admin.publicId, ban);
        final UserRankDto lowerDto = new UserRankDto();
        lowerDto.name = "Rank_" + UUID.randomUUID().toString().substring(0, 8);
        lowerDto.adminView = true;
        lowerDto.userEdit = true;
        final UserRankViewDto lower = this.userRankService.createRank(lowerDto);
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(lower.publicId).orElseThrow());

        final var ex =
                assertThrows(ValidationException.class, () -> this.userService.updateUser(admin.publicId, stale));

        assertEquals(AppConstants.PRIVILEGE_CEILING_MESSAGE, ex.getMessage());
    }

    @Test
    @DisplayName("A stale rank patch is refused and the other change stays")
    @TestTransaction
    void staleRankPatchIsRefused() {
        this.assertStaleRankWriteRefused(this.userRankService::patchRank);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A rank update without a version skips the check")
    @TestTransaction
    void nullRankVersionSkipsCheck() {
        final UserRankViewDto loaded = this.createRank();
        final UserRankDto removal = new UserRankDto();
        removal.userAdd = false;
        this.userRankService.patchRank(loaded.publicId, removal);
        final UserRankDto dto = loaded.toUserRankDto();
        dto.version = null;
        dto.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);

        this.userRankService.updateRank(loaded.publicId, dto);

        assertEquals(dto.name, this.userRankRepository.findByPublicId(loaded.publicId).orElseThrow().name);
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A concurrent settings change and an admin write both survive")
    void settingsChangeAndAdminWriteBothSurvive() {
        final String publicId = this.commitUser();
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                final var outer = this.userRepository.findByPublicId(publicId).orElseThrow();
                QuarkusTransaction.requiringNew()
                        .run(() -> this.userRepository.findByPublicId(publicId).orElseThrow().colorMode =
                                Appearance.ColorMode.DARK);
                outer.banned = true;
            });

            final var stored = this.committedUser(publicId);
            assertTrue(stored.banned);
            assertEquals(Appearance.ColorMode.DARK, stored.colorMode);
        } finally {
            this.deleteUser(publicId);
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("A settings write over a concurrent admin write fails and the admin write stays")
    void settingsWriteOverAdminWriteFails() {
        final String publicId = this.commitUser();
        try {
            // The zero-row UPDATE surfaces as HibernateException ("returned no natively generated values"), not
            // as an OptimisticLockException, because lastEdit is @Generated on UPDATE
            assertThrows(HibernateException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
                final var outer = this.userRepository.findByPublicId(publicId).orElseThrow();
                QuarkusTransaction.requiringNew()
                        .run(() -> this.userRepository.findByPublicId(publicId).orElseThrow().banned = true);
                outer.colorMode = Appearance.ColorMode.DARK;
                this.userRepository.flush();
            }));

            final var stored = this.committedUser(publicId);
            assertTrue(stored.banned);
            assertEquals(Appearance.DEFAULT.colorMode(), stored.colorMode);
        } finally {
            this.deleteUser(publicId);
        }
    }

    @SuppressWarnings("NullAway")
    private void assertStaleRankWriteRefused(final BiConsumer<String, UserRankDto> write) {
        final UserRankViewDto loaded = this.createRank();
        final UserRankDto stale = loaded.toUserRankDto();
        final UserRankDto removal = new UserRankDto();
        removal.userAdd = false;
        this.userRankService.patchRank(loaded.publicId, removal);
        stale.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);

        final var ex = assertThrows(ValidationException.class, () -> write.accept(loaded.publicId, stale));

        assertEquals(AppConstants.STALE_EDIT_MESSAGE, ex.getMessage());
        final var stored = this.userRankRepository.findByPublicId(loaded.publicId).orElseThrow();
        assertFalse(stored.userAdd);
        assertEquals(loaded.name, stored.name);
    }

    private String commitUser() {
        return QuarkusTransaction.requiringNew().call(() -> this.userService.createUser(newUser()).publicId);
    }

    private UserEntity committedUser(final String publicId) {
        return QuarkusTransaction.requiringNew().call(() -> this.userRepository.findByPublicId(publicId).orElseThrow());
    }

    private void deleteUser(final String publicId) {
        QuarkusTransaction.requiringNew()
                .run(() -> this.entityManager.createQuery("DELETE FROM UserEntity u WHERE u.publicId = :p")
                        .setParameter("p", publicId).executeUpdate());
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
