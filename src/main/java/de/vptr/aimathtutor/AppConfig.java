package de.vptr.aimathtutor;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell configuration (theme, page title and push settings) for
 * the Vaadin application.
 */
@StyleSheet("/" + Lumo.STYLESHEET)
@StyleSheet("/styles.css")
@PageTitle("AI Math Tutor")
@Push
public class AppConfig implements AppShellConfigurator {
}
