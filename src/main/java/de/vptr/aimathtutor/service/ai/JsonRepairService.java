package de.vptr.aimathtutor.service.ai;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for parsing, repairing, and sanitizing AI provider JSON responses.
 */
@ApplicationScoped
public class JsonRepairService {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRepairService.class);

    // Smart quote characters for stripping from AI responses
    // Using constants avoids checkstyle's AvoidEscapedUnicodeCharacters warning
    private static final String LEFT_DOUBLE_QUOTE = "\u201C"; // "
    private static final String RIGHT_DOUBLE_QUOTE = "\u201D"; // "
    private static final String LEFT_SINGLE_QUOTE = "\u2018"; // '
    private static final String RIGHT_SINGLE_QUOTE = "\u2019"; // '

    @Inject
    ObjectMapper objectMapper;

    /**
     * Parses AI provider's JSON response into AIFeedbackDto.
     * Falls back to extracting message if JSON parsing fails.
     *
     * @param jsonResponse the raw JSON response from the AI provider
     * @return parsed feedback DTO
     */
    public AiFeedbackDto parseFeedbackFromJson(final String jsonResponse) {
        try {
            // Try to extract JSON from response (AI provider might wrap it in markdown)
            var json = jsonResponse.trim();

            // Remove markdown code block if present
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            // Try to repair truncated JSON (missing closing braces/brackets)
            json = this.repairTruncatedJson(json);

            // Parse JSON to AIFeedbackDto
            final var feedback = this.objectMapper.readValue(json, AiFeedbackDto.class);
            feedback.clampConfidence();

            // Set timestamp
            feedback.timestamp = LocalDateTime.now();

            LOG.debug("Successfully parsed AI provider response as JSON");
            return feedback;

        } catch (final IOException e) {
            LOG.warn("Failed to parse AI provider response as JSON, creating simple feedback", e);

            // Fallback: try to extract the message field from truncated JSON
            final var feedback = this.extractFeedbackFromTruncatedResponse(jsonResponse);
            feedback.confidence = 0.7;
            return feedback;
        }
    }

    /**
     * Attempts to repair truncated JSON by adding missing closing braces and
     * brackets.
     * This handles cases where Ollama runs out of tokens mid-response.
     *
     * @param json the potentially truncated JSON string
     * @return repaired JSON string
     */
    public String repairTruncatedJson(final String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // Count opening and closing braces/brackets
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        // Track whether the previous character is an unescaped backslash
        // This correctly handles consecutive backslashes (e.g., \\" is escaped
        // backslash + quote)
        boolean prevCharIsEscape = false;

        for (int i = 0; i < json.length(); ++i) {
            final char c = json.charAt(i);
            // Track string state (ignore escaped quotes)
            if (c == '"' && !prevCharIsEscape) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    openBraces++;
                } else if (c == '}') {
                    openBraces--;
                } else if (c == '[') {
                    openBrackets++;
                } else if (c == ']') {
                    openBrackets--;
                }
            }
            // A backslash is only an escape if it's not itself escaped
            prevCharIsEscape = (c == '\\' && !prevCharIsEscape);
        }

        // If unbalanced, try to repair
        if (openBraces > 0 || openBrackets > 0) {
            LOG.debug("Attempting to repair truncated JSON: {} open braces, {} open brackets",
                    openBraces, openBrackets);

            final var repaired = new StringBuilder(json);

            // Close any open string
            if (inString) {
                repaired.append('"');
            }

            // Close open brackets first (they're usually inside braces)
            for (int i = 0; i < openBrackets; ++i) {
                repaired.append(']');
            }

            // Then close open braces
            for (int i = 0; i < openBraces; ++i) {
                repaired.append('}');
            }

            return repaired.toString();
        }

        return json;
    }

    /**
     * Extracts feedback from a truncated or malformed AI response.
     * Tries to salvage the message field if present.
     *
     * @param response the raw AI response string
     * @return feedback DTO extracted from the response
     */
    public AiFeedbackDto extractFeedbackFromTruncatedResponse(final String response) {
        if (response == null || response.isEmpty()) {
            return AiFeedbackDto.hint("I'm having trouble analyzing that step. Try another action!");
        }

        // Try to extract the "message" field using regex
        // Pattern handles escaped quotes within the value: matches
        // non-quote/non-backslash chars OR escape sequences
        final var messagePattern = Pattern.compile(
                "\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                Pattern.CASE_INSENSITIVE);
        final var matcher = messagePattern.matcher(response);

        if (matcher.find()) {
            final String extractedMessage = this.unescapeJsonString(matcher.group(1));
            LOG.debug("Extracted message from truncated response: {}", extractedMessage);

            // Try to determine the type
            // Pattern handles escaped quotes within the value: matches
            // non-quote/non-backslash chars OR escape sequences
            final var typePattern = Pattern.compile(
                    "\"type\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                    Pattern.CASE_INSENSITIVE);
            final var typeMatcher = typePattern.matcher(response);

            AiFeedbackDto feedback;
            if (typeMatcher.find()) {
                final String type = typeMatcher.group(1).toUpperCase();
                feedback = switch (type) {
                    case "CORRECTIVE" -> AiFeedbackDto.corrective(extractedMessage);
                    case "HINT" -> AiFeedbackDto.hint(extractedMessage);
                    case "SUGGESTION" -> AiFeedbackDto.suggestion(extractedMessage);
                    default -> AiFeedbackDto.positive(extractedMessage);
                };
            } else {
                feedback = AiFeedbackDto.positive(extractedMessage);
            }

            return feedback;
        }

        // If we can't extract anything useful, return a generic response
        LOG.debug("Could not extract message from response, using generic feedback");
        return AiFeedbackDto.hint("Keep going! Try your next step.");
    }

    /**
     * Unescapes common JSON string escape sequences.
     *
     * @param text The JSON-escaped text
     * @return The text with escape sequences replaced by actual characters
     */
    private String unescapeJsonString(final String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * Strips matching leading and trailing quotation marks from a string.
     * Only removes quotes if they wrap the entire string (both start and end
     * match).
     * Handles both double quotes (") and smart quotes.
     *
     * @param text The text to process
     * @return The text with quotation marks removed, or the original text if null
     */
    public String stripQuotationMarks(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.trim();

        // Remove matching quotation marks (only if both start and end match)
        // Handles: "text", "text" (smart double), 'text' (smart single)
        // Length check (> 1) prevents StringIndexOutOfBoundsException for single-char
        // strings
        while (text.length() > 1) {
            if ((text.startsWith("\"") && text.endsWith("\""))
                    || (text.startsWith(LEFT_DOUBLE_QUOTE) && text.endsWith(RIGHT_DOUBLE_QUOTE))
                    || (text.startsWith(LEFT_SINGLE_QUOTE) && text.endsWith(RIGHT_SINGLE_QUOTE))) {
                text = text.substring(1, text.length() - 1).trim();
            } else {
                break;
            }
        }

        return text;
    }
}
