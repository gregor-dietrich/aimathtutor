package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserGroupEntityTest {

    private UserGroupEntity userGroup;
    private UserEntity user1;
    private UserEntity user2;

    @BeforeEach
    void setUp() {
        this.userGroup = new UserGroupEntity();

        this.user1 = new UserEntity();
        this.user1.id = 1L;
        this.user1.username = "user1";

        this.user2 = new UserEntity();
        this.user2.id = 2L;
        this.user2.username = "user2";
    }

    @Test
    @DisplayName("Should create UserGroupEntity with all fields")
    void shouldCreateUserGroupEntityWithAllFields() {
        // Given
        final List<UserGroupMetaEntity> userGroupMetas = new ArrayList<>();

        // When
        this.userGroup.id = 1L;
        this.userGroup.name = "Test Group";
        this.userGroup.userGroupMetas = userGroupMetas;

        // Then
        assertEquals(1L, this.userGroup.id);
        assertEquals("Test Group", this.userGroup.name);
        assertEquals(userGroupMetas, this.userGroup.userGroupMetas);
    }

    @Test
    @DisplayName("Should get users from group correctly")
    void shouldGetUsersFromGroupCorrectly() {
        // Given
        final UserGroupMetaEntity meta1 = new UserGroupMetaEntity();
        meta1.user = this.user1;
        meta1.group = this.userGroup;

        final UserGroupMetaEntity meta2 = new UserGroupMetaEntity();
        meta2.user = this.user2;
        meta2.group = this.userGroup;

        final List<UserGroupMetaEntity> metas = List.of(meta1, meta2);
        this.userGroup.userGroupMetas = metas;

        // When
        final List<UserEntity> users = this.userGroup.getUsers();

        // Then
        assertEquals(2, users.size());
        assertTrue(users.contains(this.user1));
        assertTrue(users.contains(this.user2));
    }

    @Test
    @DisplayName("Should get user count correctly")
    void shouldGetUserCountCorrectly() {
        // Given
        final UserGroupMetaEntity meta1 = new UserGroupMetaEntity();
        meta1.user = this.user1;

        final UserGroupMetaEntity meta2 = new UserGroupMetaEntity();
        meta2.user = this.user2;

        final List<UserGroupMetaEntity> metas = List.of(meta1, meta2);
        this.userGroup.userGroupMetas = metas;

        // When
        final long userCount = this.userGroup.getUserCount();

        // Then
        assertEquals(2, userCount);
    }

    @Test
    @DisplayName("Should return zero user count for empty group")
    void shouldReturnZeroUserCountForEmptyGroup() {
        // Given
        this.userGroup.userGroupMetas = new ArrayList<>();

        // When
        final long userCount = this.userGroup.getUserCount();

        // Then
        assertEquals(0, userCount);
    }

    @Test
    @DisplayName("Should return zero user count for null metas")
    void shouldReturnZeroUserCountForNullMetas() {
        // Given
        this.userGroup.userGroupMetas = null;

        // When
        final long userCount = this.userGroup.getUserCount();

        // Then
        assertEquals(0, userCount);
    }

    @Test
    @DisplayName("Should return empty list for null metas when getting users")
    void shouldReturnEmptyListForNullMetasWhenGettingUsers() {
        // Given
        this.userGroup.userGroupMetas = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            this.userGroup.getUsers();
        });
    }

    @Test
    @DisplayName("Should return empty list for empty metas when getting users")
    void shouldReturnEmptyListForEmptyMetasWhenGettingUsers() {
        // Given
        this.userGroup.userGroupMetas = new ArrayList<>();

        // When
        final List<UserEntity> users = this.userGroup.getUsers();

        // Then
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("Should handle group with single user")
    void shouldHandleGroupWithSingleUser() {
        // Given
        final UserGroupMetaEntity meta = new UserGroupMetaEntity();
        meta.user = this.user1;
        meta.group = this.userGroup;

        this.userGroup.userGroupMetas = List.of(meta);

        // When
        final List<UserEntity> users = this.userGroup.getUsers();
        final long userCount = this.userGroup.getUserCount();

        // Then
        assertEquals(1, users.size());
        assertEquals(this.user1, users.get(0));
        assertEquals(1, userCount);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        this.userGroup.name = null;

        // Then
        assertNull(this.userGroup.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        this.userGroup.name = "";

        // Then
        assertEquals("", this.userGroup.name);
    }

    @Test
    @DisplayName("Should handle name with special characters")
    void shouldHandleNameWithSpecialCharacters() {
        // Given
        final String specialName = "Admin-Group_2024@Test";

        // When
        this.userGroup.name = specialName;

        // Then
        assertEquals(specialName, this.userGroup.name);
    }
}