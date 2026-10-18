package de.vptr.aimathtutor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/** Captures a component's log records for the duration of a test action. */
public final class TestLogCapture {

    private TestLogCapture() {
    }

    /** Returns records emitted by the named component while the action runs. */
    public static List<LogRecord> capture(final Class<?> source, final Runnable action) {
        final var records = new ArrayList<LogRecord>();
        final var handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
                // nothing buffered
            }

            @Override
            public void close() {
                // nothing to release
            }
        };
        final var logger = Objects.requireNonNull(LogManager.getLogManager().getLogger(source.getName()),
                () -> "No logger " + source.getName()
                        + "; tests need the JBoss log manager, which creates loggers on demand (set by surefire)");
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return records;
    }
}
