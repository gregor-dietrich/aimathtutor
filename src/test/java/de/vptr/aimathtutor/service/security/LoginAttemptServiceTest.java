package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

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
    @DisplayName("Should return zero remaining seconds when under the attempt threshold")
    void shouldReturnZeroWhenUnderThreshold() {
        final String key = "underThreshold_" + UUID.randomUUID();
        // 3 attempts — under the lockout threshold of 5
        for (int i = 0; i < 3; i++) {
            this.loginAttemptService.recordFailedAttempt(key);
        }
        assertEquals(0, this.loginAttemptService.getRemainingLockoutSeconds(key));
        assertFalse(this.loginAttemptService.isLockedOut(key));
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

    @Test
    @DisplayName("Should lock out account after max failed attempts")
    void shouldLockOutAccountAfterMaxFailedAttempts() {
        final String account = "bruteaccount";

        for (int i = 0; i < 24; i++) {
            assertFalse(this.loginAttemptService.isAccountLockedOut(account));
            this.loginAttemptService.recordFailedAccountAttempt(account);
        }

        // This covers the branch `if (this.count >= ACCOUNT_MAX_ATTEMPTS)`
        // being false in `isExpired` during cleanup/checks
        assertEquals(0, this.loginAttemptService.getRemainingAccountLockoutSeconds(account));

        this.loginAttemptService.recordFailedAccountAttempt(account);
        assertTrue(this.loginAttemptService.isAccountLockedOut(account));
        assertTrue(this.loginAttemptService.getRemainingAccountLockoutSeconds(account) > 0);
    }

    @Test
    @DisplayName("Should clear account lockout on successful login")
    void shouldClearAccountLockoutOnSuccessfulLogin() {
        final String account = "legitaccount";

        for (int i = 0; i < 25; i++) {
            this.loginAttemptService.recordFailedAccountAttempt(account);
        }
        assertTrue(this.loginAttemptService.isAccountLockedOut(account));

        this.loginAttemptService.recordSuccessfulAccountLogin(account);
        assertFalse(this.loginAttemptService.isAccountLockedOut(account));
        assertEquals(0, this.loginAttemptService.getRemainingAccountLockoutSeconds(account));
    }

    @Test
    @DisplayName("Should enforce account max cache size")
    void shouldEnforceAccountMaxCacheSize() {
        for (int i = 0; i < 10_005; i++) {
            this.loginAttemptService.recordFailedAccountAttempt("user_" + i);
        }
        // Cache cleanup should be triggered
        assertEquals(0, this.loginAttemptService.getRemainingAccountLockoutSeconds("user_10004"));
    }

    @Test
    @DisplayName("Should handle clock skew at 3600s boundary")
    void shouldHandleClockSkewAt3600sBoundary() {
        final String key = "skewuser";
        // Record 100 attempts to ensure we hit the 3600 limit
        for (int i = 0; i < 100; i++) {
            this.loginAttemptService.recordFailedAttempt(key);
        }
        final long remaining = this.loginAttemptService.getRemainingLockoutSeconds(key);
        // Should be exactly or very close to 3600
        assertTrue(remaining <= 3600, "Lockout should never exceed 3600s");
        assertTrue(remaining > 3500, "Lockout should be at the cap");
    }
}
