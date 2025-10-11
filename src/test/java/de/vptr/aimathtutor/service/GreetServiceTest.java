package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GreetServiceTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private GreetService greetService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(this.authService);
    }

    @Test
    @DisplayName("Should greet authenticated user with provided name")
    void shouldGreetAuthenticatedUserWithProvidedName() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String name = "John";

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Hello, John!", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should greet authenticated user with anonymous when name is null")
    void shouldGreetAuthenticatedUserWithAnonymousWhenNameIsNull() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String name = null;

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Hello, anonymous user!", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should greet authenticated user with anonymous when name is empty")
    void shouldGreetAuthenticatedUserWithAnonymousWhenNameIsEmpty() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String name = "";

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Hello, anonymous user!", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should return error message when user is not authenticated")
    void shouldReturnErrorMessageWhenUserIsNotAuthenticated() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(false);
        final String name = "John";

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Error: Not authenticated", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should handle exception from AuthService")
    void shouldHandleExceptionFromAuthService() {
        // Given
        when(this.authService.isAuthenticated()).thenThrow(new RuntimeException("Auth service error"));
        final String name = "John";

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Error: Could not generate greeting", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should greet authenticated user with whitespace-only name as provided")
    void shouldGreetAuthenticatedUserWithWhitespaceOnlyNameAsProvided() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String name = "   ";

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertEquals("Hello,    !", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should handle very long names correctly")
    void shouldHandleVeryLongNamesCorrectly() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String longName = "A".repeat(1000);

        // When
        final String result = this.greetService.greet(longName);

        // Then
        assertEquals("Hello, " + longName + "!", result);
        verify(this.authService).isAuthenticated();
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        when(this.authService.isAuthenticated()).thenReturn(true);
        final String nameWithSpecialChars = "José María O'Connor";

        // When
        final String result = this.greetService.greet(nameWithSpecialChars);

        // Then
        assertEquals("Hello, José María O'Connor!", result);
        verify(this.authService).isAuthenticated();
    }
}