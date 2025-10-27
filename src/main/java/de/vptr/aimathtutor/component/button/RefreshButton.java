package de.vptr.aimathtutor.component.button;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class RefreshButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Refresh";

    public RefreshButton(final ComponentEventListener<ClickEvent<Button>> refreshAction, final String tooltipText,
            final ButtonVariant... variants) {
        super("", refreshAction);
        this.setIcon(new Icon(VaadinIcon.REFRESH));
        this.setTooltipText(tooltipText);
        this.addThemeVariants(variants);
    }

    public RefreshButton(final ComponentEventListener<ClickEvent<Button>> refreshAction,
            final ButtonVariant... variants) {
        this(refreshAction, DEFAULT_TOOLTIP, variants);
    }

    public RefreshButton(final ComponentEventListener<ClickEvent<Button>> refreshAction) {
        this(refreshAction, DEFAULT_TOOLTIP, ButtonVariant.LUMO_PRIMARY);
    }
}
