package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDtoTest {

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
    }

    @Test
    @DisplayName("Should create UserDto with default constructor")
    void shouldCreateUserDtoWithDefaultConstructor() {
        // Then
        assertNull(userDto.id);
        assertNull(userDto.username);
        assertNull(userDto.password);
        assertNull(userDto.email);
        assertNull(userDto.rankId);
        assertNull(userDto.banned);
        assertNull(userDto.activated);
        assertNull(userDto.activationKey);
        assertNull(userDto.lastIp);
    }

    @Test
    @DisplayName("Should create UserDto with parameterized constructor")
    void shouldCreateUserDtoWithParameterizedConstructor() {
        // When
        UserDto dto = new UserDto("testuser", "password123", "test@example.com", 1L, false, true, "key123",
                "192.168.1.1");

        // Then
        assertEquals("testuser", dto.username);
        assertEquals("password123", dto.password);
        assertEquals("test@example.com", dto.email);
        assertEquals(1L, dto.rankId);
        assertFalse(dto.banned);
        assertTrue(dto.activated);
        assertEquals("key123", dto.activationKey);
        assertEquals("192.168.1.1", dto.lastIp);
        assertNull(dto.id); // Not set by constructor
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        userDto.id = 1L;
        userDto.username = "testuser";
        userDto.password = "hashedpassword";
        userDto.email = "test@example.com";
        userDto.rankId = 2L;
        userDto.banned = false;
        userDto.activated = true;
        userDto.activationKey = "activation123";
        userDto.lastIp = "192.168.1.1";

        // Then
        assertEquals(1L, userDto.id);
        assertEquals("testuser", userDto.username);
        assertEquals("hashedpassword", userDto.password);
        assertEquals("test@example.com", userDto.email);
        assertEquals(2L, userDto.rankId);
        assertFalse(userDto.banned);
        assertTrue(userDto.activated);
        assertEquals("activation123", userDto.activationKey);
        assertEquals("192.168.1.1", userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // When
        userDto.username = null;
        userDto.password = null;
        userDto.email = null;
        userDto.rankId = null;
        userDto.banned = null;
        userDto.activated = null;
        userDto.activationKey = null;
        userDto.lastIp = null;

        // Then
        assertNull(userDto.username);
        assertNull(userDto.password);
        assertNull(userDto.email);
        assertNull(userDto.rankId);
        assertNull(userDto.banned);
        assertNull(userDto.activated);
        assertNull(userDto.activationKey);
        assertNull(userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle empty strings")
    void shouldHandleEmptyStrings() {
        // When
        userDto.username = "";
        userDto.password = "";
        userDto.email = "";
        userDto.activationKey = "";
        userDto.lastIp = "";

        // Then
        assertEquals("", userDto.username);
        assertEquals("", userDto.password);
        assertEquals("", userDto.email);
        assertEquals("", userDto.activationKey);
        assertEquals("", userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle boolean flags correctly")
    void shouldHandleBooleanFlagsCorrectly() {
        // When - Test all boolean combinations
        userDto.banned = true;
        userDto.activated = false;

        // Then
        assertTrue(userDto.banned);
        assertFalse(userDto.activated);

        // When - Flip values
        userDto.banned = false;
        userDto.activated = true;

        // Then
        assertFalse(userDto.banned);
        assertTrue(userDto.activated);
    }

    @Test
    @DisplayName("Should handle special characters in fields")
    void shouldHandleSpecialCharactersInFields() {
        // Given
        String specialUsername = "test_user-123@domain.com";
        String specialEmail = "test+user@sub.domain.co.uk";
        String specialKey = "key-123_456@activation";
        String specialIp = "::1"; // IPv6 localhost

        // When
        userDto.username = specialUsername;
        userDto.email = specialEmail;
        userDto.activationKey = specialKey;
        userDto.lastIp = specialIp;

        // Then
        assertEquals(specialUsername, userDto.username);
        assertEquals(specialEmail, userDto.email);
        assertEquals(specialKey, userDto.activationKey);
        assertEquals(specialIp, userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle long strings")
    void shouldHandleLongStrings() {
        // Given
        StringBuilder longUsername = new StringBuilder();
        StringBuilder longPassword = new StringBuilder();
        StringBuilder longEmail = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            longUsername.append("u");
            longPassword.append("p");
            longEmail.append("e");
        }
        longEmail.append("@example.com");

        // When
        userDto.username = longUsername.toString();
        userDto.password = longPassword.toString();
        userDto.email = longEmail.toString();

        // Then
        assertEquals(longUsername.toString(), userDto.username);
        assertEquals(longPassword.toString(), userDto.password);
        assertEquals(longEmail.toString(), userDto.email);
    }

    @Test
    @DisplayName("Should handle edge case IP addresses")
    void shouldHandleEdgeCaseIpAddresses() {
        // Test various IP formats
        String[] ipAddresses = {
                "127.0.0.1", // IPv4 localhost
                "0.0.0.0", // IPv4 any
                "255.255.255.255", // IPv4 broadcast
                "::1", // IPv6 localhost
                "2001:db8::1", // IPv6 example
                "fe80::1%lo0" // IPv6 with zone
        };

        for (String ip : ipAddresses) {
            // When
            userDto.lastIp = ip;

            // Then
            assertEquals(ip, userDto.lastIp);
        }
    }
}