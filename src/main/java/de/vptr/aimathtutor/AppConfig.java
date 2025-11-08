package de.vptr.aimathtutor;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.Theme;

/**
 * Application shell configuration (theme, page title and push settings) for
 * the Vaadin application.
 */
@Theme("starter-theme")
@PageTitle("AI Math Tutor")
@Push
public class AppConfig implements AppShellConfigurator {
}
