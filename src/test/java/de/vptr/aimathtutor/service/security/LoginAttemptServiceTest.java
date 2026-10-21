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
    @DisplayName("Flooding the account map past its size cap never lifts a locked account")
    void sizeCapNeverEvictsLockedAccountBucket() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        for (int i = 0; i < 25; i++) {
            service.recordFailedAccountAttempt("target");
        }
        for (int i = 0; i < 24; i++) {
            service.recordFailedAccountAttempt("old");
        }
        clock.advance(Duration.ofSeconds(1));
        for (int i = 0; i <= 10_000; i++) {
            service.recordFailedAccountAttempt("flood-" + i);
        }

        assertTrue(service.isAccountLockedOut("target"));
        assertEquals(599, service.getRemainingAccountLockoutSeconds("target"));
        service.recordFailedAccountAttempt("old");
        assertTrue(service.isAccountLockedOut("old"), "the bucket at 24 failures outlived the count-1 flood");
    }

    @Test
    @DisplayName("Flooding the login map past its size cap never lifts a locked key")
    void sizeCapNeverEvictsLockedLoginBucket() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "target", 5);
        fail(service, "old", 4);
        clock.advance(Duration.ofSeconds(1));
        for (int i = 0; i <= 10_000; i++) {
            service.recordFailedAttempt("flood-" + i);
        }

        assertTrue(service.isLockedOut("target"));
        assertEquals(29, service.getRemainingLockoutSeconds("target"));
        assertEquals(30, service.recordFailedAttempt("old"), "the older bucket with the higher count was kept");
    }

    @Test
    @DisplayName("Flooding the login map keeps a key whose lock ended but whose count is retained")
    void sizeCapKeepsRetainedCountAfterLockEnds() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        final var key = "retained-" + UUID.randomUUID();
        fail(service, key, 5);
        clock.advance(Duration.ofSeconds(31));
        assertFalse(service.isLockedOut(key));
        for (int i = 0; i <= 10_000; i++) {
            service.recordFailedAttempt("flood-" + i);
        }

        assertEquals(60, service.recordFailedAttempt(key), "escalation survived the flood");
    }

    @Test
    @DisplayName("A clock stepped backwards does not lock a key below the threshold")
    void backwardClockStepDoesNotLockBelowThreshold() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        final var key = "back-" + UUID.randomUUID();
        service.recordFailedAttempt(key);
        clock.advance(Duration.ofSeconds(-10));

        assertFalse(service.isLockedOut(key));
        assertEquals(0, service.getRemainingLockoutSeconds(key));
    }

    @Test
    @DisplayName("A clock stepped backwards does not extend a real lock")
    void backwardClockStepDoesNotExtendLock() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        final var key = "back-lock-" + UUID.randomUUID();
        fail(service, key, 5);
        clock.advance(Duration.ofSeconds(-10));

        assertTrue(service.isLockedOut(key));
        assertEquals(30, service.getRemainingLockoutSeconds(key));
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

    private static void fail(final LoginAttemptService service, final String key, final int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailedAttempt(key);
        }
    }

    @Test
    @DisplayName("A lock of 30 s ends after 30 s")
    void lockEndsWhenItsDurationHasPassed() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "k", 5);
        assertTrue(service.isLockedOut("k"));
        assertEquals(30, service.getRemainingLockoutSeconds("k"));

        clock.advance(Duration.ofSeconds(30));
        assertFalse(service.isLockedOut("k"));
        assertEquals(0, service.getRemainingLockoutSeconds("k"));
    }

    @Test
    @DisplayName("Failures survive a lock, so the next locks escalate")
    void locksEscalateAfterTheyEnd() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "k", 5);

        clock.advance(Duration.ofSeconds(31));
        assertEquals(60, service.recordFailedAttempt("k"));
        clock.advance(Duration.ofSeconds(61));
        assertEquals(120, service.recordFailedAttempt("k"));
    }

    @Test
    @DisplayName("Failures below the threshold are kept for an hour")
    void failuresBelowTheThresholdAreKept() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "k", 4);

        clock.advance(Duration.ofSeconds(31));
        assertEquals(30, service.recordFailedAttempt("k"));
    }

    @Test
    @DisplayName("Failures are forgotten an hour after the lock ends")
    void failuresAreForgottenAnHourAfterTheLock() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "k", 5);
        clock.advance(Duration.ofSeconds(3630));
        assertEquals(60, service.recordFailedAttempt("k"), "still remembered at 30 s + 1 h");

        final var other = serviceWith(clock);
        fail(other, "k", 5);
        clock.advance(Duration.ofSeconds(3631));
        assertEquals(0, other.recordFailedAttempt("k"), "forgotten just after 30 s + 1 h");
    }

    @Test
    @DisplayName("A successful login forgets the failures at once")
    void successfulLoginForgetsFailures() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "k", 5);
        clock.advance(Duration.ofSeconds(31));

        service.recordSuccessfulLogin("k");

        assertEquals(0, service.recordFailedAttempt("k"));
    }

    @Test
    @DisplayName("Lock lengths follow the doubling schedule up to the cap, at any count")
    void lockLengthsFollowTheSchedule() {
        final var service = serviceWith(new MutableClock());
        for (int count = 1; count <= 100; count++) {
            final long expected = count < 5 ? 0 : count < 12 ? 30L << (count - 5) : 3600;
            assertEquals(expected, service.recordFailedAttempt("k"), "lock after failure " + count);
        }
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
    @DisplayName("Flooding the password-change map past its size cap never lifts a session's lock")
    void sizeCapNeverEvictsLockedPasswordChangeBucket() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryRecordPasswordChangeAttempt("locked"));
        }
        clock.advance(Duration.ofSeconds(1));
        // Past the 10,000-entry cap, all newer than the locked bucket
        for (int i = 0; i <= 10_000; i++) {
            service.tryRecordPasswordChangeAttempt("flood-" + i);
        }

        assertFalse(service.tryRecordPasswordChangeAttempt("locked"));
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
