package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.vptr.aimathtutor.component.button.ThemeToggleButton;
import de.vptr.aimathtutor.service.ThemeService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Shared top bar layout: a width-full horizontal bar that always carries a
 * right-side group ending with a {@link ThemeToggleButton}.
 *
 * <p>Layouts customise the bar by:
 * <ul>
 *     <li>{@link #setLeftContent(Component)} — set/replace the leftmost content
 *         (e.g. navigation tabs or a title)</li>
 *     <li>{@link #addRightSideButtons(Component...)} — insert buttons immediately
 *         before the trailing theme toggle</li>
 *     <li>{@link #removeFromRightSide(Component...)} — detach previously added buttons</li>
 * </ul>
 */
public class TopBar extends HorizontalLayout {

    private final HorizontalLayout rightSide;

    /**
     * Build a width-full top bar with the theme toggle pre-attached on the right.
     */
    public TopBar(final ThemeService themeService) {
        this.setWidthFull();
        this.setJustifyContentMode(JustifyContentMode.END);
        this.setPadding(true);

        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);
        this.rightSide.add(new ThemeToggleButton(themeService));

        this.add(this.rightSide);
    }

    /**
     * The right-side container. Exposed so layouts can append additional widgets;
     * prefer {@link #addRightSideButtons(Component...)} for the standard pattern
     * of inserting before the theme toggle.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Right side is intentionally exposed for child layouts")
    public HorizontalLayout getRightSide() {
        return this.rightSide;
    }

    /**
     * Replace the left-side content with the given component. Pass {@code null}
     * to leave only the right-side group (justified to the end).
     */
    public void setLeftContent(final Component leftContent) {
        this.getChildren()
                .filter(c -> c != this.rightSide)
                .toList()
                .forEach(this::remove);

        if (leftContent != null) {
            this.addComponentAsFirst(leftContent);
            this.setJustifyContentMode(JustifyContentMode.BETWEEN);
        } else {
            this.setJustifyContentMode(JustifyContentMode.END);
        }
    }

    /**
     * Insert buttons on the right side, immediately before the trailing theme
     * toggle. The first button becomes the leftmost of the inserted group.
     * Null entries are ignored.
     */
    public void addRightSideButtons(final Component... buttons) {
        final int insertIndex = Math.max(0, this.rightSide.getComponentCount() - 1);
        for (int i = buttons.length - 1; i >= 0; i--) {
            if (buttons[i] != null) {
                this.rightSide.addComponentAtIndex(insertIndex, buttons[i]);
            }
        }
    }

    /**
     * Remove the given components from the right side. Null and detached
     * components are ignored.
     */
    public void removeFromRightSide(final Component... components) {
        for (final Component c : components) {
            if (c != null) {
                this.rightSide.remove(c);
            }
        }
    }
}
