package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class ReplyButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Reply";

    public ReplyButton(final ComponentEventListener<ClickEvent<Button>> replyAction, final String tooltipText) {
        super("", replyAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        this.setIcon(LineAwesomeIcon.REPLY_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public ReplyButton(final ComponentEventListener<ClickEvent<Button>> replyAction) {
        this(replyAction, null);
    }
}
