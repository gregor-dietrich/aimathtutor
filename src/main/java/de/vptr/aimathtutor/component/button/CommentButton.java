package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import jakarta.annotation.Nullable;

/**
 * Button that shows the comments of a given entity. Displays a comment icon and forwards clicks to the provided
 * listener.
 */
public class CommentButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Show Comments";

    /**
     * Constructs a CommentButton with the specified action and tooltip.
     */
    public CommentButton(final ComponentEventListener<ClickEvent<Button>> clickListener,
            @Nullable final String tooltipText) {
        super("", clickListener);
        this.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.WARNING);
        this.setIcon(LineAwesomeIcon.COMMENT_ALT_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
        this.getElement().setAttribute("aria-label", tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public CommentButton(final ComponentEventListener<ClickEvent<Button>> clickListener) {
        this(clickListener, null);
    }
}
