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
        userGroup = new UserGroupEntity();

        user1 = new UserEntity();
        user1.id = 1L;
        user1.username = "user1";

        user2 = new UserEntity();
        user2.id = 2L;
        user2.username = "user2";
    }

    @Test
    @DisplayName("Should create UserGroupEntity with all fields")
    void shouldCreateUserGroupEntityWithAllFields() {
        // Given
        List<UserGroupMetaEntity> userGroupMetas = new ArrayList<>();

        // When
        userGroup.id = 1L;
        userGroup.name = "Test Group";
        userGroup.userGroupMetas = userGroupMetas;

        // Then
        assertEquals(1L, userGroup.id);
        assertEquals("Test Group", userGroup.name);
        assertEquals(userGroupMetas, userGroup.userGroupMetas);
    }

    @Test
    @DisplayName("Should get users from group correctly")
    void shouldGetUsersFromGroupCorrectly() {
        // Given
        UserGroupMetaEntity meta1 = new UserGroupMetaEntity();
        meta1.user = user1;
        meta1.group = userGroup;

        UserGroupMetaEntity meta2 = new UserGroupMetaEntity();
        meta2.user = user2;
        meta2.group = userGroup;

        List<UserGroupMetaEntity> metas = List.of(meta1, meta2);
        userGroup.userGroupMetas = metas;

        // When
        List<UserEntity> users = userGroup.getUsers();

        // Then
        assertEquals(2, users.size());
        assertTrue(users.contains(user1));
        assertTrue(users.contains(user2));
    }

    @Test
    @DisplayName("Should get user count correctly")
    void shouldGetUserCountCorrectly() {
        // Given
        UserGroupMetaEntity meta1 = new UserGroupMetaEntity();
        meta1.user = user1;

        UserGroupMetaEntity meta2 = new UserGroupMetaEntity();
        meta2.user = user2;

        List<UserGroupMetaEntity> metas = List.of(meta1, meta2);
        userGroup.userGroupMetas = metas;

        // When
        long userCount = userGroup.getUserCount();

        // Then
        assertEquals(2, userCount);
    }

    @Test
    @DisplayName("Should return zero user count for empty group")
    void shouldReturnZeroUserCountForEmptyGroup() {
        // Given
        userGroup.userGroupMetas = new ArrayList<>();

        // When
        long userCount = userGroup.getUserCount();

        // Then
        assertEquals(0, userCount);
    }

    @Test
    @DisplayName("Should return zero user count for null metas")
    void shouldReturnZeroUserCountForNullMetas() {
        // Given
        userGroup.userGroupMetas = null;

        // When
        long userCount = userGroup.getUserCount();

        // Then
        assertEquals(0, userCount);
    }

    @Test
    @DisplayName("Should return empty list for null metas when getting users")
    void shouldReturnEmptyListForNullMetasWhenGettingUsers() {
        // Given
        userGroup.userGroupMetas = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            userGroup.getUsers();
        });
    }

    @Test
    @DisplayName("Should return empty list for empty metas when getting users")
    void shouldReturnEmptyListForEmptyMetasWhenGettingUsers() {
        // Given
        userGroup.userGroupMetas = new ArrayList<>();

        // When
        List<UserEntity> users = userGroup.getUsers();

        // Then
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("Should handle group with single user")
    void shouldHandleGroupWithSingleUser() {
        // Given
        UserGroupMetaEntity meta = new UserGroupMetaEntity();
        meta.user = user1;
        meta.group = userGroup;

        userGroup.userGroupMetas = List.of(meta);

        // When
        List<UserEntity> users = userGroup.getUsers();
        long userCount = userGroup.getUserCount();

        // Then
        assertEquals(1, users.size());
        assertEquals(user1, users.get(0));
        assertEquals(1, userCount);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        userGroup.name = null;

        // Then
        assertNull(userGroup.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        userGroup.name = "";

        // Then
        assertEquals("", userGroup.name);
    }

    @Test
    @DisplayName("Should handle name with special characters")
    void shouldHandleNameWithSpecialCharacters() {
        // Given
        String specialName = "Admin-Group_2024@Test";

        // When
        userGroup.name = specialName;

        // Then
        assertEquals(specialName, userGroup.name);
    }
}