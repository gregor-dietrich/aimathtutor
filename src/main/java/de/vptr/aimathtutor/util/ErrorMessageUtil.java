package de.vptr.aimathtutor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for extracting error messages from HTTP responses.
 */
public class ErrorMessageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMessageUtil.class);

    /**
     * Extracts error message from HTTP response body.
     * Attempts to parse structured error response, falls back to status text.
     */
    public static String extractErrorMessage(final Response response) {
        try {
            // Try to read the response body as a structured error
            if (response.hasEntity() && response.getStatus() >= 400) {
                final String responseBody = response.readEntity(String.class);
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    if (responseBody.contains("\"message\"")) {
                        int messageStart = responseBody.indexOf("\"message\"");
                        if (messageStart != -1) {
                            messageStart = responseBody.indexOf(":", messageStart) + 1;
                            final int messageEnd = responseBody.indexOf("\"", messageStart + 1);
                            if (messageEnd != -1) {
                                final String message = responseBody.substring(messageStart + 1, messageEnd);
                                return message.trim();
                            }
                        }
                    }
                    // If no structured message found, return the whole body (might be plain text)
                    return responseBody.trim();
                }
            }
        } catch (final Exception e) {
            LOG.warn("Failed to extract error message from {} response: {}", response.getStatus(), e.getMessage());
        }

        // Fall back to HTTP status
        return "HTTP " + response.getStatus();
    }
}
