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
class RateLimitServiceTest {

    @Inject
    RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // Service is stateful; we test against the live instance
    }

    @Test
    @DisplayName("Should allow calls under the limit")
    void shouldAllowCallsUnderLimit() {
        final String userId = "gooduser";

        for (int i = 0; i < 10; i++) {
            assertTrue(this.rateLimitService.tryConsume(userId), "Call " + i + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should reject null or blank user IDs")
    void shouldRejectNullOrBlankUserIds() {
        assertFalse(this.rateLimitService.tryConsume(null));
        assertFalse(this.rateLimitService.tryConsume(""));
        assertFalse(this.rateLimitService.tryConsume("   "));
    }

    @Test
    @DisplayName("Should return zero cooldown for unknown users")
    void shouldReturnZeroCooldownForUnknownUsers() {
        assertEquals(0, this.rateLimitService.getRemainingCooldownSeconds("unknown"));
        assertEquals(0, this.rateLimitService.getRemainingCooldownSeconds(null));
        assertEquals(0, this.rateLimitService.getRemainingCooldownSeconds(""));
    }
}
