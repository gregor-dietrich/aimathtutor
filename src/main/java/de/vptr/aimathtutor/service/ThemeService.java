package de.vptr.aimathtutor.service;

import org.jboss.logging.Logger;
import org.jsoup.nodes.Document;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

import de.vptr.aimathtutor.dto.Appearance;
import de.vptr.aimathtutor.dto.Appearance.Base;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.service.security.AuthService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Facade for the signed-in user's appearance (design and colour mode): reads it from the session, saves a change, and
 * renders it into the bootstrap page so the first paint already has the right base stylesheet and attributes.
 */
@ApplicationScoped
public class ThemeService {

    private static final Logger LOG = Logger.getLogger(ThemeService.class);

    private final AuthService authService;
    private final UserService userService;

    @Inject
    ThemeService(final AuthService authService, final UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Returns the appearance of the current session.
     *
     * @return the session's appearance, or {@link Appearance#DEFAULT} when nobody is signed in
     */
    public Appearance current() {
        final var appearance = this.authService.sessionAppearance();
        return appearance != null ? appearance : Appearance.DEFAULT;
    }

    /**
     * Saves a change of appearance for the signed-in user, then stores what the row now holds in the session. A null
     * argument leaves that field as stored. Runs off the UI thread. If the save fails, the exception propagates and the
     * session is untouched.
     *
     * @param session
     *            the session that makes the change, captured on the UI thread
     * @param credentials
     *            the session's credentials, captured on the UI thread
     * @param design
     *            the design to save, or null to keep the stored one
     * @param colorMode
     *            the colour mode to save, or null to keep the stored one
     */
    public void saveForSession(final VaadinSession session, final SessionCredentials credentials,
            @Nullable final Appearance.Design design, @Nullable final Appearance.ColorMode colorMode) {
        final var stored = this.userService.updateAppearance(credentials, design, colorMode);
        this.authService.rememberAppearance(session, credentials.userPublicId(), stored);
    }

    /**
     * Applies the session's colour mode to the page already shown, without a reload.
     *
     * @param ui
     *            the UI to update
     */
    public void applyColorMode(final UI ui) {
        ui.getPage().executeJs("window.appAppearance?.setColorMode($0)", this.current().colorMode().getValue());
    }

    void onServiceInit(@Observes final ServiceInitEvent event) {
        // The handler runs under the session lock, so the session attribute read in current() is safe.
        event.addIndexHtmlRequestListener(response -> render(response.getDocument(), this.current()));
    }

    /**
     * Writes the appearance into the bootstrap page: the design, base and colour mode attributes on {@code <html>}, and
     * only the stylesheet link of the active base.
     */
    static void render(final Document page, final Appearance appearance) {
        final var html = page.selectFirst("html");
        if (html == null) {
            LOG.warnf("Bootstrap page has no <html> element, appearance %s not rendered", appearance);
        } else {
            html.attr("data-design", appearance.design().getValue());
            html.attr("data-base", appearance.design().getBase().getValue());
            html.attr("data-color-mode", appearance.colorMode().getValue());
        }

        final var active = appearance.design().getBase();
        final var unused = active == Base.AURA ? Lumo.STYLESHEET : Aura.STYLESHEET;
        final var used = active == Base.AURA ? Aura.STYLESHEET : Lumo.STYLESHEET;
        page.select("link[href*=\"" + unused + "\"]").remove();
        if (page.select("link[href*=\"" + used + "\"]").isEmpty()) {
            LOG.warnf("Bootstrap page has no stylesheet link for the %s base", active.getValue());
        }
    }
}
