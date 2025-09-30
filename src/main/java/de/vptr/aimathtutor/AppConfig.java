package de.vptr.aimathtutor;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.Theme;

@Theme("starter-theme")
@PageTitle("Midas")
@Push
public class AppConfig implements AppShellConfigurator {
}
