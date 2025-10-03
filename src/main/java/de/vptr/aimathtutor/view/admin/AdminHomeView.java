package de.vptr.aimathtutor.view.admin;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.service.AuthService;
import jakarta.inject.Inject;

@Route(value = "admin", layout = AdminMainLayout.class)
public class AdminHomeView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    AuthService authService;

    public AdminHomeView() {
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.buildUI();
    }

    private void buildUI() {
        this.removeAll();
        final var welcomeLabel = new H2("Welcome, " + this.authService.getUsername() + "!");
        this.add(welcomeLabel);
    }
}