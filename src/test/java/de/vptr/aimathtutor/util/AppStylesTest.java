package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.dependency.StyleSheet;

import de.vptr.aimathtutor.AppConfig;

@DisplayName("AppStyles - names defined in the stylesheets")
class AppStylesTest {

    private static final Pattern TOKEN = Pattern.compile("var\\((--app-[\\w-]+)\\)");

    private static final Pattern COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");

    /** The stylesheet without its comments, so a name that survives only in a comment does not count as defined. */
    private static String stylesheet(final String path) throws IOException {
        try (var in = AppStylesTest.class.getResourceAsStream("/META-INF/resources" + path)) {
            assertNotNull(in, path + " must be on the classpath");
            return COMMENT.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8)).replaceAll("");
        }
    }

    /** styles.css plus every design stylesheet AppConfig declares: a class may be defined by either. */
    private static String classStylesheets() throws IOException {
        final var paths = Stream
                .concat(Stream.of("/styles.css"), Arrays.stream(AppConfig.class.getAnnotationsByType(StyleSheet.class))
                        .map(StyleSheet::value).filter(path -> path.startsWith("/designs/") && path.endsWith(".css")))
                .toList();
        final var css = new StringBuilder();
        for (final var path : paths) {
            css.append(stylesheet(path)).append('\n');
        }
        return css.toString();
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
    @DisplayName("every AppStyles constant is defined in the stylesheets")
    void testConstants_definedInStylesheet() throws IOException {
        final var css = stylesheet("/styles.css");
        final var classCss = classStylesheets();
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
                assertTrue(Pattern.compile("\\." + Pattern.quote(constant[1]) + "(?![\\w-])").matcher(classCss).find(),
                        "no stylesheet has a ." + constant[1] + " selector (AppStyles." + constant[0] + ")");
            }
        }
        assertTrue(tokens > 0, "AppStyles has no token constants");
        assertTrue(classes > 0, "AppStyles has no class name constants");
    }
}
