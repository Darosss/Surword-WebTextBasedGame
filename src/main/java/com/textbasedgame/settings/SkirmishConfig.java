package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = SkirmishConfig.class)
public class SkirmishConfig {
    private final long challengeCooldownMS;
    private final long dungeonCooldownMS;

    public SkirmishConfig(long challengeCooldownMS, long dungeonCooldownMS) {
        this.challengeCooldownMS = challengeCooldownMS;
        this.dungeonCooldownMS = dungeonCooldownMS;
    }

    public long getChallengeCooldownMS() {
        return challengeCooldownMS;
    }

    public long getDungeonCooldownMS() {
        return dungeonCooldownMS;
    }

    public static SkirmishConfig defaults() {
        return new SkirmishConfig(120000, 600000);
    }

    @Override
    public String toString() {
        return "SkirmishConfig{" +
                "challengeCooldownMS=" + challengeCooldownMS +
                ", dungeonCooldownMS=" + dungeonCooldownMS +
                '}';
    }
}
