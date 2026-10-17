package de.vptr.aimathtutor.component.dashboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.util.AppStyles;

@DisplayName("ChartUtil - theme-neutral SVG output")
class ChartUtilTest {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 1);
    // 30 chars: after truncation to 21 (bar label) or 13 (donut legend) chars the "<img" part still survives
    private static final String HOSTILE = "<img src=x onerror=alert(1)>\"&";

    private static Map<LocalDate, Long> series() {
        return Map.of(DAY, 1L, DAY.plusDays(1), 3L);
    }

    private static Map<String, Integer> categories() {
        final var data = new LinkedHashMap<String, Integer>();
        data.put("a", 4);
        data.put("b", 3);
        data.put("c", 2);
        data.put("d", 1);
        return data;
    }

    @Test
    @DisplayName("Line chart uses the accent token and no font-family")
    void testLineChart_withData() {
        final var svg = ChartUtil.lineChart(series(), "Title", 400, 200);
        assertTrue(svg.contains(AppStyles.ACCENT));
        assertFalse(svg.contains("font-family"));
    }

    @Test
    @DisplayName("Horizontal bar chart uses the accent token and no font-family")
    void testHorizontalBarChart_withData() {
        final var svg = ChartUtil.horizontalBarChart(categories(), "Title", 400, 200);
        assertTrue(svg.contains(AppStyles.ACCENT));
        assertFalse(svg.contains("font-family"));
    }

    @Test
    @DisplayName("Donut chart uses the accent token and no font-family")
    void testDonutChart_withData() {
        final var svg = ChartUtil.donutChart(categories(), "Label", "10", 200);
        assertTrue(svg.contains(AppStyles.ACCENT));
        assertFalse(svg.contains("font-family"));
    }

    @Test
    @DisplayName("Donut chart with four categories uses the status tokens")
    void testDonutChart_statusColors() {
        final var svg = ChartUtil.donutChart(categories(), null, null, 200);
        assertTrue(svg.contains(AppStyles.ERROR));
        assertTrue(svg.contains(AppStyles.SUCCESS));
        assertTrue(svg.contains(AppStyles.WARNING));
    }

    @Test
    @DisplayName("Empty charts carry no font-family")
    void testCharts_emptyData() {
        assertFalse(ChartUtil.lineChart(Map.of(), null, 400, 200).contains("font-family"));
        assertFalse(ChartUtil.horizontalBarChart(Map.of(), null, 400, 200).contains("font-family"));
        assertFalse(ChartUtil.donutChart(Map.of(), null, null, 200).contains("font-family"));
    }

    @Test
    @DisplayName("Line chart escapes a hostile title")
    void testLineChart_escapesTitle() {
        final var svg = ChartUtil.lineChart(series(), HOSTILE, 400, 200);
        assertFalse(svg.contains("<img"));
        assertTrue(svg.contains("&lt;img"));
    }

    @Test
    @DisplayName("Horizontal bar chart escapes a hostile title and bar label")
    void testHorizontalBarChart_escapesTitleAndLabel() {
        final var titled = ChartUtil.horizontalBarChart(categories(), HOSTILE, 400, 200);
        assertFalse(titled.contains("<img"));
        assertTrue(titled.contains("&lt;img"));

        final var labelled = ChartUtil.horizontalBarChart(Map.of(HOSTILE, 5), "Title", 400, 200);
        assertFalse(labelled.contains("<img"));
        assertTrue(labelled.contains("&lt;img"));
    }

    @Test
    @DisplayName("Donut chart escapes a hostile legend key and center texts")
    void testDonutChart_escapesLegendKeyAndCenter() {
        final var legend = ChartUtil.donutChart(Map.of(HOSTILE, 5), null, null, 200);
        assertFalse(legend.contains("<img"));
        assertTrue(legend.contains("&lt;img"));

        final var center = ChartUtil.donutChart(categories(), HOSTILE, HOSTILE, 200);
        assertFalse(center.contains("<img"));
        assertTrue(center.contains("&lt;img"));
    }

    @Test
    @DisplayName("Donut chart with a single category draws the full-circle hole in the background colour")
    void testDonutChart_fullCircle() {
        final var svg = ChartUtil.donutChart(Map.of("only", 7), null, null, 200);
        assertTrue(svg.contains("fill=\"var(--vaadin-background-color)\""));
    }
}
