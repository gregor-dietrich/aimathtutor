package de.vptr.aimathtutor.component.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.aimathtutor.component.GlassAccentBar;
import de.vptr.aimathtutor.util.AppStyles;

/**
 * Premium chart card with glass-morphism styling, matching the DashboardKpiCard design language. Wraps SVG chart
 * content with a title header.
 */
public class ChartCard extends VerticalLayout {

    private final Div chartContainer;

    /**
     * Creates a chart card with the given title.
     *
     * @param title
     *            the chart title (may be null or empty to omit)
     */
    public ChartCard(final String title) {
        this.setPadding(false);
        this.setSpacing(false);
        this.setWidthFull();
        this.setHeightFull();

        this.addClassNames(AppStyles.GLASS_CARD, AppStyles.LIFT_ON_HOVER);
        this.getStyle().set("overflow", "hidden");

        final var accentBar = new GlassAccentBar();

        if (title != null && !title.isEmpty()) {
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("font-size", "13px").set("font-weight", "700").set("color", AppStyles.ACCENT_TEXT)
                    .set("letter-spacing", "0.3px").set("padding", "12px 16px 0").set("display", "block");
            this.add(accentBar, titleSpan);
        } else {
            this.add(accentBar);
        }

        this.chartContainer = new Div();
        this.chartContainer.setWidthFull();
        this.chartContainer.setHeightFull();
        this.chartContainer.getStyle().set("min-height", "200px").set("padding", "8px 8px 4px");
        this.add(this.chartContainer);
    }

    /**
     * Replaces the chart content with the given SVG string.
     *
     * @param svgContent
     *            raw SVG markup or null/empty to show "No data available"
     */
    public void setChartContent(final String svgContent) {
        this.chartContainer.removeAll();
        if (svgContent == null || svgContent.isEmpty()) {
            this.chartContainer.add(new Span("No data available"));
            return;
        }
        this.chartContainer.getElement().setProperty("innerHTML", svgContent);
    }
}
