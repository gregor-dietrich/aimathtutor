package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiConfigCache")
class AiConfigCacheTest {

    private final AiConfigCache cache = new AiConfigCache();

    @Test
    @DisplayName("A fill that began before an eviction is discarded")
    void fillOverlappingEvictionIsDiscarded() {
        final var seen = this.cache.generation();
        this.cache.evict(List.of("a"));
        this.cache.putIfCurrent("a", "stale", seen);
        assertNull(this.cache.get("a"));
    }

    @Test
    @DisplayName("A fill that began before a clear is discarded")
    void fillOverlappingClearIsDiscarded() {
        final var seen = this.cache.generation();
        this.cache.clear();
        this.cache.putIfCurrent("a", "stale", seen);
        assertNull(this.cache.get("a"));
    }

    @Test
    @DisplayName("A fill with the current generation caches")
    void currentFillCaches() {
        this.cache.putIfCurrent("a", "v", this.cache.generation());
        assertEquals("v", this.cache.get("a"));
    }

    @Test
    @DisplayName("A null value removes an existing entry")
    void nullValueRemoves() {
        this.cache.putIfCurrent("a", "v", this.cache.generation());
        this.cache.putIfCurrent("a", null, this.cache.generation());
        assertNull(this.cache.get("a"));
    }

    @Test
    @DisplayName("Evicting one key leaves the others cached")
    void evictKeepsOtherKeys() {
        this.cache.putIfCurrent("a", "1", this.cache.generation());
        this.cache.putIfCurrent("b", "2", this.cache.generation());
        this.cache.evict(List.of("a"));
        assertNull(this.cache.get("a"));
        assertEquals("2", this.cache.get("b"));
    }
}
