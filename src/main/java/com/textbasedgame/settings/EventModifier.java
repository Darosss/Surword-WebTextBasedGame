package com.textbasedgame.settings;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Map;

@Entity("event_modifier")
public class EventModifier {

    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    private ObjectId id;

    private Map<ModifierType, Double> modifiers;
    private Instant start;
    private Instant end;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Map<ModifierType, Double> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Map<ModifierType, Double> modifiers) {
        this.modifiers = modifiers;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "EventModifier{" +
                "id=" + id +
                ", modifiers=" + modifiers +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
