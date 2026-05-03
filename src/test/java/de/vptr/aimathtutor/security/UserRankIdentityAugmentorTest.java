package de.vptr.aimathtutor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserRankIdentityAugmentorTest {

    @Inject
    UserRankIdentityAugmentor augmentor;

    @Test
    @DisplayName("Should build roles from user rank with all permissions")
    void shouldBuildRolesFromUserRankWithAllPermissions() {
        final UserRankEntity rank = new UserRankEntity();
        rank.adminView = true;
        rank.exerciseAdd = true;
        rank.exerciseEdit = true;
        rank.exerciseDelete = true;
        rank.lessonAdd = true;
        rank.lessonEdit = true;
        rank.lessonDelete = true;
        rank.commentAdd = true;
        rank.commentEdit = true;
        rank.commentDelete = true;
        rank.userAdd = true;
        rank.userEdit = true;
        rank.userDelete = true;
        rank.userGroupAdd = true;
        rank.userGroupEdit = true;
        rank.userGroupDelete = true;
        rank.userRankAdd = true;
        rank.userRankEdit = true;
        rank.userRankDelete = true;

        final Set<String> roles = this.augmentor.buildRolesFromUserRank(rank);

        assertEquals(19, roles.size());
        assertTrue(roles.contains("admin:view"));
        assertTrue(roles.contains("exercise:add"));
        assertTrue(roles.contains("exercise:edit"));
        assertTrue(roles.contains("exercise:delete"));
        assertTrue(roles.contains("lesson:add"));
        assertTrue(roles.contains("lesson:edit"));
        assertTrue(roles.contains("lesson:delete"));
        assertTrue(roles.contains("comment:add"));
        assertTrue(roles.contains("comment:edit"));
        assertTrue(roles.contains("comment:delete"));
        assertTrue(roles.contains("user:add"));
        assertTrue(roles.contains("user:edit"));
        assertTrue(roles.contains("user:delete"));
        assertTrue(roles.contains("user-group:add"));
        assertTrue(roles.contains("user-group:edit"));
        assertTrue(roles.contains("user-group:delete"));
        assertTrue(roles.contains("user-rank:add"));
        assertTrue(roles.contains("user-rank:edit"));
        assertTrue(roles.contains("user-rank:delete"));
    }

    @Test
    @DisplayName("Should build empty roles when rank has no permissions")
    void shouldBuildEmptyRolesWhenRankHasNoPermissions() {
        final UserRankEntity rank = new UserRankEntity();

        final Set<String> roles = this.augmentor.buildRolesFromUserRank(rank);

        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("Should only add roles for true permissions")
    void shouldOnlyAddRolesForTruePermissions() {
        final UserRankEntity rank = new UserRankEntity();
        rank.adminView = true;
        rank.exerciseAdd = false;
        rank.exerciseEdit = true;

        final Set<String> roles = this.augmentor.buildRolesFromUserRank(rank);

        assertEquals(2, roles.size());
        assertTrue(roles.contains("admin:view"));
        assertTrue(roles.contains("exercise:edit"));
    }
}
