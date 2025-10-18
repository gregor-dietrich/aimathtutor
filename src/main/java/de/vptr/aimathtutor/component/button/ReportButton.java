package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import de.vptr.aimathtutor.component.dialog.ConfirmationDialog;

public class ReportButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Report as inappropriate";

    private final ComponentEventListener<ClickEvent<Button>> reportAction;

    public ReportButton(final ComponentEventListener<ClickEvent<Button>> reportAction, final String tooltipText) {
        super("");
        this.reportAction = reportAction;
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        this.setIcon(LineAwesomeIcon.FLAG_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
        this.addClickListener(this::showConfirmationDialog);
    }

    public ReportButton(final ComponentEventListener<ClickEvent<Button>> reportAction) {
        this(reportAction, null);
    }

    private void showConfirmationDialog(final ClickEvent<Button> event) {
        final var confirmDialog = new ConfirmationDialog(e -> {
            if (this.reportAction != null) {
                this.reportAction.onComponentEvent(event);
            }
        }, "Report Content", "Report this content for moderation review?", "Report");
        confirmDialog.open();
    }
}
