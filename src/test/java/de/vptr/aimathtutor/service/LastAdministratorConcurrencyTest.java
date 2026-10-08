package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestUserRankFactory;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

/**
 * Removes each of the last two active administrators in its own transaction at the same time. Concurrent transactions
 * can't share a test transaction, so each test commits its setup and restores it afterwards.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
class LastAdministratorConcurrencyTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    // No user or rank has this ID
    private static final String UNKNOWN_PUBLIC_ID = "00000000000000000000000000";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @InjectSpy
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    EntityManager em;

    @InjectMock
    PermissionService permissionService;

    @BeforeEach
    @Transactional
    void setUp() {
        // The caller is an administrator, so the privilege ceiling never applies
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = { "patchUser", "updateUser", "deleteUser", "patchRank", "updateRank", "patchUser+patchRank" })
    @DisplayName("Concurrent removals of the last two active administrators leave one")
    void concurrentRemovalsLeaveOneAdministrator(final String methods) throws InterruptedException {
        final var banned = new ArrayList<String>();
        final var ranks = new ArrayList<String>();
        final var admins = new ArrayList<UserViewDto>();
        try {
            banned.addAll(QuarkusTransaction.requiringNew().call(this::banActiveAdministrators));
            ranks.addAll(QuarkusTransaction.requiringNew().call(() -> Stream
                    .generate(() -> this.userRankService.createRank(TestUserRankFactory.rankWithout("none")).publicId)
                    .limit(2).toList()));
            admins.addAll(
                    QuarkusTransaction.requiringNew().call(() -> ranks.stream().map(this::createActiveUser).toList()));
            final var removals = methods.contains("+") ? Stream.of(methods.split("\\+")).map(this::removal).toList()
                    : List.of(this.removal(methods), this.removal(methods));
            this.assertOneAdministratorRemains(admins, removals);
        } finally {
            QuarkusTransaction.requiringNew().run(() -> banned
                    .forEach(publicId -> this.userRepository.findByPublicId(publicId).orElseThrow().banned = false));
            QuarkusTransaction.requiringNew()
                    .run(() -> admins.forEach(admin -> this.userRepository.deleteByPublicId(admin.publicId)));
            QuarkusTransaction.requiringNew().run(() -> ranks.forEach(this.userRankRepository::deleteByPublicId));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "patchUser", "updateUser", "deleteUser", "patchRank", "updateRank" })
    @DisplayName("Every write path that can remove an administrator locks before its permission check")
    @TestTransaction
    void locksBeforePermissionCheck(final String method) {
        final var order = inOrder(this.userRepository, this.permissionService);
        final var update = new UserDto();
        update.username = "unknown";
        switch (method) {
            case "patchUser" -> assertThrows(WebApplicationException.class,
                    () -> this.userService.patchUser(UNKNOWN_PUBLIC_ID, new UserDto()));
            case "updateUser" -> assertThrows(WebApplicationException.class,
                    () -> this.userService.updateUser(UNKNOWN_PUBLIC_ID, update));
            case "deleteUser" -> assertFalse(this.userService.deleteUser(UNKNOWN_PUBLIC_ID));
            case "patchRank" -> assertThrows(WebApplicationException.class,
                    () -> this.userRankService.patchRank(UNKNOWN_PUBLIC_ID, new UserRankDto()));
            default -> assertThrows(WebApplicationException.class,
                    () -> this.userRankService.updateRank(UNKNOWN_PUBLIC_ID, new UserRankDto("unknown")));
        }
        order.verify(this.userRepository).lockAdministrators();
        switch (method) {
            case "patchUser", "updateUser" -> order.verify(this.permissionService).requireUserEdit();
            case "deleteUser" -> order.verify(this.permissionService).requireUserDelete();
            default -> order.verify(this.permissionService).requireUserRankEdit();
        }
    }

    /**
     * Builds the call that removes an administrator: the user change bans or deactivates them, or deletes them, and the
     * rank change takes {@code userRankEdit} from their rank.
     */
    private BiConsumer<UserViewDto, String> removal(final String method) {
        return switch (method) {
            case "patchUser" -> (admin, rank) -> {
                final var ban = new UserDto();
                ban.banned = true;
                this.userService.patchUser(admin.publicId, ban);
            };
            case "updateUser" -> (admin, rank) -> {
                final var deactivate = new UserDto();
                deactivate.username = admin.username;
                deactivate.rankPublicId = rank;
                this.userService.updateUser(admin.publicId, deactivate);
            };
            case "deleteUser" -> (admin, rank) -> this.userService.deleteUser(admin.publicId);
            case "patchRank" -> (admin, rank) -> {
                final var strip = new UserRankDto();
                strip.userRankEdit = false;
                this.userRankService.patchRank(rank, strip);
            };
            default ->
                (admin, rank) -> this.userRankService.updateRank(rank, TestUserRankFactory.rankWithout("userRankEdit"));
        };
    }

    /**
     * Runs each removal on its administrator in its own transaction, all at once. Each keeps its transaction open after
     * its removal until the others have made theirs, which without the lock all of them would, or another waits on the
     * advisory lock.
     *
     * @param admins
     *            the committed administrators, each holding its own rank
     * @param removals
     *            the removal for each administrator, given the administrator and their rank's public ID
     */
    private void assertOneAdministratorRemains(final List<UserViewDto> admins,
            final List<BiConsumer<UserViewDto, String>> removals) throws InterruptedException {
        final var removed = new CountDownLatch(admins.size());
        final List<Future<Boolean>> results;
        try (var executor = Executors.newFixedThreadPool(admins.size())) {
            results = IntStream.range(0, admins.size())
                    .mapToObj(i -> executor.submit(() -> QuarkusTransaction.requiringNew().call(() -> {
                        removals.get(i).accept(admins.get(i), admins.get(i).rankPublicId);
                        removed.countDown();
                        return this.othersRemovedBeforeCommit(removed);
                    }))).toList();
        }
        var refused = 0;
        for (final var result : results) {
            try {
                assertFalse(result.get(), "Every removal was made before any of them committed");
            } catch (final ExecutionException e) {
                assertEquals(AppConstants.LAST_ADMINISTRATOR_MESSAGE, e.getCause().getMessage());
                refused++;
            }
        }
        assertEquals(admins.size() - 1, refused);
        assertEquals(1L, QuarkusTransaction.requiringNew().call(this.userRepository::countActiveAdministrators));
    }

    /**
     * Waits until every removal has been made, or until another one is waiting on the advisory lock.
     *
     * @return true if every removal was made, so none of them waited for another to commit
     */
    private boolean othersRemovedBeforeCommit(final CountDownLatch removed) throws InterruptedException {
        final var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            if (removed.await(20, TimeUnit.MILLISECONDS)) {
                return true;
            }
            if (((Number) this.em
                    .createNativeQuery("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted")
                    .getSingleResult()).longValue() > 0) {
                return false;
            }
        }
        throw new AssertionError("Another removal neither finished nor waited on the lock");
    }

    /** Bans every active administrator and returns their public IDs. */
    private List<String> banActiveAdministrators() {
        final var admins =
                this.userRepository.findAll().stream().filter(UserRepository::isActiveAdministrator).toList();
        admins.forEach(user -> user.banned = true);
        return admins.stream().map(user -> user.publicId).toList();
    }

    private UserViewDto createActiveUser(final String rankPublicId) {
        final var dto = new UserDto();
        dto.username = "concurrent_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = "P@ssw0rd1";
        dto.rankPublicId = rankPublicId;
        dto.activated = true;
        return this.userService.createUser(dto);
    }
}
