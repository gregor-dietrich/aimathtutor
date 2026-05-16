package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserGroupEntity;
import de.vptr.aimathtutor.entity.UserGroupMetaEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class UserGroupMetaRepositoryTest {

    @Inject
    UserGroupMetaRepository userGroupMetaRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserGroupRepository userGroupRepository;

    @Test
    @DisplayName("findByUserPublicIdAndGroupPublicId should find membership")
    @TestTransaction
    void testFindByUserPublicIdAndGroupPublicId() {
        final var user = this.userRepository.findByUsername("student1");
        final var group = this.userGroupRepository.findAll().get(0);

        createAndPersistMeta(user, group);

        final var found =
                this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(user.publicId, group.publicId);
        assertNotNull(found);
        assertEquals(user.id, found.user.id);
        assertEquals(group.id, found.group.id);
    }

    @Test
    @DisplayName("isUserInGroup should return true for members")
    @TestTransaction
    void testIsUserInGroup() {
        final var user = this.userRepository.findByUsername("student1");
        final var group = this.userGroupRepository.findAll().get(0);

        assertFalse(this.userGroupMetaRepository.isUserInGroup(user.id, group.id));

        createAndPersistMeta(user, group);

        assertTrue(this.userGroupMetaRepository.isUserInGroup(user.id, group.id));
    }

    @Test
    @DisplayName("findByGroupPublicIdWithUsers should load users")
    @TestTransaction
    void testFindByGroupPublicIdWithUsers() {
        final var user = this.userRepository.findByUsername("student1");
        final var group = this.userGroupRepository.findAll().get(0);

        createAndPersistMeta(user, group);

        final var results = this.userGroupMetaRepository.findByGroupPublicIdWithUsers(group.publicId);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(m -> "student1".equals(m.user.username)));
    }

    private UserGroupMetaEntity createAndPersistMeta(UserEntity user, UserGroupEntity group) {
        final var meta = new UserGroupMetaEntity();
        meta.user = user;
        meta.group = group;
        meta.publicId = UlidUtil.generate();
        this.userGroupMetaRepository.persist(meta);
        this.userGroupMetaRepository.flush();
        return meta;
    }

    @Test
    @DisplayName("Null handling in repository methods")
    void testNullHandling() {
        assertFalse(this.userGroupMetaRepository.findByPublicId(null).isPresent());
        assertTrue(this.userGroupMetaRepository.findByUserPublicId(null).isEmpty());
        assertTrue(this.userGroupMetaRepository.findByGroupPublicId(null).isEmpty());
        assertTrue(this.userGroupMetaRepository.findByGroupPublicIdWithUsers(null).isEmpty());
        assertNull(this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId(null, "foo"));
        assertNull(this.userGroupMetaRepository.findByUserPublicIdAndGroupPublicId("bar", null));
        this.userGroupMetaRepository.persist(null);
        this.userGroupMetaRepository.delete(null);
    }
}
