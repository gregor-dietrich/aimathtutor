package de.vptr.aimathtutor.event;

import java.util.List;

/**
 * CDI event fired by {@code AiConfigService} inside the transaction of every AI-config write, before its first write.
 * {@code AiConfigService} observes it after the transaction completes, whether it committed or rolled back, and evicts
 * the cached values of the keys, so a fill by a reader outside the transaction cannot keep the old value. Firing before
 * the first write is defensive.
 *
 * @param configKeys
 *            the keys the write changes, copied on construction
 */
public record AiConfigChangedEvent(List<String> configKeys) {

    public AiConfigChangedEvent {
        configKeys = List.copyOf(configKeys);
    }
}
