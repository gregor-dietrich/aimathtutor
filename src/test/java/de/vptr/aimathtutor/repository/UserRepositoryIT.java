package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link UserRepository}.
 */
@QuarkusTest
public class UserRepositoryIT {

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    private UserEntity createUser(final String suffix) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "Rank_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = "usr_" + suffix;
        user.password = "pw";
        user.email = "usr_" + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final UserEntity user = this.createUser("fid");
        final UserEntity found = this.userRepository.findById(Objects.requireNonNull(user.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(user.username, found.username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindById_null() {
        Assertions.assertNull(this.userRepository.findById(null));
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final UserEntity user = this.createUser("fido");
        final Optional<UserEntity> found = this.userRepository.findByIdOptional(Objects.requireNonNull(user.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(user.username, found.get().username);
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_notFound() {
        Assertions.assertTrue(this.userRepository.findByIdOptional(999_999L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserEntity user = this.createUser("fpub");
        final Optional<UserEntity> found = this.userRepository.findByPublicId(Objects.requireNonNull(user.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(user.username, found.get().username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.userRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUsernameOptional_found() {
        final UserEntity user = this.createUser("funo");
        final Optional<UserEntity> found =
                this.userRepository.findByUsernameOptional(Objects.requireNonNull(user.username));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(user.username, found.get().username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByUsernameOptional_null() {
        Assertions.assertTrue(this.userRepository.findByUsernameOptional(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUsername_found() {
        final UserEntity user = this.createUser("fun");
        final UserEntity found = this.userRepository.findByUsername(Objects.requireNonNull(user.username));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(user.username, found.username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByUsername_null() {
        Assertions.assertNull(this.userRepository.findByUsername(null));
    }

    @Test
    @TestTransaction
    public void testFindByEmailOptional_found() {
        final UserEntity user = this.createUser("femail");
        final Optional<UserEntity> found = this.userRepository.findByEmailOptional(Objects.requireNonNull(user.email));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(user.username, found.get().username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByEmailOptional_null() {
        Assertions.assertTrue(this.userRepository.findByEmailOptional(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsUsers() {
        final UserEntity user = this.createUser("fall");
        final List<UserEntity> all = this.userRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
        Assertions.assertTrue(all.stream().anyMatch(u -> Objects.equals(u.username, user.username)));
    }

    @Test
    @TestTransaction
    public void testFindActiveUsers_excludesBanned() {
        this.createUser("actv");

        final UserEntity banned = new UserEntity();
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "Rank_banned";
        this.userRankRepository.persist(rank);
        banned.username = "usr_banned_" + UUID.randomUUID();
        banned.password = "pw";
        banned.email = "banned@example.com";
        banned.activated = true;
        banned.banned = true;
        banned.rank = rank;
        this.userRepository.persist(banned);

        final List<UserEntity> active = this.userRepository.findActiveUsers();
        Assertions.assertTrue(active.stream().allMatch(u -> u.activated && !u.banned));
    }

    @Test
    @TestTransaction
    public void testFindByRankId_returnsUsersWithRank() {
        final UserEntity user = this.createUser("frank");
        final UserRankEntity rank = Objects.requireNonNull(user.rank);
        final List<UserEntity> ranked = this.userRepository.findByRankId(Objects.requireNonNull(rank.id));
        Assertions.assertFalse(ranked.isEmpty());
        Assertions
                .assertTrue(ranked.stream().allMatch(u -> Objects.equals(Objects.requireNonNull(u.rank).id, rank.id)));
    }

    @Test
    @TestTransaction
    public void testCountByRankId_returnsCount() {
        final UserEntity user = this.createUser("cntrk");
        final UserRankEntity rank = Objects.requireNonNull(user.rank);
        final long count = this.userRepository.countByRankId(Objects.requireNonNull(rank.id));
        Assertions.assertTrue(count >= 1);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testCountByRankId_null() {
        Assertions.assertEquals(0L, this.userRepository.countByRankId(null));
    }

    @Test
    @TestTransaction
    public void testCountByRankPublicId_returnsCount() {
        final UserEntity user = this.createUser("cntrpi");
        final UserRankEntity rank = Objects.requireNonNull(user.rank);
        final long count = this.userRepository.countByRankPublicId(Objects.requireNonNull(rank.publicId));
        Assertions.assertTrue(count >= 1);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testCountByRankPublicId_null() {
        Assertions.assertEquals(0L, this.userRepository.countByRankPublicId(null));
    }

    @Test
    @TestTransaction
    public void testCountAll_returnsCount() {
        this.createUser("cntal");
        Assertions.assertTrue(this.userRepository.countAll() >= 1);
    }

    @Test
    @TestTransaction
    public void testSearch_byUsername() {
        final UserEntity user = this.createUser("srch");
        final List<UserEntity> result = this.userRepository.search(Objects.requireNonNull(user.username));
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(u -> Objects.equals(u.username, user.username)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testSearch_nullReturnsAll() {
        Assertions.assertFalse(this.userRepository.search(null).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testSearch_blankReturnsAll() {
        Assertions.assertFalse(this.userRepository.search("").isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingUser() {
        final UserEntity user = this.createUser("delid");
        final Long id = Objects.requireNonNull(user.id);
        Assertions.assertTrue(this.userRepository.deleteById(id));
        Assertions.assertNull(this.userRepository.findById(id));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertFalse(this.userRepository.deleteById(999_999L));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existingUser() {
        final UserEntity user = this.createUser("delpi");
        final String publicId = Objects.requireNonNull(user.publicId);
        Assertions.assertTrue(this.userRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.userRepository.findByPublicId(publicId).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDeleteByPublicId_nonExisting() {
        Assertions.assertFalse(this.userRepository.deleteByPublicId("nonexistent"));
    }
}
