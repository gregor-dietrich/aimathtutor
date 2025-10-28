package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button that opens the comment editor for a given entity. Displays a comment
 * icon and forwards clicks to the provided listener.
 */
public class CommentButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Add Comment";

    /**
     * Constructs a CommentButton with the specified action and tooltip.
     */
    public CommentButton(final ComponentEventListener<ClickEvent<Button>> addUserAction, final String tooltipText) {
        super("", addUserAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_WARNING);
        this.setIcon(LineAwesomeIcon.COMMENT_ALT_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public CommentButton(final ComponentEventListener<ClickEvent<Button>> addUserAction) {
        this(addUserAction, null);
    }
}
