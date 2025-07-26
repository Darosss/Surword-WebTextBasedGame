package com.textbasedgame.settings;

import dev.morphia.Datastore;
import dev.morphia.query.filters.Filters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class EventModifierCache {

    private final Datastore ds;
    private volatile List<EventModifier> active = List.of();

    public EventModifierCache(Datastore ds) {
        this.ds = ds;
        this.refreshActiveModifiers();
    }

    public void refreshActiveModifiers() {
        Instant now = Instant.now();
        List<EventModifier> live = ds.find(EventModifier.class)
                .filter(
                        Filters.and(
                                Filters.lte("start", now),
                                Filters.gte("end", now)
                        )
                )
                .iterator().toList();
        active = List.copyOf(live);
    }

    public Double multiplier(ModifierType type) {
        return active.stream()
                .map(ev -> ev.getModifiers().getOrDefault(type, 1.0))
                .reduce(1.0, (a, b) -> a * b);
    }
}

