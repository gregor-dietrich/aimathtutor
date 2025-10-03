package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserGroupDtoTest {

    private UserGroupDto userGroupDto;

    @BeforeEach
    void setUp() {
        this.userGroupDto = new UserGroupDto();
    }

    @Test
    @DisplayName("Should create UserGroupDto with default constructor")
    void shouldCreateUserGroupDtoWithDefaultConstructor() {
        // Then
        assertNull(this.userGroupDto.id);
        assertNull(this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        this.userGroupDto.id = 1L;
        this.userGroupDto.name = "Administrators";

        // Then
        assertEquals(1L, this.userGroupDto.id);
        assertEquals("Administrators", this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // When
        this.userGroupDto.id = null;
        this.userGroupDto.name = null;

        // Then
        assertNull(this.userGroupDto.id);
        assertNull(this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        this.userGroupDto.name = "";

        // Then
        assertEquals("", this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle whitespace-only name")
    void shouldHandleWhitespaceOnlyName() {
        // When
        this.userGroupDto.name = "   ";

        // Then
        assertEquals("   ", this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        final String specialName = "Admin-Group_2024 & Moderators!";

        // When
        this.userGroupDto.name = specialName;

        // Then
        assertEquals(specialName, this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
        // Given
        final String unicodeName = "Administradores España 🇪🇸";

        // When
        this.userGroupDto.name = unicodeName;

        // Then
        assertEquals(unicodeName, this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle long group names")
    void shouldHandleLongGroupNames() {
        // Given
        final StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("G");
        }

        // When
        this.userGroupDto.name = longName.toString();

        // Then
        assertEquals(longName.toString(), this.userGroupDto.name);
        assertEquals(200, this.userGroupDto.name.length());
    }

    @Test
    @DisplayName("Should handle common group name patterns")
    void shouldHandleCommonGroupNamePatterns() {
        final String[] commonNames = {
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

        for (final String name : commonNames) {
            // When
            this.userGroupDto.name = name;

            // Then
            assertEquals(name, this.userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should handle group names with numbers")
    void shouldHandleGroupNamesWithNumbers() {
        // Given
        final String[] numericNames = {
                "Admin Level 1",
                "Team 2024",
                "Group 001",
                "Version 2.0 Testers",
                "Class of 2023"
        };

        for (final String name : numericNames) {
            // When
            this.userGroupDto.name = name;

            // Then
            assertEquals(name, this.userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should handle group names with mixed case")
    void shouldHandleGroupNamesWithMixedCase() {
        // Test case sensitivity preservation
        final String[] mixedCaseNames = {
                "ADMINISTRATORS",
                "moderators",
                "MiXeD cAsE gRoUp",
                "CamelCaseGroup",
                "snake_case_group"
        };

        for (final String name : mixedCaseNames) {
            // When
            this.userGroupDto.name = name;

            // Then
            assertEquals(name, this.userGroupDto.name);
        }
    }

    @Test
    @DisplayName("Should maintain field independence")
    void shouldMaintainFieldIndependence() {
        // When
        this.userGroupDto.id = 1L;
        this.userGroupDto.name = "Test Group";

        // Then
        assertEquals(1L, this.userGroupDto.id);
        assertEquals("Test Group", this.userGroupDto.name);

        // When - Change one field
        this.userGroupDto.id = 2L;

        // Then - Other field should remain unchanged
        assertEquals(2L, this.userGroupDto.id);
        assertEquals("Test Group", this.userGroupDto.name);

        // When - Change other field
        this.userGroupDto.name = "Modified Group";

        // Then - First field should remain unchanged
        assertEquals(2L, this.userGroupDto.id);
        assertEquals("Modified Group", this.userGroupDto.name);
    }

    @Test
    @DisplayName("Should handle negative and zero IDs")
    void shouldHandleNegativeAndZeroIds() {
        // Test zero ID
        this.userGroupDto.id = 0L;
        assertEquals(0L, this.userGroupDto.id);

        // Test negative ID
        this.userGroupDto.id = -1L;
        assertEquals(-1L, this.userGroupDto.id);

        // Test very large ID
        this.userGroupDto.id = Long.MAX_VALUE;
        assertEquals(Long.MAX_VALUE, this.userGroupDto.id);

        // Test very small ID
        this.userGroupDto.id = Long.MIN_VALUE;
        assertEquals(Long.MIN_VALUE, this.userGroupDto.id);
    }
}