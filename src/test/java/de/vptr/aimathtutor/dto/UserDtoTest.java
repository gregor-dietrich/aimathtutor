package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDtoTest {

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        this.userDto = new UserDto();
    }

    @Test
    @DisplayName("Should create UserDto with default constructor")
    void shouldCreateUserDtoWithDefaultConstructor() {
        // Then
        assertNull(this.userDto.id);
        assertNull(this.userDto.username);
        assertNull(this.userDto.password);
        assertNull(this.userDto.email);
        assertNull(this.userDto.rankId);
        assertNull(this.userDto.banned);
        assertNull(this.userDto.activated);
        assertNull(this.userDto.activationKey);
        assertNull(this.userDto.lastIp);
    }

    @Test
    @DisplayName("Should create UserDto with parameterized constructor")
    void shouldCreateUserDtoWithParameterizedConstructor() {
        // When
        final UserDto dto = new UserDto("testuser", "password123", "test@example.com", 1L, false, true, "key123",
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
        this.userDto.id = 1L;
        this.userDto.username = "testuser";
        this.userDto.password = "hashedpassword";
        this.userDto.email = "test@example.com";
        this.userDto.rankId = 2L;
        this.userDto.banned = false;
        this.userDto.activated = true;
        this.userDto.activationKey = "activation123";
        this.userDto.lastIp = "192.168.1.1";

        // Then
        assertEquals(1L, this.userDto.id);
        assertEquals("testuser", this.userDto.username);
        assertEquals("hashedpassword", this.userDto.password);
        assertEquals("test@example.com", this.userDto.email);
        assertEquals(2L, this.userDto.rankId);
        assertFalse(this.userDto.banned);
        assertTrue(this.userDto.activated);
        assertEquals("activation123", this.userDto.activationKey);
        assertEquals("192.168.1.1", this.userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // When
        this.userDto.username = null;
        this.userDto.password = null;
        this.userDto.email = null;
        this.userDto.rankId = null;
        this.userDto.banned = null;
        this.userDto.activated = null;
        this.userDto.activationKey = null;
        this.userDto.lastIp = null;

        // Then
        assertNull(this.userDto.username);
        assertNull(this.userDto.password);
        assertNull(this.userDto.email);
        assertNull(this.userDto.rankId);
        assertNull(this.userDto.banned);
        assertNull(this.userDto.activated);
        assertNull(this.userDto.activationKey);
        assertNull(this.userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle empty strings")
    void shouldHandleEmptyStrings() {
        // When
        this.userDto.username = "";
        this.userDto.password = "";
        this.userDto.email = "";
        this.userDto.activationKey = "";
        this.userDto.lastIp = "";

        // Then
        assertEquals("", this.userDto.username);
        assertEquals("", this.userDto.password);
        assertEquals("", this.userDto.email);
        assertEquals("", this.userDto.activationKey);
        assertEquals("", this.userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle boolean flags correctly")
    void shouldHandleBooleanFlagsCorrectly() {
        // When - Test all boolean combinations
        this.userDto.banned = true;
        this.userDto.activated = false;

        // Then
        assertTrue(this.userDto.banned);
        assertFalse(this.userDto.activated);

        // When - Flip values
        this.userDto.banned = false;
        this.userDto.activated = true;

        // Then
        assertFalse(this.userDto.banned);
        assertTrue(this.userDto.activated);
    }

    @Test
    @DisplayName("Should handle special characters in fields")
    void shouldHandleSpecialCharactersInFields() {
        // Given
        final String specialUsername = "test_user-123@domain.com";
        final String specialEmail = "test+user@sub.domain.co.uk";
        final String specialKey = "key-123_456@activation";
        final String specialIp = "::1"; // IPv6 localhost

        // When
        this.userDto.username = specialUsername;
        this.userDto.email = specialEmail;
        this.userDto.activationKey = specialKey;
        this.userDto.lastIp = specialIp;

        // Then
        assertEquals(specialUsername, this.userDto.username);
        assertEquals(specialEmail, this.userDto.email);
        assertEquals(specialKey, this.userDto.activationKey);
        assertEquals(specialIp, this.userDto.lastIp);
    }

    @Test
    @DisplayName("Should handle long strings")
    void shouldHandleLongStrings() {
        // Given
        final StringBuilder longUsername = new StringBuilder();
        final StringBuilder longPassword = new StringBuilder();
        final StringBuilder longEmail = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            longUsername.append("u");
            longPassword.append("p");
            longEmail.append("e");
        }
        longEmail.append("@example.com");

        // When
        this.userDto.username = longUsername.toString();
        this.userDto.password = longPassword.toString();
        this.userDto.email = longEmail.toString();

        // Then
        assertEquals(longUsername.toString(), this.userDto.username);
        assertEquals(longPassword.toString(), this.userDto.password);
        assertEquals(longEmail.toString(), this.userDto.email);
    }

    @Test
    @DisplayName("Should handle edge case IP addresses")
    void shouldHandleEdgeCaseIpAddresses() {
        // Test various IP formats
        final String[] ipAddresses = {
                "127.0.0.1", // IPv4 localhost
                "0.0.0.0", // IPv4 any
                "255.255.255.255", // IPv4 broadcast
                "::1", // IPv6 localhost
                "2001:db8::1", // IPv6 example
                "fe80::1%lo0" // IPv6 with zone
        };

        for (final String ip : ipAddresses) {
            // When
            this.userDto.lastIp = ip;

            // Then
            assertEquals(ip, this.userDto.lastIp);
        }
    }
}