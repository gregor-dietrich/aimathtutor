package de.vptr.aimathtutor.service.security;

import static de.vptr.aimathtutor.service.security.LoginAttemptServiceTest.fail;
import static de.vptr.aimathtutor.service.security.LoginAttemptServiceTest.serviceWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.security.LoginAttemptServiceTest.MutableClock;

/**
 * The sign-in credit of {@link LoginAttemptService#recordSuccessfulLogin}; plain JUnit on a service with a manual
 * clock.
 */
class LoginAttemptSignInCreditTest {

    @Test
    @DisplayName("The first account in a sign-in window forgives nothing beyond its own failures")
    void firstAccountForgivesNothingExtra() {
        final var service = serviceWith(new MutableClock());
        fail(service, "c", 4);

        service.recordSuccessfulLogin("a:c", "c");

        assertTrue(service.recordFailedAttempt("c") > 0, "still 4, so the fifth locks");
    }

    @Test
    @DisplayName("A second, different account forgives exactly one more failure")
    void secondAccountForgivesOne() {
        final var service = serviceWith(new MutableClock());
        fail(service, "c", 4);

        service.recordSuccessfulLogin("a:c", "c");
        service.recordSuccessfulLogin("b:c", "c");

        assertEquals(0, service.recordFailedAttempt("c"), "3 + 1 is 4");
        assertTrue(service.recordFailedAttempt("c") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("A repeated account forgives nothing")
    void repeatedAccountForgivesNothing() {
        final var service = serviceWith(new MutableClock());
        fail(service, "c", 4);

        service.recordSuccessfulLogin("a:c", "c");
        service.recordSuccessfulLogin("b:c", "c");
        service.recordSuccessfulLogin("a:c", "c");
        service.recordSuccessfulLogin("b:c", "c");

        assertEquals(0, service.recordFailedAttempt("c"), "3 + 1 is 4");
        assertTrue(service.recordFailedAttempt("c") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("A window is still open 3600 s after it began")
    void windowStaysOpenForAnHour() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        service.recordSuccessfulLogin("a:c", "c");
        clock.advance(Duration.ofHours(1));
        fail(service, "c", 4);
        service.recordSuccessfulLogin("b:c", "c");
        assertEquals(0, service.recordFailedAttempt("c"), "b earned 1: 4 - 1 + 1 is 4");
        assertTrue(service.recordFailedAttempt("c") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("A window expires after 3600 s: the next account then forgives nothing")
    void accountAfterWindowForgivesNothing() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        service.recordSuccessfulLogin("a:c", "c");
        clock.advance(Duration.ofSeconds(3601));
        fail(service, "c", 4);

        service.recordSuccessfulLogin("b:c", "c");

        assertTrue(service.recordFailedAttempt("c") > 0, "still 4, so the fifth locks");
    }

    @Test
    @DisplayName("The credit is never banked, with no client bucket or an emptied one")
    void creditIsNotBanked() {
        final var service = serviceWith(new MutableClock());
        service.recordSuccessfulLogin("a:c", "c");
        service.recordSuccessfulLogin("b:c", "c");
        service.recordSuccessfulLogin("d:c", "c");
        fail(service, "c", 1);
        service.recordSuccessfulLogin("e:c", "c");
        service.recordSuccessfulLogin("f:c", "c");

        for (var i = 0; i < 4; i++) {
            assertEquals(0, service.recordFailedAttempt("c"));
        }
        assertFalse(service.isLockedOut("c"), "a later failure counts from 1: four are not enough");
        assertTrue(service.recordFailedAttempt("c") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("Own failures and the credit are subtracted together")
    void ownFailuresAndCreditAddUp() {
        final var service = serviceWith(new MutableClock());
        service.recordSuccessfulLogin("a:c", "c");
        fail(service, "b:c", 2);
        fail(service, "c", 6);

        service.recordSuccessfulLogin("b:c", "c");

        assertEquals(0, service.recordFailedAttempt("c"), "6 - 2 - 1 = 3, then 4");
        assertTrue(service.recordFailedAttempt("c") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("An expired own bucket forgives nothing")
    void expiredOwnBucketForgivesNothing() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "me:c", 5);
        clock.advance(Duration.ofSeconds(3631));
        fail(service, "c", 4);

        service.recordSuccessfulLogin("me:c", "c");

        assertTrue(service.recordFailedAttempt("c") > 0, "the 4 failures stay");
    }

    @Test
    @DisplayName("An expired client bucket is removed by a sign-in")
    void expiredClientBucketIsRemoved() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        fail(service, "c", 3);
        fail(service, "me:c", 1);
        clock.advance(Duration.ofSeconds(3601));

        service.recordSuccessfulLogin("me:c", "c");

        for (var i = 0; i < 4; i++) {
            assertEquals(0, service.recordFailedAttempt("c"));
        }
        assertTrue(service.recordFailedAttempt("c") > 0, "counted from 1 again");
    }

    @Test
    @DisplayName("Flooding the sign-in map past its size cap evicts the windows with the fewest accounts first")
    void sizeCapEvictsSmallestWindowsFirst() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        service.recordSuccessfulLogin("x:one", "one");
        service.recordSuccessfulLogin("a:two", "two");
        service.recordSuccessfulLogin("b:two", "two");
        clock.advance(Duration.ofSeconds(1));
        for (int i = 0; i <= 10_000; i++) {
            service.recordSuccessfulLogin("u:flood-" + i, "flood-" + i);
        }
        fail(service, "one", 4);
        fail(service, "two", 4);

        service.recordSuccessfulLogin("y:one", "one");
        service.recordSuccessfulLogin("c:two", "two");

        assertTrue(service.recordFailedAttempt("one") > 0, "the evicted one-account window earns no credit");
        assertEquals(0, service.recordFailedAttempt("two"), "the two-account window survived: 4 - 1 + 1 is 4");
        assertTrue(service.recordFailedAttempt("two") > 0, "the fifth locks");
    }

    @Test
    @DisplayName("The periodic cleanup drops expired windows, so they cannot crowd out a live one")
    void cleanupDropsExpiredWindows() {
        final var clock = new MutableClock();
        final var service = serviceWith(clock);
        for (int i = 0; i < 10_000; i++) {
            service.recordSuccessfulLogin("a:old-" + i, "old-" + i);
            service.recordSuccessfulLogin("b:old-" + i, "old-" + i);
        }
        clock.advance(Duration.ofSeconds(3601));
        service.recordSuccessfulLogin("x:keep", "keep");
        service.recordSuccessfulLogin("z:other", "other");
        fail(service, "keep", 4);

        service.recordSuccessfulLogin("y:keep", "keep");

        assertEquals(0, service.recordFailedAttempt("keep"), "the live window was not evicted: 4 - 1 + 1 is 4");
        assertTrue(service.recordFailedAttempt("keep") > 0, "the fifth locks");
    }
}
