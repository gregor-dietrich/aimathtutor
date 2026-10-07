package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        final String target = "user_target";
        // Record enough attempts to cause a lockout
        for (int i = 0; i < 30; i++) {
            this.loginAttemptService.recordFailedAccountAttempt(target);
        }
        assertTrue(this.loginAttemptService.getRemainingAccountLockoutSeconds(target) > 0);

        // Flood the cache with many distinct accounts to trigger eviction
        for (int i = 0; i < 10_005; i++) {
            this.loginAttemptService.recordFailedAccountAttempt("user_" + i);
        }

        // Target should have been evicted, so lockout is now 0
        assertEquals(0, this.loginAttemptService.getRemainingAccountLockoutSeconds(target),
                "Target account should have been evicted from the bounded cache");
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
        // Should be exactly 3600
        assertEquals(3600, remaining, "Lockout should be exactly capped at 3600s");
    }

    /** A clock the test moves by hand, to cross the throttle windows without waiting. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(final Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }

    private static LoginAttemptService serviceWith(final MutableClock clock) {
        final var service = new LoginAttemptService();
        service.clock = clock;
        return service;
    }

    @Test
    @DisplayName("Password changes are allowed 5 times per session, then refused without counting")
    void passwordChangeAllowsFiveAttempts() {
        final var token = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(token));
        }
        assertFalse(this.loginAttemptService.tryRecordPasswordChangeAttempt(token));
        assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(UUID.randomUUID().toString()),
                "another session has its own bucket");
    }

    @Test
    @DisplayName("The password-change cap does not reset after a minute, only 15 minutes after the 5th attempt")
    void passwordChangeCapDoesNotResetEveryMinute() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryRecordPasswordChangeAttempt("t"));
        }

        clock.advance(Duration.ofSeconds(61));
        assertFalse(service.tryRecordPasswordChangeAttempt("t"), "still locked after 61 s");
        clock.advance(Duration.ofMinutes(13));
        assertFalse(service.tryRecordPasswordChangeAttempt("t"), "still locked just before 15 minutes");
        clock.advance(Duration.ofMinutes(1));
        assertTrue(service.tryRecordPasswordChangeAttempt("t"), "window restarts after 15 minutes");
    }

    @Test
    @DisplayName("The password-change window restarts 15 minutes after its first attempt, not after each attempt")
    void passwordChangeWindowRunsFromTheFirstAttempt() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        for (int i = 0; i < 4; i++) {
            assertTrue(service.tryRecordPasswordChangeAttempt("t"));
            clock.advance(Duration.ofMinutes(4));
        }
        // 16 minutes since the first attempt: a new window, so 5 more are allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryRecordPasswordChangeAttempt("t"));
        }
        assertFalse(service.tryRecordPasswordChangeAttempt("t"));
    }

    @Test
    @DisplayName("Clearing the password-change attempts frees only that session")
    void clearingPasswordChangeAttemptsIsPerSession() {
        final var a = UUID.randomUUID().toString();
        final var b = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(a));
            assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(b));
        }

        this.loginAttemptService.clearPasswordChangeAttempts(a);

        assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(a));
        assertFalse(this.loginAttemptService.tryRecordPasswordChangeAttempt(b));
    }

    @Test
    @DisplayName("Login keys never touch the password-change bucket, whatever their text")
    void passwordChangeHasItsOwnKeySpace() {
        final var token = UUID.randomUUID().toString();
        for (final var key : new String[] { token, "change-password:" + token }) {
            for (int i = 0; i < 6; i++) {
                this.loginAttemptService.recordFailedAttempt(key);
                this.loginAttemptService.recordFailedAccountAttempt(key);
            }
        }
        assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(token), "login failures are not counted");

        for (int i = 0; i < 4; i++) {
            assertTrue(this.loginAttemptService.tryRecordPasswordChangeAttempt(token));
        }
        assertFalse(this.loginAttemptService.tryRecordPasswordChangeAttempt(token));
        this.loginAttemptService.recordSuccessfulLogin(token);
        this.loginAttemptService.recordSuccessfulAccountLogin(token);
        assertFalse(this.loginAttemptService.tryRecordPasswordChangeAttempt(token), "login success does not clear it");
    }

    @Test
    @DisplayName("Concurrent password-change attempts never exceed the cap")
    void passwordChangeAttemptsAreAtomic() throws Exception {
        final var token = UUID.randomUUID().toString();
        final var threads = 32;
        final var executor = Executors.newFixedThreadPool(threads);
        try {
            final var start = new CountDownLatch(1);
            final var results = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return this.loginAttemptService.tryRecordPasswordChangeAttempt(token);
                }));
            }
            start.countDown();
            var allowed = 0;
            for (final var result : results) {
                allowed += result.get(10, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertEquals(5, allowed);
        } finally {
            executor.shutdownNow();
        }
    }
}
