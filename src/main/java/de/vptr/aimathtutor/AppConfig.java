package de.vptr.aimathtutor;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell configuration (theme, page title and push settings) for the Vaadin application.
 */
@StyleSheet("/" + Lumo.STYLESHEET)
@StyleSheet("/styles.css")
@PageTitle("AI Math Tutor")
@Push
// Pin react-router above the Vaadin platform default (currently 7.13.1, bundled in
// flow-server) to pick up security fixes. The frontend build writes that default on
// every prepare/build-frontend; this application-level @NpmPackage is re-applied
// afterwards, so the pinned version survives the build instead of being reverted.
// The "$react-router" entry in package.json overrides then cascades it to transitives.
@NpmPackage(value = "react-router", version = "7.15.0")
public class AppConfig implements AppShellConfigurator {
}
