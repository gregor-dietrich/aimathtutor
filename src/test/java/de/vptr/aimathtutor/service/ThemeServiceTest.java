package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.Appearance.ColorMode;
import de.vptr.aimathtutor.dto.Appearance.Design;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.util.TestLogCapture;

@DisplayName("ThemeService")
@SuppressWarnings("NullAway")
class ThemeServiceTest {

    private static final String PAGE = """
            <html lang="de"><head>
            <link href="/lumo/lumo.css?v-c=1" rel="stylesheet">
            <link href="/aura/aura.css?v-c=2" rel="stylesheet">
            <link href="/styles.css?v-c=3" rel="stylesheet">
            </head><body></body></html>""";

    private final AuthService authService = mock(AuthService.class);
    private final UserService userService = mock(UserService.class);
    private ThemeService themeService;

    @BeforeEach
    void setUp() {
        this.themeService = new ThemeService(this.authService, this.userService);
    }

    @Test
    @DisplayName("current() returns the session appearance")
    void currentReturnsSessionAppearance() {
        final var stored = new Appearance(Design.CHALKBOARD, ColorMode.DARK);
        when(this.authService.sessionAppearance()).thenReturn(stored);

        assertSame(stored, this.themeService.current());
    }

    @Test
    @DisplayName("current() falls back to the default without a session appearance")
    void currentFallsBackToDefault() {
        when(this.authService.sessionAppearance()).thenReturn(null);

        assertEquals(Appearance.DEFAULT, this.themeService.current());
    }

    @Test
    @DisplayName("applyColorMode passes the mode value to executeJs")
    void applyColorModePassesModeValue() {
        when(this.authService.sessionAppearance()).thenReturn(new Appearance(Design.VELLUM, ColorMode.DARK));
        final var ui = mock(UI.class);
        final var page = mock(Page.class);
        when(ui.getPage()).thenReturn(page);

        this.themeService.applyColorMode(ui);

        verify(page).executeJs("window.appAppearance?.setColorMode($0)", "dark");
    }

    @Test
    @DisplayName("saveForSession remembers the appearance the update returned, not the arguments")
    void saveForSessionRemembersReturnedAppearance() {
        final var session = mock(VaadinSession.class);
        final var credentials = new SessionCredentials("user-1", "stamp", "throttle");
        final var stored = new Appearance(Design.CHALKBOARD, ColorMode.DARK);
        when(this.userService.updateAppearance(credentials, null, ColorMode.DARK)).thenReturn(stored);

        this.themeService.saveForSession(session, credentials, null, ColorMode.DARK);

        final var order = inOrder(this.userService, this.authService);
        order.verify(this.userService).updateAppearance(credentials, null, ColorMode.DARK);
        order.verify(this.authService).rememberAppearance(session, "user-1", stored);
    }

    @Test
    @DisplayName("saveForSession leaves the session untouched when the update throws")
    void saveForSessionLeavesSessionOnFailure() {
        final var session = mock(VaadinSession.class);
        final var credentials = new SessionCredentials("user-1", "stamp", "throttle");
        when(this.userService.updateAppearance(credentials, Design.VELLUM, ColorMode.LIGHT))
                .thenThrow(new IllegalStateException("gone"));

        assertThrows(IllegalStateException.class,
                () -> this.themeService.saveForSession(session, credentials, Design.VELLUM, ColorMode.LIGHT));

        verify(this.authService, never()).rememberAppearance(any(), any(), any());
    }

    @Test
    @DisplayName("the registered bootstrap listener renders the session appearance into the page")
    void bootstrapListenerRendersSessionAppearance() {
        when(this.authService.sessionAppearance()).thenReturn(new Appearance(Design.VIBRANIUM, ColorMode.LIGHT));
        final var event = mock(ServiceInitEvent.class);
        this.themeService.onServiceInit(event);
        final var captor = ArgumentCaptor.forClass(IndexHtmlRequestListener.class);
        verify(event).addIndexHtmlRequestListener(captor.capture());
        final var doc = Jsoup.parse(PAGE);
        final var response = mock(IndexHtmlResponse.class);
        when(response.getDocument()).thenReturn(doc);

        captor.getValue().modifyIndexHtmlResponse(response);

        assertEquals("vibranium", doc.selectFirst("html").attr("data-design"));
        assertEquals(List.of("/lumo/lumo.css?v-c=1", "/styles.css?v-c=3"), hrefs(doc));
    }

    @Test
    @DisplayName("render for a Lumo design keeps only the Lumo base link")
    void renderLumoDesign() {
        final var doc = Jsoup.parse(PAGE);

        ThemeService.render(doc, new Appearance(Design.POWER_UP, ColorMode.DARK));

        final var html = doc.selectFirst("html");
        assertEquals("powerup", html.attr("data-design"));
        assertEquals("lumo", html.attr("data-base"));
        assertEquals("dark", html.attr("data-color-mode"));
        assertEquals(List.of("/lumo/lumo.css?v-c=1", "/styles.css?v-c=3"), hrefs(doc));
        assertEquals("", html.attr("theme"));
    }

    @Test
    @DisplayName("render for Vellum keeps only the Aura base link")
    void renderVellum() {
        final var doc = Jsoup.parse(PAGE);

        ThemeService.render(doc, Appearance.DEFAULT);

        final var html = doc.selectFirst("html");
        assertEquals("vellum", html.attr("data-design"));
        assertEquals("aura", html.attr("data-base"));
        assertEquals("system", html.attr("data-color-mode"));
        assertEquals("de", html.attr("lang"));
        assertEquals(List.of("/aura/aura.css?v-c=2", "/styles.css?v-c=3"), hrefs(doc));
    }

    @Test
    @DisplayName("render does not throw when the active base link is missing")
    void renderMissingActiveLink() {
        final var doc = Jsoup.parse("<html><head><link href=\"/lumo/lumo.css\" rel=\"stylesheet\"></head></html>");

        final var records = TestLogCapture.capture(ThemeService.class,
                () -> assertDoesNotThrow(() -> ThemeService.render(doc, Appearance.DEFAULT)));

        assertTrue(hrefs(doc).isEmpty());
        assertEquals("aura", doc.selectFirst("html").attr("data-base"));
        assertEquals(1, records.stream().filter(r -> r.getLevel().equals(Level.WARNING)).count());
    }

    @Test
    @DisplayName("render does not throw when there is no html element")
    void renderMissingHtml() {
        final var doc = Jsoup.parse("<html></html>");
        doc.selectFirst("html").remove();

        assertDoesNotThrow(() -> ThemeService.render(doc, Appearance.DEFAULT));
        assertNull(doc.selectFirst("html"));
    }

    @Test
    @DisplayName("persisted enum names are pinned: renaming one needs a Flyway migration of users.design/color_mode")
    void persistedEnumNamesArePinned() {
        assertEquals(List.of("VELLUM", "POWER_UP", "VIBRANIUM", "CHALKBOARD"),
                Arrays.stream(Design.values()).map(Enum::name).toList());
        assertEquals(List.of("LIGHT", "DARK", "SYSTEM"), Arrays.stream(ColorMode.values()).map(Enum::name).toList());
    }

    private static List<String> hrefs(final Document doc) {
        return doc.select("link").eachAttr("href");
    }
}
