package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserGroupMetaEntityTest {

    private UserGroupMetaEntity userGroupMeta;
    private UserEntity user;
    private UserGroupEntity group;

    @BeforeEach
    void setUp() {
        userGroupMeta = new UserGroupMetaEntity();

        user = new UserEntity();
        user.id = 1L;
        user.username = "testuser";

        group = new UserGroupEntity();
        group.id = 1L;
        group.name = "Test Group";
    }

    @Test
    @DisplayName("Should create UserGroupMetaEntity with all fields")
    void shouldCreateUserGroupMetaEntityWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        userGroupMeta.id = 1L;
        userGroupMeta.user = user;
        userGroupMeta.group = group;
        userGroupMeta.timestamp = now;

        // Then
        assertEquals(1L, userGroupMeta.id);
        assertEquals(user, userGroupMeta.user);
        assertEquals(group, userGroupMeta.group);
        assertEquals(now, userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should handle null timestamp")
    void shouldHandleNullTimestamp() {
        // When
        userGroupMeta.user = user;
        userGroupMeta.group = group;
        userGroupMeta.timestamp = null;

        // Then
        assertEquals(user, userGroupMeta.user);
        assertEquals(group, userGroupMeta.group);
        assertNull(userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should maintain relationships correctly")
    void shouldMaintainRelationshipsCorrectly() {
        // When
        userGroupMeta.user = user;
        userGroupMeta.group = group;

        // Then
        assertNotNull(userGroupMeta.user);
        assertNotNull(userGroupMeta.group);
        assertEquals(1L, userGroupMeta.user.id);
        assertEquals("testuser", userGroupMeta.user.username);
        assertEquals(1L, userGroupMeta.group.id);
        assertEquals("Test Group", userGroupMeta.group.name);
    }

    @Test
    @DisplayName("Should handle different user and group combinations")
    void shouldHandleDifferentUserAndGroupCombinations() {
        // Given
        UserEntity anotherUser = new UserEntity();
        anotherUser.id = 2L;
        anotherUser.username = "anotheruser";

        UserGroupEntity anotherGroup = new UserGroupEntity();
        anotherGroup.id = 2L;
        anotherGroup.name = "Another Group";

        // When
        userGroupMeta.user = anotherUser;
        userGroupMeta.group = anotherGroup;

        // Then
        assertEquals(anotherUser, userGroupMeta.user);
        assertEquals(anotherGroup, userGroupMeta.group);
        assertEquals(2L, userGroupMeta.user.id);
        assertEquals("anotheruser", userGroupMeta.user.username);
        assertEquals(2L, userGroupMeta.group.id);
        assertEquals("Another Group", userGroupMeta.group.name);
    }

    @Test
    @DisplayName("Should handle null user")
    void shouldHandleNullUser() {
        // When
        userGroupMeta.user = null;
        userGroupMeta.group = group;

        // Then
        assertNull(userGroupMeta.user);
        assertEquals(group, userGroupMeta.group);
    }

    @Test
    @DisplayName("Should handle null group")
    void shouldHandleNullGroup() {
        // When
        userGroupMeta.user = user;
        userGroupMeta.group = null;

        // Then
        assertEquals(user, userGroupMeta.user);
        assertNull(userGroupMeta.group);
    }

    @Test
    @DisplayName("Should handle both null user and group")
    void shouldHandleBothNullUserAndGroup() {
        // When
        userGroupMeta.user = null;
        userGroupMeta.group = null;

        // Then
        assertNull(userGroupMeta.user);
        assertNull(userGroupMeta.group);
    }

    @Test
    @DisplayName("Should track membership timestamp")
    void shouldTrackMembershipTimestamp() {
        // Given
        LocalDateTime membershipTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // When
        userGroupMeta.user = user;
        userGroupMeta.group = group;
        userGroupMeta.timestamp = membershipTime;

        // Then
        assertEquals(membershipTime, userGroupMeta.timestamp);
        assertEquals(2024, userGroupMeta.timestamp.getYear());
        assertEquals(1, userGroupMeta.timestamp.getMonthValue());
        assertEquals(15, userGroupMeta.timestamp.getDayOfMonth());
    }

    @Test
    @DisplayName("Should represent user-group association correctly")
    void shouldRepresentUserGroupAssociationCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        userGroupMeta.user = user;
        userGroupMeta.group = group;
        userGroupMeta.timestamp = now;

        // Then - Verify the association is correctly established
        assertEquals(user.id, userGroupMeta.user.id);
        assertEquals(group.id, userGroupMeta.group.id);
        assertEquals(user.username, userGroupMeta.user.username);
        assertEquals(group.name, userGroupMeta.group.name);
        assertNotNull(userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should handle multiple meta entities for same user")
    void shouldHandleMultipleMetaEntitiesForSameUser() {
        // Given
        UserGroupEntity group2 = new UserGroupEntity();
        group2.id = 2L;
        group2.name = "Second Group";

        UserGroupMetaEntity meta2 = new UserGroupMetaEntity();

        // When
        userGroupMeta.user = user;
        userGroupMeta.group = group;

        meta2.user = user; // Same user
        meta2.group = group2; // Different group

        // Then
        assertEquals(user, userGroupMeta.user);
        assertEquals(user, meta2.user);
        assertEquals(group, userGroupMeta.group);
        assertEquals(group2, meta2.group);
        assertEquals(userGroupMeta.user.id, meta2.user.id); // Same user ID
        assertNotEquals(userGroupMeta.group.id, meta2.group.id); // Different group IDs
    }
}