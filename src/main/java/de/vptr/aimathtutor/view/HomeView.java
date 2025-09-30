package de.vptr.aimathtutor.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.rest.service.AuthService;
import de.vptr.aimathtutor.rest.service.GreetService;
import jakarta.inject.Inject;

@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    AuthService authService;

    @Inject
    GreetService greetService;

    public HomeView() {
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.START);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.buildUI();
    }

    private void buildUI() {
        this.removeAll();

        // Welcome header
        final var welcomeLabel = new H2("Welcome, " + this.authService.getUsername() + "!");
        welcomeLabel.getStyle().set("margin-top", "var(--lumo-space-l)");

        // Main content
        final var content = new VerticalLayout();
        content.setAlignItems(Alignment.CENTER);
        content.addClassName("centered-content");

        final var textField = new TextField("Your name");
        textField.addThemeName("bordered");
        textField.setWidth("300px");

        final var button = new Button("Say hello 👋", e -> {
            content.add(new Paragraph(this.greetService.greet(textField.getValue())));
        });

        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickShortcut(Key.ENTER);
        button.setWidth("300px");

        content.add(textField, button);

        this.add(welcomeLabel, content);
    }
}