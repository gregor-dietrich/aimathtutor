package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link UserRankRepository}.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
public class UserRankRepositoryIT {

    @Inject
    UserRankRepository userRankRepository;

    private UserRankEntity createRank(final String name) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = name;
        this.userRankRepository.persist(rank);
        return rank;
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsRanks() {
        this.createRank("Rank_fall");
        final List<UserRankEntity> all = this.userRankRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final UserRankEntity rank = this.createRank("Rank_fid");
        final UserRankEntity found = this.userRankRepository.findById(Objects.requireNonNull(rank.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals("Rank_fid", found.name);
    }

    @Test
    @TestTransaction
    public void testFindById_null() {
        Assertions.assertNull(this.userRankRepository.findById(null));
    }

    @Test
    @TestTransaction
    public void testFindById_notFound() {
        Assertions.assertNull(this.userRankRepository.findById(999_999L));
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final UserRankEntity rank = this.createRank("Rank_fido");
        final var found = this.userRankRepository.findByIdOptional(Objects.requireNonNull(rank.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("Rank_fido", found.get().name);
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_notFound() {
        Assertions.assertTrue(this.userRankRepository.findByIdOptional(999_999L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserRankEntity rank = this.createRank("Rank_fpub");
        final var found = this.userRankRepository.findByPublicId(Objects.requireNonNull(rank.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(rank.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.userRankRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_notFound() {
        Assertions.assertTrue(this.userRankRepository.findByPublicId("nonexistent-public-id").isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByName_found() {
        final UserRankEntity rank = this.createRank("UniqueRankName_IT");
        final var found = this.userRankRepository.findByName(Objects.requireNonNull(rank.name));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("UniqueRankName_IT", found.get().name);
    }

    @Test
    @TestTransaction
    public void testFindByName_notFound() {
        Assertions.assertTrue(this.userRankRepository.findByName("nonexistent_rank_xyz").isEmpty());
    }

    @Test
    @TestTransaction
    public void testSearch_returnsMatchingRanks() {
        this.createRank("SearchableRank_IT");
        final List<UserRankEntity> results = this.userRankRepository.search("%searchablerank%");
        Assertions.assertFalse(results.isEmpty());
        Assertions.assertTrue(results.stream().anyMatch(r -> "SearchableRank_IT".equals(r.name)));
    }

    @Test
    @TestTransaction
    public void testPersist_setsIdAndPublicId() {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "PersistTestRank_IT";
        this.userRankRepository.persist(rank);
        Assertions.assertNotNull(rank.id);
        Assertions.assertNotNull(rank.publicId);
    }

    @Test
    @TestTransaction
    public void testPersist_null_doesNotThrow() {
        Assertions.assertDoesNotThrow(() -> this.userRankRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingRank() {
        final UserRankEntity rank = this.createRank("Rank_delid");
        final Long id = Objects.requireNonNull(rank.id);
        Assertions.assertTrue(this.userRankRepository.deleteById(id));
        Assertions.assertNull(this.userRankRepository.findById(id));
    }

    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertFalse(this.userRankRepository.deleteById(999_999L));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existingRank() {
        final UserRankEntity rank = this.createRank("Rank_delpub");
        final String publicId = Objects.requireNonNull(rank.publicId);
        Assertions.assertTrue(this.userRankRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.userRankRepository.findByPublicId(publicId).isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_nonExisting() {
        Assertions.assertFalse(this.userRankRepository.deleteByPublicId("nonexistent"));
    }
}
