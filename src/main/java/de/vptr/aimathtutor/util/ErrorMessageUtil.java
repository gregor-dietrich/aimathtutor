package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for extracting error messages from HTTP responses.
 */
public class ErrorMessageUtil {

    private static final Logger LOG = Logger.getLogger(ErrorMessageUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Extracts error message from HTTP response body.
     * Attempts to parse structured error response, falls back to status text.
     */
    public static String extractErrorMessage(final Response response) {
        try {
            // Try to read the response body as a structured error
            if (response.hasEntity() && response.getStatus() >= 400) {
                final String responseBody = response.readEntity(String.class);
                if (responseBody != null && !responseBody.isBlank()) {
                    final String trimmed = responseBody.trim();
                    // Try structured JSON first
                    if (trimmed.startsWith("{")) {
                        try {
                            final JsonNode root = OBJECT_MAPPER.readTree(trimmed);
                            final String msg = findMessageNode(root);
                            if (msg != null && !msg.isEmpty()) {
                                return msg;
                            }
                        } catch (final Exception e) {
                            // JSON parsing failed — fall through to regex/plain-text fallback
                        }
                        // Fallback for malformed JSON: try regex extraction
                        final String regexMsg = extractMessageWithRegex(trimmed);
                        if (regexMsg != null) {
                            return regexMsg;
                        }
                    }
                    // If no structured message found, return the whole body (might be plain text)
                    return trimmed;
                }
            }
        } catch (final Exception e) {
            LOG.warnf(e, "Failed to extract error message from %s response",  response.getStatus());
        }

        // Fall back to HTTP status
        return "HTTP " + response.getStatus();
    }

    private static String findMessageNode(final JsonNode root) {
        if (root == null) {
            return null;
        }
        // Check top-level message
        if (root.has("message") && root.get("message").isTextual()) {
            return root.get("message").asText().trim();
        }
        // Check nested error.message
        if (root.has("error") && root.get("error").isObject()
                && root.get("error").has("message")
                && root.get("error").get("message").isTextual()) {
            return root.get("error").get("message").asText().trim();
        }
        return null;
    }

    private static String extractMessageWithRegex(final String responseBody) {
        if (!responseBody.contains("\"message\"")) {
            return null;
        }
        var messageStart = responseBody.indexOf("\"message\"");
        if (messageStart == -1) {
            return null;
        }
        messageStart = responseBody.indexOf(":", messageStart) + 1;
        final var messageEnd = responseBody.indexOf("\"", messageStart + 1);
        if (messageEnd == -1) {
            return null;
        }
        final var message = responseBody.substring(messageStart + 1, messageEnd);
        return message.trim();
    }
}
