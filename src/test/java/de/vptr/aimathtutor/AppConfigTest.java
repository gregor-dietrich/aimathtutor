package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.Inline;

@DisplayName("AppConfig")
class AppConfigTest {

    @Test
    @DisplayName("stylesheet order is load-bearing: both bases, then styles.css")
    void stylesheetOrder() {
        assertEquals(List.of("/lumo/lumo.css", "/aura/aura.css", "/styles.css"),
                Arrays.stream(AppConfig.class.getAnnotationsByType(StyleSheet.class)).map(StyleSheet::value).toList());
    }

    @Test
    @DisplayName("the appearance script is inlined and exists on the classpath")
    void appearanceScriptIsInlined() throws Exception {
        final var inline = AppConfig.class.getAnnotation(Inline.class);
        assertNotNull(inline);
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(inline.value())) {
            assertNotNull(in);
            assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("window.appAppearance"));
        }
    }
}
