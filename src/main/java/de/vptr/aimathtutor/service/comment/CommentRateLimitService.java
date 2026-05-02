package de.vptr.aimathtutor.service.comment;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.repository.CommentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Service for enforcing comment rate limits.
 */
@ApplicationScoped
public class CommentRateLimitService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentRateLimitService.class);

    private static final long RATE_LIMIT_WINDOW_SECONDS = 5;
    private static final int RATE_LIMIT_DAILY = 200;

    @Inject
    CommentRepository commentRepository;

    /**
     * Checks rate limiting for a user. Enforces a 5-second window between
     * comments and a daily limit of 200 comments.
     *
     * @param userId the user ID
     * @throws WebApplicationException if rate limit is exceeded
     */
    public void checkRateLimit(final Long userId) {
        if (userId == null) {
            throw new WebApplicationException("User ID is required", Response.Status.BAD_REQUEST);
        }
        // Get user's last comment timestamp
        final LocalDateTime fiveSecondsAgo = LocalDateTime.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        final long recentCount = this.commentRepository.countByUserSince(userId, fiveSecondsAgo);

        if (recentCount > 0) {
            LOG.debug("Rate limit exceeded (5-second window): userId={}, recentCount={}", userId, recentCount);
            throw new WebApplicationException("Please wait before posting another comment",
                    Response.Status.TOO_MANY_REQUESTS);
        }

        // Check daily limit
        final LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        final long dailyCount = this.commentRepository.countByUserSince(userId, oneDayAgo);

        if (dailyCount >= RATE_LIMIT_DAILY) {
            LOG.warn("Daily comment limit exceeded: userId={}, dailyCount={}, limit={}", userId, dailyCount,
                    RATE_LIMIT_DAILY);
            throw new WebApplicationException("Daily comment limit exceeded",
                    Response.Status.TOO_MANY_REQUESTS);
        }
    }
}
