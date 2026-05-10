package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserGroupEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link UserGroupRepository}.
 */
@QuarkusTest
public class UserGroupRepositoryIT {

    @Inject
    UserGroupRepository userGroupRepository;

    private UserGroupEntity createGroup(final String suffix) {
        final UserGroupEntity group = new UserGroupEntity();
        group.name = "Group_" + suffix;
        this.userGroupRepository.persist(group);
        return group;
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsGroups() {
        final UserGroupEntity group = this.createGroup("fall");
        final List<UserGroupEntity> all = this.userGroupRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
        Assertions.assertTrue(all.stream().anyMatch(g -> Objects.equals(g.id, group.id)));
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final UserGroupEntity group = this.createGroup("fido");
        final Optional<UserGroupEntity> found =
                this.userGroupRepository.findByIdOptional(Objects.requireNonNull(group.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(group.name, found.get().name);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByIdOptional_notFound() {
        Assertions.assertTrue(this.userGroupRepository.findByIdOptional(999_999L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserGroupEntity group = this.createGroup("fpub");
        final Optional<UserGroupEntity> found =
                this.userGroupRepository.findByPublicId(Objects.requireNonNull(group.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(group.publicId, found.get().publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.userGroupRepository.findByPublicId(null).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_notFound() {
        Assertions.assertTrue(this.userGroupRepository.findByPublicId("nonexistent").isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final UserGroupEntity group = this.createGroup("fid");
        final UserGroupEntity found = this.userGroupRepository.findById(Objects.requireNonNull(group.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(group.name, found.name);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindById_null() {
        Assertions.assertNull(this.userGroupRepository.findById(null));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindById_notFound() {
        Assertions.assertNull(this.userGroupRepository.findById(999_999L));
    }

    @Test
    @TestTransaction
    public void testFindByName_found() {
        final UserGroupEntity group = this.createGroup("fnam");
        final UserGroupEntity found = this.userGroupRepository.findByName(Objects.requireNonNull(group.name));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(group.name, found.name);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByName_notFound() {
        Assertions.assertNull(this.userGroupRepository.findByName("NonexistentGroup_" + System.currentTimeMillis()));
    }

    @Test
    @TestTransaction
    public void testSearch_returnsMatchingGroups() {
        final UserGroupEntity group = this.createGroup("srch");
        final List<UserGroupEntity> result =
                this.userGroupRepository.search(Objects.requireNonNull(group.name).toLowerCase(Locale.ROOT));
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(g -> Objects.equals(g.id, group.id)));
    }

    @Test
    @TestTransaction
    public void testPersist_group() {
        final UserGroupEntity group = new UserGroupEntity();
        group.name = "NewGroup_" + System.currentTimeMillis();
        this.userGroupRepository.persist(group);
        Assertions.assertNotNull(group.id);
        Assertions.assertNotNull(group.publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testPersist_null() {
        Assertions.assertDoesNotThrow(() -> this.userGroupRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDeleteById_existing() {
        final UserGroupEntity group = this.createGroup("delid");
        final Long id = Objects.requireNonNull(group.id);
        Assertions.assertTrue(this.userGroupRepository.deleteById(id));
        Assertions.assertNull(this.userGroupRepository.findById(id));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertFalse(this.userGroupRepository.deleteById(999_999L));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existing() {
        final UserGroupEntity group = this.createGroup("delpi");
        final String publicId = Objects.requireNonNull(group.publicId);
        Assertions.assertTrue(this.userGroupRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.userGroupRepository.findByPublicId(publicId).isEmpty());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDeleteByPublicId_nonExisting() {
        Assertions.assertFalse(this.userGroupRepository.deleteByPublicId("nonexistent"));
    }
}
