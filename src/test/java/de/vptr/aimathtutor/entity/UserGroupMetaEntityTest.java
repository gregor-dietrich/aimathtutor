package de.vptr.aimathtutor.entity;

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
        this.userGroupMeta = new UserGroupMetaEntity();

        this.user = new UserEntity();
        this.user.id = 1L;
        this.user.username = "testuser";

        this.group = new UserGroupEntity();
        this.group.id = 1L;
        this.group.name = "Test Group";
    }

    @Test
    @DisplayName("Should create UserGroupMetaEntity with all fields")
    void shouldCreateUserGroupMetaEntityWithAllFields() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.userGroupMeta.id = 1L;
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;
        this.userGroupMeta.timestamp = now;

        // Then
        assertEquals(1L, this.userGroupMeta.id);
        assertEquals(this.user, this.userGroupMeta.user);
        assertEquals(this.group, this.userGroupMeta.group);
        assertEquals(now, this.userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should handle null timestamp")
    void shouldHandleNullTimestamp() {
        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;
        this.userGroupMeta.timestamp = null;

        // Then
        assertEquals(this.user, this.userGroupMeta.user);
        assertEquals(this.group, this.userGroupMeta.group);
        assertNull(this.userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should maintain relationships correctly")
    void shouldMaintainRelationshipsCorrectly() {
        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;

        // Then
        assertNotNull(this.userGroupMeta.user);
        assertNotNull(this.userGroupMeta.group);
        assertEquals(1L, this.userGroupMeta.user.id);
        assertEquals("testuser", this.userGroupMeta.user.username);
        assertEquals(1L, this.userGroupMeta.group.id);
        assertEquals("Test Group", this.userGroupMeta.group.name);
    }

    @Test
    @DisplayName("Should handle different user and group combinations")
    void shouldHandleDifferentUserAndGroupCombinations() {
        // Given
        final UserEntity anotherUser = new UserEntity();
        anotherUser.id = 2L;
        anotherUser.username = "anotheruser";

        final UserGroupEntity anotherGroup = new UserGroupEntity();
        anotherGroup.id = 2L;
        anotherGroup.name = "Another Group";

        // When
        this.userGroupMeta.user = anotherUser;
        this.userGroupMeta.group = anotherGroup;

        // Then
        assertEquals(anotherUser, this.userGroupMeta.user);
        assertEquals(anotherGroup, this.userGroupMeta.group);
        assertEquals(2L, this.userGroupMeta.user.id);
        assertEquals("anotheruser", this.userGroupMeta.user.username);
        assertEquals(2L, this.userGroupMeta.group.id);
        assertEquals("Another Group", this.userGroupMeta.group.name);
    }

    @Test
    @DisplayName("Should handle null user")
    void shouldHandleNullUser() {
        // When
        this.userGroupMeta.user = null;
        this.userGroupMeta.group = this.group;

        // Then
        assertNull(this.userGroupMeta.user);
        assertEquals(this.group, this.userGroupMeta.group);
    }

    @Test
    @DisplayName("Should handle null group")
    void shouldHandleNullGroup() {
        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = null;

        // Then
        assertEquals(this.user, this.userGroupMeta.user);
        assertNull(this.userGroupMeta.group);
    }

    @Test
    @DisplayName("Should handle both null user and group")
    void shouldHandleBothNullUserAndGroup() {
        // When
        this.userGroupMeta.user = null;
        this.userGroupMeta.group = null;

        // Then
        assertNull(this.userGroupMeta.user);
        assertNull(this.userGroupMeta.group);
    }

    @Test
    @DisplayName("Should track membership timestamp")
    void shouldTrackMembershipTimestamp() {
        // Given
        final LocalDateTime membershipTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;
        this.userGroupMeta.timestamp = membershipTime;

        // Then
        assertEquals(membershipTime, this.userGroupMeta.timestamp);
        assertEquals(2024, this.userGroupMeta.timestamp.getYear());
        assertEquals(1, this.userGroupMeta.timestamp.getMonthValue());
        assertEquals(15, this.userGroupMeta.timestamp.getDayOfMonth());
    }

    @Test
    @DisplayName("Should represent user-group association correctly")
    void shouldRepresentUserGroupAssociationCorrectly() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;
        this.userGroupMeta.timestamp = now;

        // Then - Verify the association is correctly established
        assertEquals(this.user.id, this.userGroupMeta.user.id);
        assertEquals(this.group.id, this.userGroupMeta.group.id);
        assertEquals(this.user.username, this.userGroupMeta.user.username);
        assertEquals(this.group.name, this.userGroupMeta.group.name);
        assertNotNull(this.userGroupMeta.timestamp);
    }

    @Test
    @DisplayName("Should handle multiple meta entities for same user")
    void shouldHandleMultipleMetaEntitiesForSameUser() {
        // Given
        final UserGroupEntity group2 = new UserGroupEntity();
        group2.id = 2L;
        group2.name = "Second Group";

        final UserGroupMetaEntity meta2 = new UserGroupMetaEntity();

        // When
        this.userGroupMeta.user = this.user;
        this.userGroupMeta.group = this.group;

        meta2.user = this.user; // Same user
        meta2.group = group2; // Different group

        // Then
        assertEquals(this.user, this.userGroupMeta.user);
        assertEquals(this.user, meta2.user);
        assertEquals(this.group, this.userGroupMeta.group);
        assertEquals(group2, meta2.group);
        assertEquals(this.userGroupMeta.user.id, meta2.user.id); // Same user ID
        assertNotEquals(this.userGroupMeta.group.id, meta2.group.id); // Different group IDs
    }
}