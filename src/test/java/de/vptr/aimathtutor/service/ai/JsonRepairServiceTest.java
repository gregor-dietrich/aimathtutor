package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class JsonRepairServiceTest {

    @Inject
    private JsonRepairService jsonRepairService;

    // =========================================================================
    // stripQuotationMarks Tests
    // =========================================================================

    @Test
    @DisplayName("stripQuotationMarks should handle null input")
    void stripQuotationMarksShouldHandleNull() {
        assertNull(this.jsonRepairService.stripQuotationMarks(null));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle empty string")
    void stripQuotationMarksShouldHandleEmptyString() {
        assertEquals("", this.jsonRepairService.stripQuotationMarks(""));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle single character string")
    void stripQuotationMarksShouldHandleSingleCharacter() {
        // Single character strings should be returned unchanged (no quotes to strip)
        assertEquals("a", this.jsonRepairService.stripQuotationMarks("a"));
        assertEquals("\"", this.jsonRepairService.stripQuotationMarks("\""));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove normal double quotes")
    void stripQuotationMarksShouldRemoveNormalDoubleQuotes() {
        assertEquals("Hello world", this.jsonRepairService.stripQuotationMarks("\"Hello world\""));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove smart double quotes (U+201C and U+201D)")
    void stripQuotationMarksShouldRemoveSmartDoubleQuotes() {
        // Smart double quotes: " (U+201C) and " (U+201D)
        assertEquals("Hello world", this.jsonRepairService.stripQuotationMarks("\u201CHello world\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should remove smart single quotes (U+2018 and U+2019)")
    void stripQuotationMarksShouldRemoveSmartSingleQuotes() {
        // Smart single quotes: ' (U+2018) and ' (U+2019)
        assertEquals("Hello world", this.jsonRepairService.stripQuotationMarks("\u2018Hello world\u2019"));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle nested quotes")
    void stripQuotationMarksShouldHandleNestedQuotes() {
        // Nested quotes: outer quotes removed, inner preserved
        assertEquals("He said \"hello\"",
                this.jsonRepairService.stripQuotationMarks("\"He said \"hello\"\""));

        // Multiple levels of smart quotes
        assertEquals("inner",
                this.jsonRepairService.stripQuotationMarks("\u201C\u201Cinner\u201D\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should not remove mismatched quotes")
    void stripQuotationMarksShouldNotRemoveMismatchedQuotes() {
        // Mismatched quotes should be left alone
        assertEquals("\"Hello world'", this.jsonRepairService.stripQuotationMarks("\"Hello world'"));
        assertEquals("\u201CHello world\u2019",
                this.jsonRepairService.stripQuotationMarks("\u201CHello world\u2019"));
    }

    @Test
    @DisplayName("stripQuotationMarks should not remove quotes that don't wrap the entire string")
    void stripQuotationMarksShouldNotRemovePartialQuotes() {
        // Quotes in the middle should be preserved
        assertEquals("Hello \"world\" there",
                this.jsonRepairService.stripQuotationMarks("Hello \"world\" there"));
    }

    @Test
    @DisplayName("stripQuotationMarks should trim whitespace before stripping quotes")
    void stripQuotationMarksShouldTrimWhitespace() {
        assertEquals("Hello world", this.jsonRepairService.stripQuotationMarks("  \"Hello world\"  "));
    }

    @Test
    @DisplayName("stripQuotationMarks should handle string with only quotes")
    void stripQuotationMarksShouldHandleOnlyQuotes() {
        // Just matching quotes should result in empty or trimmed content
        assertEquals("", this.jsonRepairService.stripQuotationMarks("\"\""));
        assertEquals("", this.jsonRepairService.stripQuotationMarks("\u201C\u201D"));
    }

    @Test
    @DisplayName("stripQuotationMarks should preserve text without quotes")
    void stripQuotationMarksShouldPreserveTextWithoutQuotes() {
        assertEquals("Hello world", this.jsonRepairService.stripQuotationMarks("Hello world"));
    }

    // =========================================================================
    // repairTruncatedJson Tests
    // =========================================================================

    @Test
    @DisplayName("repairTruncatedJson should handle null input")
    void repairTruncatedJsonShouldHandleNull() {
        assertNull(this.jsonRepairService.repairTruncatedJson(null));
    }

    @Test
    @DisplayName("repairTruncatedJson should handle empty input")
    void repairTruncatedJsonShouldHandleEmpty() {
        assertEquals("", this.jsonRepairService.repairTruncatedJson(""));
    }

    @Test
    @DisplayName("repairTruncatedJson should not modify valid JSON")
    void repairTruncatedJsonShouldNotModifyValidJson() {
        final String validJson = "{\"type\": \"POSITIVE\", \"message\": \"Good job!\"}";
        assertEquals(validJson, this.jsonRepairService.repairTruncatedJson(validJson));
    }

    @Test
    @DisplayName("repairTruncatedJson should add missing closing brace")
    void repairTruncatedJsonShouldAddMissingClosingBrace() {
        final String truncated = "{\"type\": \"POSITIVE\", \"message\": \"Good job!\"";
        final String repaired = this.jsonRepairService.repairTruncatedJson(truncated);
        assertEquals(truncated + "}", repaired);
    }

    @Test
    @DisplayName("repairTruncatedJson should add missing closing bracket and brace")
    void repairTruncatedJsonShouldAddMissingBracketAndBrace() {
        final String truncated = "{\"hints\": [\"hint1\", \"hint2\"";
        final String repaired = this.jsonRepairService.repairTruncatedJson(truncated);
        assertEquals(truncated + "]}", repaired);
    }

    @Test
    @DisplayName("repairTruncatedJson should handle escaped quotes correctly")
    void repairTruncatedJsonShouldHandleEscapedQuotes() {
        // JSON with escaped quote inside string value
        final String json = "{\"message\": \"He said \\\"hello\\\"\"}";
        final String repaired = this.jsonRepairService.repairTruncatedJson(json);
        assertEquals(json, repaired); // Should not modify valid JSON with escaped quotes
    }

    @Test
    @DisplayName("repairTruncatedJson should handle consecutive backslashes before quote")
    void repairTruncatedJsonShouldHandleConsecutiveBackslashes() {
        // Java literal "C:\\\\Users\\\\" becomes the string: C:\\Users\\ In JSON, \\
        // represents a single escaped backslash, so both quotes are unescaped and
        // properly close/open the string value - this is valid JSON
        final String json = "{\"path\": \"C:\\\\Users\\\\\"}";
        final String repaired = this.jsonRepairService.repairTruncatedJson(json);
        assertEquals(json, repaired); // Should not modify valid JSON
    }

    @Test
    @DisplayName("repairTruncatedJson should correctly identify unescaped quote after escaped backslash")
    void repairTruncatedJsonShouldIdentifyUnescapedQuoteAfterEscapedBackslash() {
        // Truncated JSON: {"message": "path is C:\\"}
        // The final quote closes the string (backslashes are escaped, not the quote)
        final String truncated = "{\"message\": \"path is C:\\\\\"";
        final String repaired = this.jsonRepairService.repairTruncatedJson(truncated);
        assertEquals(truncated + "}", repaired); // Should add closing brace
    }

    // =========================================================================
    // extractFeedbackFromTruncatedResponse Tests
    // =========================================================================

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should handle null input")
    void extractFeedbackFromTruncatedResponseShouldHandleNull() {
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(null);
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.HINT, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should handle empty input")
    void extractFeedbackFromTruncatedResponseShouldHandleEmpty() {
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse("");
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.HINT, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should extract message from truncated JSON")
    void extractFeedbackFromTruncatedResponseShouldExtractMessage() {
        final String truncated = "{\"type\": \"POSITIVE\", \"message\": \"Great work!\", \"confidence\": 0.9";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Great work!", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should extract CORRECTIVE type")
    void extractFeedbackFromTruncatedResponseShouldExtractCorrectiveType() {
        final String truncated = "{\"type\": \"CORRECTIVE\", \"message\": \"Check your work\"";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Check your work", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.CORRECTIVE, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should extract HINT type")
    void extractFeedbackFromTruncatedResponseShouldExtractHintType() {
        final String truncated = "{\"type\": \"HINT\", \"message\": \"Try simplifying first\"";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Try simplifying first", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.HINT, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should extract SUGGESTION type")
    void extractFeedbackFromTruncatedResponseShouldExtractSuggestionType() {
        final String truncated = "{\"type\": \"SUGGESTION\", \"message\": \"Consider factoring\"";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Consider factoring", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.SUGGESTION, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should handle escaped quotes in message")
    void extractFeedbackFromTruncatedResponseShouldHandleEscapedQuotesInMessage() {
        // Message contains escaped quotes: He said "hello"
        final String json = "{\"type\": \"POSITIVE\", \"message\": \"He said \\\"hello\\\"\"}";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(json);
        assertNotNull(feedback);
        assertEquals("He said \"hello\"", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should default to POSITIVE for unknown type")
    void extractFeedbackFromTruncatedResponseShouldDefaultToPositive() {
        final String truncated = "{\"type\": \"UNKNOWN\", \"message\": \"Some feedback\"";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Some feedback", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type);
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should handle missing type field")
    void extractFeedbackFromTruncatedResponseShouldHandleMissingType() {
        final String truncated = "{\"message\": \"Just a message\"}";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(truncated);
        assertNotNull(feedback);
        assertEquals("Just a message", feedback.message);
        assertEquals(AiFeedbackDto.FeedbackType.POSITIVE, feedback.type); // Defaults to POSITIVE
    }

    @Test
    @DisplayName("extractFeedbackFromTruncatedResponse should return generic hint for garbage input")
    void extractFeedbackFromTruncatedResponseShouldReturnGenericForGarbage() {
        final String garbage = "This is not JSON at all";
        final AiFeedbackDto feedback = this.jsonRepairService.extractFeedbackFromTruncatedResponse(garbage);
        assertNotNull(feedback);
        assertEquals(AiFeedbackDto.FeedbackType.HINT, feedback.type);
        assertNotNull(feedback.message);
    }
}
