package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class LoginAttemptServiceTest {

    @Inject
    LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        // Clear any leftover state from previous tests
        this.loginAttemptService.recordSuccessfulLogin("testuser");
        this.loginAttemptService.recordSuccessfulLogin("testip");
        this.loginAttemptService.recordSuccessfulLogin("bruteuser");
        this.loginAttemptService.recordSuccessfulLogin("legituser");
        this.loginAttemptService.recordSuccessfulLogin("escalating");
        this.loginAttemptService.recordSuccessfulLogin("newuser");
    }

    @Test
    @DisplayName("Should not be locked out initially")
    void shouldNotBeLockedOutInitially() {
        assertFalse(this.loginAttemptService.isLockedOut("newuser"));
        assertEquals(0, this.loginAttemptService.getRemainingLockoutSeconds("newuser"));
    }

    @Test
    @DisplayName("Should lock out after max failed attempts")
    void shouldLockOutAfterMaxFailedAttempts() {
        final String key = "bruteuser";

        // 4 failed attempts - not locked out yet
        for (int i = 0; i < 4; i++) {
            assertFalse(this.loginAttemptService.isLockedOut(key));
            this.loginAttemptService.recordFailedAttempt(key);
        }

        // 5th attempt triggers lockout
        this.loginAttemptService.recordFailedAttempt(key);
        assertTrue(this.loginAttemptService.isLockedOut(key));
        assertTrue(this.loginAttemptService.getRemainingLockoutSeconds(key) > 0);
    }

    @Test
    @DisplayName("Should clear lockout on successful login")
    void shouldClearLockoutOnSuccessfulLogin() {
        final String key = "legituser";

        for (int i = 0; i < 5; i++) {
            this.loginAttemptService.recordFailedAttempt(key);
        }
        assertTrue(this.loginAttemptService.isLockedOut(key));

        this.loginAttemptService.recordSuccessfulLogin(key);
        assertFalse(this.loginAttemptService.isLockedOut(key));
        assertEquals(0, this.loginAttemptService.getRemainingLockoutSeconds(key));
    }

    @Test
    @DisplayName("Should increase lockout duration exponentially")
    void shouldIncreaseLockoutDurationExponentially() {
        final String key = "escalating";

        final long[] expectedLockouts = { 0, 0, 0, 0, 30, 60, 120, 240, 480, 960 };
        for (int i = 0; i < expectedLockouts.length; i++) {
            final long lockout = this.loginAttemptService.recordFailedAttempt(key);
            assertEquals(expectedLockouts[i], lockout,
                    "Lockout at attempt " + (i + 1) + " should match expected value");
        }

        // Verify cap at 1 hour (3600 seconds)
        // Must verify exact cap value of 3600, not just <= 3600.
        long cappedLockout;
        do {
            cappedLockout = this.loginAttemptService.recordFailedAttempt(key);
        } while (cappedLockout < 3600);
        assertEquals(3600, cappedLockout, "Lockout should be capped at exactly 3600 seconds");
    }
}
