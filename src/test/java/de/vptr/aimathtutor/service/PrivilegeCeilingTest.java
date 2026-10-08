package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
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
 * The privilege ceiling: a caller may not change, delete or assign a user or rank that holds a permission the caller's
 * rank lacks, nor grant a rank such a permission. The caller holds a user-and-rank manager rank without
 * {@code aiConfigEdit}, so the seeded Admin rank is above it.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
class PrivilegeCeilingTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String VALID_PASSWORD = "P@ssw0rd1";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    UserRepository userRepository;

    // require* checks do nothing; the ceiling reads the caller's rank from findCurrentUserRank
    @InjectMock
    PermissionService permissionService;

    /** The caller's rank: manages users and ranks, but lacks aiConfigEdit and the content permissions. */
    private UserRankViewDto callerRank;

    @BeforeEach
    @Transactional
    void noCeilingForSetup() {
        // Admin until a test calls actAsManager(), so the test setup itself is not limited
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    // Users above the caller

    @Test
    @DisplayName("Creating a user with a higher rank is refused")
    @TestTransaction
    void createUserWithHigherRankIsRefused() {
        this.actAsManager();
        assertRefused(() -> this.userService.createUser(userDto(ADMIN_RANK_PUBLIC_ID)));
    }

    @Test
    @DisplayName("Updating a user with a higher rank is refused")
    @TestTransaction
    void updateHigherUserIsRefused() {
        final UserViewDto admin = this.userService.createUser(userDto(ADMIN_RANK_PUBLIC_ID));
        this.actAsManager();
        // Assign the caller's own rank, so only the check of the user's current rank can refuse
        final UserDto change = userDto(this.callerRank.publicId);
        change.username = admin.username;

        assertRefused(() -> this.userService.updateUser(admin.publicId, change));
    }

    @Test
    @DisplayName("Changing the password of a user with a higher rank is refused")
    @TestTransaction
    void patchHigherUserPasswordIsRefused() {
        final UserViewDto admin = this.userService.createUser(userDto(ADMIN_RANK_PUBLIC_ID));
        this.actAsManager();
        final UserDto change = new UserDto();
        change.password = "N3w-P@ssword";

        assertRefused(() -> this.userService.patchUser(admin.publicId, change));
    }

    @Test
    @DisplayName("Deleting a user with a higher rank is refused")
    @TestTransaction
    void deleteHigherUserIsRefused() {
        final UserViewDto admin = this.userService.createUser(userDto(ADMIN_RANK_PUBLIC_ID));
        this.actAsManager();

        assertRefused(() -> this.userService.deleteUser(admin.publicId));
    }

    @Test
    @DisplayName("Assigning a higher rank to another user is refused")
    @TestTransaction
    void assignHigherRankIsRefused() {
        this.actAsManager();
        final UserViewDto peer = this.userService.createUser(userDto(this.callerRank.publicId));
        final UserDto promote = new UserDto();
        promote.rankPublicId = ADMIN_RANK_PUBLIC_ID;

        assertRefused(() -> this.userService.patchUser(peer.publicId, promote));
    }

    @Test
    @DisplayName("Assigning a higher rank to oneself is refused")
    @TestTransaction
    void assignHigherRankToSelfIsRefused() {
        this.actAsManager();
        final UserViewDto self = this.userService.createUser(userDto(this.callerRank.publicId));
        // The caller is this user: their ceiling is whatever rank they hold at the moment it is read
        when(this.permissionService.findCurrentUserRank())
                .thenAnswer(call -> this.userRepository.findByPublicId(self.publicId).orElseThrow().rank);
        final UserDto promote = userDto(ADMIN_RANK_PUBLIC_ID);
        promote.username = self.username;
        // A password on one's own account is refused before the rank is checked
        promote.password = null;

        assertRefused(() -> this.userService.updateUser(self.publicId, promote));
    }

    // Ranks above the caller

    @Test
    @DisplayName("Creating a rank with a permission the caller lacks is refused")
    @TestTransaction
    void createRankBeyondCallerIsRefused() {
        this.actAsManager();
        final UserRankDto dto = rankDto(true);
        dto.aiConfigEdit = true;

        assertRefused(() -> this.userRankService.createRank(dto));
    }

    @Test
    @DisplayName("Granting the caller's own rank a permission it lacks is refused (update)")
    @TestTransaction
    void updateOwnRankBeyondCallerIsRefused() {
        this.actAsManager();
        final UserRankDto dto = rankDto(true);
        dto.name = this.callerRank.name;
        dto.aiConfigEdit = true;

        assertRefused(() -> this.userRankService.updateRank(this.callerRank.publicId, dto));
    }

    @Test
    @DisplayName("Granting the caller's own rank a permission it lacks is refused (patch)")
    @TestTransaction
    void patchOwnRankBeyondCallerIsRefused() {
        this.actAsManager();
        final UserRankDto dto = new UserRankDto();
        dto.lessonAdd = true;

        assertRefused(() -> this.userRankService.patchRank(this.callerRank.publicId, dto));
    }

    @Test
    @DisplayName("Editing a higher rank is refused, even without granting anything")
    @TestTransaction
    void editHigherRankIsRefused() {
        this.actAsManager();
        final UserRankDto strip = new UserRankDto();
        strip.aiConfigEdit = false;

        assertRefused(() -> this.userRankService.patchRank(ADMIN_RANK_PUBLIC_ID, strip));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "updateRank", "patchRank" })
    @DisplayName("Lowering a higher rank to exactly the caller's permissions is refused")
    @TestTransaction
    void lowerHigherRankToCallerLevelIsRefused(final String method) {
        this.actAsManager();
        // After either edit the Admin rank holds exactly the caller's permissions, so only the check of the rank
        // before the edit can refuse
        final UserRankDto dto;
        if ("updateRank".equals(method)) {
            dto = rankDto(true);
            dto.name = this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow().name;
        } else {
            dto = withoutPermissionsBeyondCaller();
        }

        assertRefused("updateRank".equals(method) ? () -> this.userRankService.updateRank(ADMIN_RANK_PUBLIC_ID, dto)
                : () -> this.userRankService.patchRank(ADMIN_RANK_PUBLIC_ID, dto));
    }

    @Test
    @DisplayName("Deleting a higher rank is refused")
    @TestTransaction
    void deleteHigherRankIsRefused() {
        final UserRankDto dto = rankDto(true);
        dto.aiConfigEdit = true;
        final UserRankViewDto higher = this.userRankService.createRank(dto);
        this.actAsManager();

        assertRefused(() -> this.userRankService.deleteRank(higher.publicId));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "createUser", "createRank" })
    @DisplayName("Without a current user rank, user and rank writes are refused")
    @TestTransaction
    void noCurrentRankIsRefused(final String method) {
        final String lowerRank = this.userRankService.createRank(rankDto(false)).publicId;
        when(this.permissionService.findCurrentUserRank()).thenReturn(null);

        assertThrows(PermissionDeniedException.class,
                "createUser".equals(method) ? () -> this.userService.createUser(userDto(lowerRank))
                        : () -> this.userRankService.createRank(rankDto(false)));
    }

    // Equal or lower level

    @Test
    @DisplayName("Users with the caller's rank or a lower one can be created, edited and deleted")
    @TestTransaction
    void equalAndLowerUsersAreAllowed() {
        this.actAsManager();
        final String lowerRank = this.userRankService.createRank(rankDto(false)).publicId;

        final UserViewDto peer = this.userService.createUser(userDto(this.callerRank.publicId));
        final UserViewDto lower = this.userService.createUser(userDto(lowerRank));
        final UserDto demote = new UserDto();
        demote.rankPublicId = lowerRank;
        final UserDto promote = userDto(this.callerRank.publicId);
        promote.username = lower.username;
        promote.activated = true;

        assertDoesNotThrow(() -> this.userService.patchUser(peer.publicId, demote));
        assertDoesNotThrow(() -> this.userService.updateUser(lower.publicId, promote));
        // lower now holds the caller's rank, peer a lower one
        assertDoesNotThrow(() -> this.userService.deleteUser(lower.publicId));
        assertDoesNotThrow(() -> this.userService.deleteUser(peer.publicId));
    }

    @Test
    @DisplayName("Ranks with the caller's permissions or fewer can be created, edited and deleted")
    @TestTransaction
    void equalAndLowerRanksAreAllowed() {
        this.actAsManager();
        final UserRankViewDto same = this.userRankService.createRank(rankDto(true));
        final UserRankViewDto lower = this.userRankService.createRank(rankDto(false));
        final UserRankDto rename = new UserRankDto();
        rename.name = "Renamed_" + UUID.randomUUID().toString().substring(0, 8);
        final UserRankDto narrow = rankDto(false);
        narrow.name = same.name;

        assertDoesNotThrow(() -> this.userRankService.patchRank(this.callerRank.publicId, rename));
        assertDoesNotThrow(() -> this.userRankService.updateRank(same.publicId, narrow));
        assertDoesNotThrow(() -> this.userRankService.deleteRank(lower.publicId));
    }

    /** Creates the caller's rank while still acting as admin, then makes it the current user's rank. */
    private void actAsManager() {
        this.callerRank = this.userRankService.createRank(rankDto(true));
        final UserRankEntity rank = this.userRankRepository.findByPublicId(this.callerRank.publicId).orElseThrow();
        when(this.permissionService.findCurrentUserRank()).thenReturn(rank);
    }

    /**
     * A rank DTO with the caller's permissions ({@code manager}) or a subset of them: the admin view and user editing
     * only.
     */
    private static UserRankDto rankDto(final boolean manager) {
        final UserRankDto dto = new UserRankDto();
        dto.name = "Rank_" + UUID.randomUUID().toString().substring(0, 8);
        dto.adminView = true;
        dto.userEdit = true;
        dto.userAdd = manager;
        dto.userDelete = manager;
        dto.userRankAdd = manager;
        dto.userRankEdit = manager;
        dto.userRankDelete = manager;
        return dto;
    }

    /** A PATCH that switches off every permission the seeded Admin rank holds beyond the caller's rank. */
    private static UserRankDto withoutPermissionsBeyondCaller() {
        final UserRankDto dto = new UserRankDto();
        dto.exerciseAdd = false;
        dto.exerciseDelete = false;
        dto.exerciseEdit = false;
        dto.lessonAdd = false;
        dto.lessonDelete = false;
        dto.lessonEdit = false;
        dto.commentAdd = false;
        dto.commentDelete = false;
        dto.commentEdit = false;
        dto.userGroupAdd = false;
        dto.userGroupDelete = false;
        dto.userGroupEdit = false;
        dto.aiConfigEdit = false;
        return dto;
    }

    private static UserDto userDto(final String rankPublicId) {
        final UserDto dto = new UserDto();
        dto.username = "ceiling_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = VALID_PASSWORD;
        dto.rankPublicId = rankPublicId;
        return dto;
    }

    private static void assertRefused(final Executable change) {
        final var e = assertThrows(ValidationException.class, change);
        assertEquals(AppConstants.PRIVILEGE_CEILING_MESSAGE, e.getMessage());
    }
}
