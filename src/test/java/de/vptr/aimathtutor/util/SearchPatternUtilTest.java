package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchPatternUtilTest {

    @Test
    @DisplayName("escapeLike escapes the escape character before the wildcards")
    void escapeLikeEscapesEscapeCharFirst() {
        // The escape char must be doubled first, otherwise the escapes added for % and _ would be escaped.
        assertEquals("!!", SearchPatternUtil.escapeLike("!"));
        assertEquals("!%", SearchPatternUtil.escapeLike("%"));
        assertEquals("!_", SearchPatternUtil.escapeLike("_"));
        assertEquals("a!!!%!_b", SearchPatternUtil.escapeLike("a!%_b"));
    }

    @Test
    @DisplayName("escapeLike leaves ordinary characters untouched")
    void escapeLikeLeavesOrdinaryCharsUntouched() {
        assertEquals("foo!!bar@example.com", SearchPatternUtil.escapeLike("foo!bar@example.com"));
        assertEquals("ordinary text", SearchPatternUtil.escapeLike("ordinary text"));
        assertEquals("", SearchPatternUtil.escapeLike(""));
    }

    @Test
    @DisplayName("containsPattern wraps the escaped term in unescaped wildcards")
    void containsPatternWrapsEscapedTerm() {
        assertEquals("%abc%", SearchPatternUtil.containsPattern("abc"));
        assertEquals("%50!%off%", SearchPatternUtil.containsPattern("50%off"));
        assertEquals("%a!_b%", SearchPatternUtil.containsPattern("a_b"));
    }
}
