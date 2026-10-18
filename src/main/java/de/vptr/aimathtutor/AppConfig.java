package de.vptr.aimathtutor;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell configuration (theme, page title and push settings) for the Vaadin application.
 */
// The order of the @StyleSheet lines is load-bearing: later sheets override earlier ones, and the bootstrap
// listener in ThemeService removes whichever base sheet (Lumo or Aura) the user's design does not use, so exactly
// one of them loads per page. After styles.css come the fonts, the Lumo ink derivation, one sheet per design (each
// keyed on html[data-design]) and, last, component-tweaks.css, whose line alone can be dropped
// (AppConfigTest's list of expected sheets must drop it too).
@StyleSheet("/" + Lumo.STYLESHEET)
@StyleSheet("/" + Aura.STYLESHEET)
@StyleSheet("/styles.css")
@StyleSheet("/designs/fonts.css")
@StyleSheet("/designs/lumo-inks.css")
@StyleSheet("/designs/vellum.css")
@StyleSheet("/designs/powerup.css")
@StyleSheet("/designs/vibranium.css")
@StyleSheet("/designs/chalkboard.css")
@StyleSheet("/designs/component-tweaks.css")
@Inline("META-INF/resources/js/appearance.js")
@PageTitle("AI Math Tutor")
@Push
public class AppConfig implements AppShellConfigurator {
}
