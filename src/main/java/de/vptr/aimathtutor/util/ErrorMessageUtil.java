package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for extracting error messages from HTTP responses.
 */
public final class ErrorMessageUtil {

    private ErrorMessageUtil() {
    }

    private static final Logger LOG = Logger.getLogger(ErrorMessageUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Extracts error message from HTTP response body. Attempts to parse structured error response, falls back to status
     * text.
     */
    public static String extractErrorMessage(final Response response) {
        final int status = response != null ? response.getStatus() : -1;
        try {
            if (response == null) {
                return "HTTP " + status;
            }
            if (response.hasEntity() && status >= 400) {
                final String responseBody = response.readEntity(String.class);
                if (responseBody != null && !responseBody.isBlank()) {
                    final String trimmed = responseBody.trim();
                    if (trimmed.startsWith("{")) {
                        JsonNode root = null;
                        try {
                            root = OBJECT_MAPPER.readTree(trimmed);
                            final String msg = findMessageNode(root);
                            if (msg != null && !msg.isEmpty()) {
                                return msg;
                            }
                        } catch (final Exception e) {
                            LOG.debugf(e, "JSON parse failed for error response, trying regex fallback");
                        }
                        final String regexMsg = extractMessageWithRegex(trimmed, root);
                        if (regexMsg != null) {
                            return regexMsg;
                        }
                    }
                    return trimmed;
                }
            }
        } catch (final Exception e) {
            LOG.warnf(e, "Failed to extract error message from HTTP %s response", status);
        }

        return "HTTP " + status;
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
        if (root.has("error") && root.get("error").isObject() && root.get("error").has("message")
                && root.get("error").get("message").isTextual()) {
            return root.get("error").get("message").asText().trim();
        }
        return null;
    }

    private static String extractMessageWithRegex(final String responseBody, final JsonNode alreadyParsed) {
        if (alreadyParsed != null) {
            final String msg = findMessageNode(alreadyParsed);
            if (msg != null && !msg.isEmpty()) {
                return msg;
            }
        } else {
            try {
                final JsonNode root = OBJECT_MAPPER.readTree(responseBody);
                final String msg = findMessageNode(root);
                if (msg != null && !msg.isEmpty()) {
                    return msg;
                }
            } catch (final Exception e) {
                // Not valid JSON, fall through to regex
            }
        }

        if (!responseBody.contains("\"message\"")) {
            return null;
        }
        var messageStart = responseBody.indexOf("\"message\"");
        if (messageStart == -1) {
            return null;
        }
        final int colonPos = responseBody.indexOf(":", messageStart);
        if (colonPos == -1) {
            return null;
        }
        messageStart = colonPos + 1;
        while (messageStart < responseBody.length() && Character.isWhitespace(responseBody.charAt(messageStart))) {
            messageStart++;
        }
        if (messageStart >= responseBody.length() || responseBody.charAt(messageStart) != '"') {
            return null;
        }
        final var messageEnd = responseBody.indexOf("\"", messageStart + 1);
        if (messageEnd == -1) {
            return null;
        }
        final var message = responseBody.substring(messageStart + 1, messageEnd);
        final String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
