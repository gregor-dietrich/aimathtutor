package de.vptr.aimathtutor.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Bridge bean that forwards CDI {@link CommentCreatedEvent}s to programmatically
 * registered listeners. Allows non-CDI components (e.g. Vaadin layouts created
 * with {@code new}) to observe comment creation events without becoming beans.
 */
@ApplicationScoped
public class CommentCreatedEventBridge {

    private static final Logger LOG = LoggerFactory.getLogger(CommentCreatedEventBridge.class);

    private final Set<Consumer<CommentCreatedEvent>> listeners = new CopyOnWriteArraySet<>();

    /**
     * Registers a listener that will be invoked for every comment creation event.
     *
     * @param listener the consumer to add
     */
    public void addListener(final Consumer<CommentCreatedEvent> listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the consumer to remove
     */
    public void removeListener(final Consumer<CommentCreatedEvent> listener) {
        this.listeners.remove(listener);
    }

    void onCommentCreated(@Observes final CommentCreatedEvent event) {
        for (final Consumer<CommentCreatedEvent> listener : this.listeners) {
            try {
                listener.accept(event);
            } catch (final Throwable t) {
                LOG.error("Listener {} failed handling event {}", listener.getClass().getName(), event, t);
            }
        }
    }
}
