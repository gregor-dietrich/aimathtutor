package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppStyles - names defined in styles.css")
class AppStylesTest {

    private static final Pattern TOKEN = Pattern.compile("var\\((--app-[\\w-]+)\\)");

    private static final Pattern COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");

    /** The stylesheet without its comments, so a name that survives only in a comment does not count as defined. */
    private static String stylesheet() throws IOException {
        try (var in = AppStylesTest.class.getResourceAsStream("/META-INF/resources/styles.css")) {
            assertNotNull(in, "styles.css must be on the classpath");
            return COMMENT.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8)).replaceAll("");
        }
    }

    private static List<String[]> constants() {
        return Arrays.stream(AppStyles.class.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers()) && f.getType() == String.class)
                .map(f -> {
                    try {
                        return new String[] { f.getName(), (String) f.get(null) };
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }).toList();
    }

    @Test
    @DisplayName("every AppStyles constant is defined in styles.css")
    void testConstants_definedInStylesheet() throws IOException {
        final var css = stylesheet();
        var tokens = 0;
        var classes = 0;
        for (final var constant : constants()) {
            final var matcher = TOKEN.matcher(constant[1]);
            if (matcher.matches()) {
                tokens++;
                assertTrue(css.contains(matcher.group(1) + ":"),
                        "styles.css does not declare " + matcher.group(1) + " (AppStyles." + constant[0] + ")");
            } else {
                classes++;
                assertTrue(Pattern.compile("\\." + Pattern.quote(constant[1]) + "(?![\\w-])").matcher(css).find(),
                        "styles.css has no ." + constant[1] + " selector (AppStyles." + constant[0] + ")");
            }
        }
        assertTrue(tokens > 0, "AppStyles has no token constants");
        assertTrue(classes > 0, "AppStyles has no class name constants");
    }
}
