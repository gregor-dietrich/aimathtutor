package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserGroupDtoTest {

    private UserGroupDto userGroupDto;

    @BeforeEach
    void setUp() {
        userGroupDto = new UserGroupDto();
    }

    @Test
    @DisplayName("Should create UserGroupDto with default constructor")
    void shouldCreateUserGroupDtoWithDefaultConstructor() {
        // Then
        assertNull(userGroupDto.id);
        assertNull(userGroupDto.name);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        userGroupDto.id = 1L;
        userGroupDto.name = "Administrators";

        // Then
        assertEquals(1L, userGroupDto.id);
        assertEquals("Administrators", userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // When
        userGroupDto.id = null;
        userGroupDto.name = null;

        // Then
        assertNull(userGroupDto.id);
        assertNull(userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        userGroupDto.name = "";

        // Then
        assertEquals("", userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle whitespace-only name")
    void shouldHandleWhitespaceOnlyName() {
        // When
        userGroupDto.name = "   ";

        // Then
        assertEquals("   ", userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        String specialName = "Admin-Group_2024 & Moderators!";

        // When
        userGroupDto.name = specialName;

        // Then
        assertEquals(specialName, userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
        // Given
        String unicodeName = "Administradores España 🇪🇸";

        // When
        userGroupDto.name = unicodeName;

        // Then
        assertEquals(unicodeName, userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle long group names")
    void shouldHandleLongGroupNames() {
        // Given
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("G");
        }

        // When
        userGroupDto.name = longName.toString();

        // Then
        assertEquals(longName.toString(), userGroupDto.name);
        assertEquals(200, userGroupDto.name.length());
    }

    @Test
    @DisplayName("Should handle common group name patterns")
    void shouldHandleCommonGroupNamePatterns() {
        String[] commonNames = {
                "Administrators",
                "Moderators",
                "Users",
                "Guests",
                "Super Admins",
                "Content Managers",
                "Beta Testers",
                "VIP Members",
                "Support Team",
                "Developer Team"
        };

        for (String name : commonNames) {
            // When
            userGroupDto.name = name;

            // Then
            assertEquals(name, userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should handle group names with numbers")
    void shouldHandleGroupNamesWithNumbers() {
        // Given
        String[] numericNames = {
                "Admin Level 1",
                "Team 2024",
                "Group 001",
                "Version 2.0 Testers",
                "Class of 2023"
        };

        for (String name : numericNames) {
            // When
            userGroupDto.name = name;

            // Then
            assertEquals(name, userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should handle group names with mixed case")
    void shouldHandleGroupNamesWithMixedCase() {
        // Test case sensitivity preservation
        String[] mixedCaseNames = {
                "ADMINISTRATORS",
                "moderators",
                "MiXeD cAsE gRoUp",
                "CamelCaseGroup",
                "snake_case_group"
        };

        for (String name : mixedCaseNames) {
            // When
            userGroupDto.name = name;

            // Then
            assertEquals(name, userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should maintain field independence")
    void shouldMaintainFieldIndependence() {
        // When
        userGroupDto.id = 1L;
        userGroupDto.name = "Test Group";

        // Then
        assertEquals(1L, userGroupDto.id);
        assertEquals("Test Group", userGroupDto.name);

        // When - Change one field
        userGroupDto.id = 2L;

        // Then - Other field should remain unchanged
        assertEquals(2L, userGroupDto.id);
        assertEquals("Test Group", userGroupDto.name);

        // When - Change other field
        userGroupDto.name = "Modified Group";

        // Then - First field should remain unchanged
        assertEquals(2L, userGroupDto.id);
        assertEquals("Modified Group", userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle negative and zero IDs")
    void shouldHandleNegativeAndZeroIds() {
        // Test zero ID
        userGroupDto.id = 0L;
        assertEquals(0L, userGroupDto.id);

        // Test negative ID
        userGroupDto.id = -1L;
        assertEquals(-1L, userGroupDto.id);

        // Test very large ID
        userGroupDto.id = Long.MAX_VALUE;
        assertEquals(Long.MAX_VALUE, userGroupDto.id);

        // Test very small ID
        userGroupDto.id = Long.MIN_VALUE;
        assertEquals(Long.MIN_VALUE, userGroupDto.id);
    }
}