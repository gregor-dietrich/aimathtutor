package de.vptr.aimathtutor.service.ai;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;

/**
 * The value cache of {@link AiConfigService}. A fill reads a row and caches it in two separate steps, so a write can
 * commit in between and the fill would cache the value that write replaced. The rule that prevents it: a fill passes
 * the {@linkplain #generation() generation} it read before loading the row to {@link #putIfCurrent}, and every eviction
 * advances it, so a fill that overlapped an eviction is discarded. The generation is global and never reset: one write
 * cancels every in-flight fill, which then only costs a re-read.
 */
final class AiConfigCache {

    private final Map<String, String> values = new ConcurrentHashMap<>();
    private long generation;

    /** Returns the cached value of the key, or null. Lock-free. */
    @Nullable
    String get(final String key) {
        return this.values.get(key);
    }

    /** Returns the generation a fill must read before it loads the row. */
    synchronized long generation() {
        return this.generation;
    }

    /**
     * Caches the value, or removes the key for a null value, unless an eviction happened since the fill took its
     * generation.
     *
     * @param seenGeneration
     *            the generation read before the row was loaded
     */
    synchronized void putIfCurrent(final String key, @Nullable final String value, final long seenGeneration) {
        if (this.generation != seenGeneration) {
            return;
        }
        if (value == null) {
            this.values.remove(key);
        } else {
            this.values.put(key, value);
        }
    }

    /** Advances the generation, then removes the keys. */
    synchronized void evict(final Collection<String> keys) {
        this.generation++;
        this.values.keySet().removeAll(keys);
    }

    /** Advances the generation, then removes every key. */
    synchronized void clear() {
        this.generation++;
        this.values.clear();
    }
}
