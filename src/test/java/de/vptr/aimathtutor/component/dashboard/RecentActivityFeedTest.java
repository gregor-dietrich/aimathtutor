package de.vptr.aimathtutor.component.dashboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;

import de.vptr.aimathtutor.dto.StudentSessionViewDto;

@DisplayName("RecentActivityFeed - escaping of user text in the innerHTML line")
class RecentActivityFeedTest {

    private static final String HOSTILE = "<img src=x onerror=alert(1)>\"&";
    private static final String SINK = "innerHTML";

    private static Stream<Element> descendants(final Element root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(RecentActivityFeedTest::descendants));
    }

    private static String sinkValue(final StudentSessionViewDto session) {
        final var feed = new RecentActivityFeed(List.of(session));
        final var sink = descendants(feed.getElement()).filter(e -> e.hasProperty(SINK)).findFirst();
        assertTrue(sink.isPresent(), "no element carries the innerHTML property");
        return sink.get().getProperty(SINK);
    }

    private static StudentSessionViewDto session(final String username, final String title) {
        final var dto = new StudentSessionViewDto();
        dto.username = username;
        dto.exerciseTitle = title;
        return dto;
    }

    @Test
    @DisplayName("Hostile username and exercise title are escaped")
    void testConstructor_escapesUserText() {
        final var html = sinkValue(session(HOSTILE, HOSTILE));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("\""));
        assertTrue(html.contains("&lt;img"));
    }

    @Test
    @DisplayName("Null username and exercise title render as Unknown")
    @SuppressWarnings("NullAway")
    void testConstructor_nullTextIsUnknown() {
        final var html = sinkValue(session(null, null));
        assertTrue(html.contains("<strong>Unknown</strong>"));
        assertTrue(html.contains(" — Unknown — "));
    }
}
