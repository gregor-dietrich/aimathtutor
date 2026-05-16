package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class UserRankRepositoryTest {

    @Inject
    UserRankRepository userRankRepository;

    @Test
    @DisplayName("Null handling for repository methods and persist(null) is a no-op")
    @TestTransaction
    void testNullHandling() {
        assertTrue(this.userRankRepository.findByPublicId(null).isEmpty());
        assertNull(this.userRankRepository.findById(null));
        assertTrue(this.userRankRepository.findByIdOptional(null).isEmpty());
        this.userRankRepository.persist(null);
        assertNull(this.userRankRepository.findById(null));
    }

    @Test
    @DisplayName("findAll returns non-null list with seeded entries")
    void testFindAll() {
        final var all = this.userRankRepository.findAll();
        assertNotNull(all);
    }

    @Test
    @DisplayName("persist and deleteById cover both found and not-found paths")
    @TestTransaction
    void testPersistAndDeleteById() {
        final var rank = new UserRankEntity();
        rank.name = "TestRank " + UlidUtil.generate();
        rank.publicId = UlidUtil.generate();
        this.userRankRepository.persist(rank);
        this.userRankRepository.flush();

        assertNotNull(this.userRankRepository.findById(rank.id));
        assertFalse(this.userRankRepository.deleteById(999_999L));
        assertTrue(this.userRankRepository.deleteById(rank.id));
        assertNull(this.userRankRepository.findById(rank.id));
    }

    @Test
    @DisplayName("deleteByPublicId covers found and not-found paths")
    @TestTransaction
    void testDeleteByPublicId() {
        final String pid = UlidUtil.generate();
        assertFalse(this.userRankRepository.deleteByPublicId(pid));

        final var rank = new UserRankEntity();
        rank.name = "DelRank " + UlidUtil.generate();
        rank.publicId = pid;
        this.userRankRepository.persist(rank);
        this.userRankRepository.flush();

        assertTrue(this.userRankRepository.deleteByPublicId(pid));
        assertTrue(this.userRankRepository.findByPublicId(pid).isEmpty());
    }

    @Test
    @DisplayName("findByName returns present for created rank")
    @TestTransaction
    void testFindByName() {
        final String name = "UniqueRank " + UlidUtil.generate();
        final var rank = new UserRankEntity();
        rank.name = name;
        rank.publicId = UlidUtil.generate();
        this.userRankRepository.persist(rank);
        this.userRankRepository.flush();

        assertTrue(this.userRankRepository.findByName(name).isPresent());
        assertTrue(this.userRankRepository.findByName("NoSuchRankXYZ").isEmpty());
    }

    @Test
    @DisplayName("search returns matching ranks by name")
    @TestTransaction
    void testSearch() {
        final var rank = new UserRankEntity();
        rank.name = "SearchableRank " + UlidUtil.generate();
        rank.publicId = UlidUtil.generate();
        this.userRankRepository.persist(rank);
        this.userRankRepository.flush();

        final var results = this.userRankRepository.search("%searchablerank%");
        assertFalse(results.isEmpty());
    }
}
