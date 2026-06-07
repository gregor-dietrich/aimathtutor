package de.vptr.aimathtutor.util;

/**
 * Helpers for building safe SQL {@code LIKE} patterns from user-supplied search terms. Escapes the {@code LIKE}
 * wildcard metacharacters ({@code %} and {@code _}) so they are matched literally rather than acting as wildcards.
 *
 * <p>
 * Uses {@code !} as the escape character. The queries that bind these patterns must declare {@code ESCAPE '!'} so the
 * escaping is applied (Hibernate does not enable a {@code LIKE} escape character by default). {@code !} is used instead
 * of the backslash to avoid escape-character ambiguity in JPQL/SQL string literals.
 * </p>
 */
public final class SearchPatternUtil {

    /** Escape character used in the generated patterns; queries must declare {@code ESCAPE '!'}. */
    public static final char ESCAPE_CHAR = '!';

    private SearchPatternUtil() {
        // Utility class
    }

    /**
     * Escapes the {@code LIKE} metacharacters ({@code !}, {@code %}, {@code _}) in the given term so they are treated
     * literally. The escape character itself is escaped first to avoid double-escaping the escapes added for {@code %}
     * and {@code _}.
     *
     * @param term
     *            the raw search term
     * @return the term with {@code LIKE} metacharacters escaped
     */
    public static String escapeLike(final String term) {
        return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Builds a "contains" pattern ({@code %term%}) with the term's {@code LIKE} metacharacters escaped. The surrounding
     * {@code %} wildcards are intentionally left unescaped.
     *
     * @param term
     *            the raw search term (already trimmed/lower-cased by the caller as needed)
     * @return a {@code LIKE} pattern matching rows that contain the literal term
     */
    public static String containsPattern(final String term) {
        return "%" + escapeLike(term) + "%";
    }
}
