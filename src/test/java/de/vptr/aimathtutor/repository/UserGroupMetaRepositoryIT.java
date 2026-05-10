package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserGroupEntity;
import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link UserGroupMetaRepository}.
 */
@QuarkusTest
public class UserGroupMetaRepositoryIT extends AbstractRepositoryIT {

    @Inject
    UserGroupMetaRepository userGroupMetaRepository;

    @Inject
    UserGroupRepository userGroupRepository;

    private UserGroupMetaEntity createMeta(final String suffix) {
        final UserEntity user = this.createUser(suffix, "ugmuser_");

        final UserGroupEntity group = new UserGroupEntity();
        group.name = "Group_" + suffix;
        this.userGroupRepository.persist(group);

        final UserGroupMetaEntity meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;
        this.userGroupMetaRepository.persist(meta);
        return meta;
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final UserGroupMetaEntity meta = this.createMeta("ugmpub");
        final Optional<UserGroupMetaEntity> found =
                this.userGroupMetaRepository.findByPublicId(Objects.requireNonNull(meta.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(meta.publicId, found.get().publicId);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.userGroupMetaRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserPublicId_returnsMetas() {
        final UserGroupMetaEntity meta = this.createMeta("ugmupi");
        final String userPublicId = Objects.requireNonNull(Objects.requireNonNull(meta.user).publicId);
        final List<UserGroupMetaEntity> result = this.userGroupMetaRepository.findByUserPublicId(userPublicId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(m -> Objects.equals(m.id, meta.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByUserPublicId_null() {
        Assertions.assertTrue(this.userGroupMetaRepository.findByUserPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByGroupPublicId_returnsMetas() {
        final UserGroupMetaEntity meta = this.createMeta("ugmgpi");
        final String groupPublicId = Objects.requireNonNull(Objects.requireNonNull(meta.group).publicId);
        final List<UserGroupMetaEntity> result = this.userGroupMetaRepository.findByGroupPublicId(groupPublicId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(m -> Objects.equals(m.id, meta.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByGroupPublicId_null() {
        Assertions.assertTrue(this.userGroupMetaRepository.findByGroupPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByGroupIdWithUsers_returnsMetas() {
        final UserGroupMetaEntity meta = this.createMeta("ugmgiwu");
        final UserGroupEntity group = Objects.requireNonNull(meta.group);
        final List<UserGroupMetaEntity> result =
                this.userGroupMetaRepository.findByGroupIdWithUsers(Objects.requireNonNull(group.id));
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(m -> Objects.equals(m.id, meta.id)));
        Assertions.assertNotNull(result.get(0).user);
        Assertions.assertNotNull(result.get(0).user.username);
    }

    @Test
    @TestTransaction
    public void testFindByGroupPublicIdWithUsers_returnsMetas() {
        final UserGroupMetaEntity meta = this.createMeta("ugmgpiwu");
        final String groupPublicId = Objects.requireNonNull(Objects.requireNonNull(meta.group).publicId);
        final List<UserGroupMetaEntity> result =
                this.userGroupMetaRepository.findByGroupPublicIdWithUsers(groupPublicId);
        Assertions.assertEquals(1, result.size());
        Assertions.assertNotNull(result.get(0).user);
        Assertions.assertNotNull(result.get(0).user.username);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByGroupPublicIdWithUsers_null() {
        Assertions.assertTrue(this.userGroupMetaRepository.findByGroupPublicIdWithUsers(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserId_returnsMetas() {
        final UserGroupMetaEntity meta = this.createMeta("ugmbyui");
        final UserEntity user = Objects.requireNonNull(meta.user);
        final List<UserGroupMetaEntity> result =
                this.userGroupMetaRepository.findByUserId(Objects.requireNonNull(user.id));
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(m -> Objects.equals(m.id, meta.id)));
    }

    @Test
    @TestTransaction
    public void testFindByUserAndGroup_found() {
        final UserGroupMetaEntity meta = this.createMeta("ugmfuag");
        final UserEntity user = Objects.requireNonNull(meta.user);
        final UserGroupEntity group = Objects.requireNonNull(meta.group);
        final UserGroupMetaEntity found = this.userGroupMetaRepository
                .findByUserAndGroup(Objects.requireNonNull(user.id), Objects.requireNonNull(group.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(meta.id, found.id);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByUserAndGroup_notFound() {
        Assertions.assertNull(this.userGroupMetaRepository.findByUserAndGroup(999_999L, 999_999L));
    }

    @Test
    @TestTransaction
    public void testFindByUserPublicIdAndGroupPublicId_found() {
        final UserGroupMetaEntity meta = this.createMeta("ugmfupgp");
        final UserEntity user = Objects.requireNonNull(meta.user);
        final UserGroupEntity group = Objects.requireNonNull(meta.group);
        final UserGroupMetaEntity found = this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(
                Objects.requireNonNull(user.publicId), Objects.requireNonNull(group.publicId));
        Assertions.assertNotNull(found);
        Assertions.assertEquals(meta.id, found.id);
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByUserPublicIdAndGroupPublicId_null() {
        Assertions.assertNull(this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(null, "g"));
        Assertions.assertNull(this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId("u", null));
        Assertions.assertNull(this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(null, null));
    }

    @Test
    @TestTransaction
    public void testIsUserInGroup_true() {
        final UserGroupMetaEntity meta = this.createMeta("ugmiugt");
        final UserEntity user = Objects.requireNonNull(meta.user);
        final UserGroupEntity group = Objects.requireNonNull(meta.group);
        Assertions.assertTrue(this.userGroupMetaRepository.isUserInGroup(Objects.requireNonNull(user.id),
                Objects.requireNonNull(group.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testIsUserInGroup_false() {
        Assertions.assertFalse(this.userGroupMetaRepository.isUserInGroup(999_999L, 999_999L));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testPersist_null() {
        Assertions.assertDoesNotThrow(() -> this.userGroupMetaRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testDelete_existing() {
        final UserGroupMetaEntity meta = this.createMeta("ugmdel");
        final UserEntity user = Objects.requireNonNull(meta.user);
        final UserGroupEntity group = Objects.requireNonNull(meta.group);
        this.userGroupMetaRepository.delete(meta);
        Assertions.assertNull(this.userGroupMetaRepository.findByUserAndGroup(Objects.requireNonNull(user.id),
                Objects.requireNonNull(group.id)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testDelete_null() {
        Assertions.assertDoesNotThrow(() -> this.userGroupMetaRepository.delete(null));
    }
}
