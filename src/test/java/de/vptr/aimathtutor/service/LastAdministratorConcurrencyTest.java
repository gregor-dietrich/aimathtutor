package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.PermissionService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestUserRankFactory;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Removes each of the last two active administrators in its own transaction at the same time. Concurrent transactions
 * can't share a test transaction, so each test commits its setup and restores it afterwards.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
class LastAdministratorConcurrencyTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @InjectMock
    PermissionService permissionService;

    @BeforeEach
    @Transactional
    void setUp() {
        // The caller is an administrator, so the privilege ceiling never applies
        when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
    }

    @Test
    @DisplayName("Concurrent bans of the last two active administrators leave one")
    void concurrentBansLeaveOneAdministrator() throws InterruptedException {
        final var ban = new UserDto();
        ban.banned = true;
        this.assertOneAdministratorRemains(List.of(ADMIN_RANK_PUBLIC_ID, ADMIN_RANK_PUBLIC_ID),
                (user, rank) -> this.userService.patchUser(user, ban));
    }

    @Test
    @DisplayName("Concurrent edits of the two ranks holding the last active administrators leave one")
    void concurrentRankEditsLeaveOneAdministrator() throws InterruptedException {
        final List<String> ranks = QuarkusTransaction.requiringNew().call(() -> Stream
                .generate(() -> this.userRankService.createRank(TestUserRankFactory.rankWithout("none")).publicId)
                .limit(2).toList());
        try {
            final var strip = new UserRankDto();
            strip.userRankEdit = false;
            this.assertOneAdministratorRemains(ranks, (user, rank) -> this.userRankService.patchRank(rank, strip));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> ranks.forEach(this.userRankRepository::deleteByPublicId));
        }
    }

    /**
     * Commits one active administrator per rank and bans every other one, then removes those administrators in
     * concurrent transactions. Each keeps its transaction open after its removal until the others have made theirs, for
     * up to two seconds, so without the lock all of them would pass the guard and commit.
     *
     * @param rankPublicIds
     *            the rank of each administrator
     * @param removal
     *            removes the administrator given by public ID, who holds the given rank
     */
    private void assertOneAdministratorRemains(final List<String> rankPublicIds,
            final BiConsumer<String, String> removal) throws InterruptedException {
        final List<String> banned = QuarkusTransaction.requiringNew().call(this::banActiveAdministrators);
        final List<String> admins = QuarkusTransaction.requiringNew()
                .call(() -> rankPublicIds.stream().map(this::createActiveUser).toList());
        try {
            final var removed = new CountDownLatch(admins.size());
            final List<Future<Boolean>> removals;
            try (ExecutorService executor = Executors.newFixedThreadPool(admins.size())) {
                removals = IntStream.range(0, admins.size())
                        .mapToObj(i -> executor.submit(() -> QuarkusTransaction.requiringNew().call(() -> {
                            removal.accept(admins.get(i), rankPublicIds.get(i));
                            removed.countDown();
                            return removed.await(2, TimeUnit.SECONDS);
                        }))).toList();
            }
            int refused = 0;
            for (final Future<Boolean> future : removals) {
                try {
                    assertFalse(future.get(), "Every removal was made before any of them committed");
                } catch (final ExecutionException e) {
                    assertEquals(AppConstants.LAST_ADMINISTRATOR_MESSAGE, e.getCause().getMessage());
                    refused++;
                }
            }
            assertEquals(admins.size() - 1, refused);
            assertEquals(1L, QuarkusTransaction.requiringNew().call(this.userRepository::countActiveAdministrators));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> {
                admins.forEach(this.userRepository::deleteByPublicId);
                banned.forEach(publicId -> this.userRepository.findByPublicId(publicId).orElseThrow().banned = false);
            });
        }
    }

    /** Bans every active administrator and returns their public IDs. */
    private List<String> banActiveAdministrators() {
        final List<UserEntity> admins =
                this.userRepository.findAll().stream().filter(UserRepository::isActiveAdministrator).toList();
        admins.forEach(user -> user.banned = true);
        return admins.stream().map(user -> user.publicId).toList();
    }

    private String createActiveUser(final String rankPublicId) {
        final var dto = new UserDto();
        dto.username = "concurrent_" + UUID.randomUUID().toString().substring(0, 8);
        dto.password = "P@ssw0rd1";
        dto.rankPublicId = rankPublicId;
        dto.activated = true;
        return this.userService.createUser(dto).publicId;
    }
}
