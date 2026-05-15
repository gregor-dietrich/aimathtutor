package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class UserRepositoryTest {

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Test
    @DisplayName("findByEmailOptional should find user by blind index")
    @TestTransaction
    void testFindByEmailOptional() {
        final var rank = this.userRankRepository.findAll().get(0);
        final String email = "test_" + UlidUtil.generate() + "@example.com";

        final var user = new UserEntity();
        user.username = "user_" + UlidUtil.generate().substring(0, 8);
        user.email = email;
        user.password = "password";
        user.rank = rank;
        user.publicId = UlidUtil.generate();

        this.userRepository.persist(user);
        this.userRepository.flush();

        final var found = this.userRepository.findByEmailOptional(email);
        assertTrue(found.isPresent());
        assertEquals(user.username, found.get().username);
        assertNotNull(found.get().emailBlindIndex);
    }

    @Test
    @DisplayName("countByRankPublicId should count users with rank")
    @TestTransaction
    void testCountByRankPublicId() {
        final var rank = this.userRankRepository.findAll().get(0);
        final long initialCount = this.userRepository.countByRankPublicId(rank.publicId);

        final var user = new UserEntity();
        user.username = "user_rank_test";
        user.password = "password";
        user.rank = rank;
        user.publicId = UlidUtil.generate();
        this.userRepository.persist(user);
        this.userRepository.flush();

        assertEquals(initialCount + 1, this.userRepository.countByRankPublicId(rank.publicId));
    }

    @Test
    @DisplayName("search with email should trigger blind index lookup")
    @TestTransaction
    void testSearchByEmail() {
        final var rank = this.userRankRepository.findAll().get(0);
        final String email = "search_" + UlidUtil.generate() + "@example.com";

        final var user = new UserEntity();
        user.username = "search_user";
        user.email = email;
        user.password = "password";
        user.rank = rank;
        user.publicId = UlidUtil.generate();
        this.userRepository.persist(user);
        this.userRepository.flush();

        final var results = this.userRepository.search(email);
        assertEquals(1, results.size());
        assertEquals("search_user", results.get(0).username);
    }

    @Test
    @DisplayName("deleteByPublicId should remove the user")
    @TestTransaction
    void testDeleteByPublicId() {
        final var rank = this.userRankRepository.findAll().get(0);
        final var user = new UserEntity();
        user.username = "to_delete";
        user.password = "password";
        user.rank = rank;
        user.publicId = UlidUtil.generate();
        this.userRepository.persist(user);
        this.userRepository.flush();

        assertTrue(this.userRepository.findByPublicId(user.publicId).isPresent());
        assertTrue(this.userRepository.deleteByPublicId(user.publicId));
        assertFalse(this.userRepository.findByPublicId(user.publicId).isPresent());
    }

    @Test
    @DisplayName("Null handling in repository methods")
    void testNullHandling() {
        assertNull(this.userRepository.findById(null));
        assertFalse(this.userRepository.findByPublicId(null).isPresent());
        assertFalse(this.userRepository.findByUsernameOptional(null).isPresent());
        assertFalse(this.userRepository.findByEmailOptional(null).isPresent());
        assertNull(this.userRepository.persist(null));
        assertEquals(0L, this.userRepository.countByRankId(null));
        assertEquals(0L, this.userRepository.countByRankPublicId(null));
        assertFalse(this.userRepository.deleteById(null));
        assertFalse(this.userRepository.deleteByPublicId(null));
    }
}
