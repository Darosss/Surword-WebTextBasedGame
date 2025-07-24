package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

import java.util.List;

@ExternalEntity(target = SystemConfig.class)
public class SystemConfig{
        private final long leaderboardRefreshCooldownMs;
        private final List<MercenaryLimit> mercenaryCharacterLimits;

    public SystemConfig() {
        SystemConfig df = SystemConfig.defaults();
        this.leaderboardRefreshCooldownMs = df.leaderboardRefreshCooldownMs;
        this.mercenaryCharacterLimits = df.mercenaryCharacterLimits;
    }

    public SystemConfig(long leaderboardRefreshCooldownMs, List<MercenaryLimit> mercenaryCharacterLimits) {
        this.leaderboardRefreshCooldownMs = leaderboardRefreshCooldownMs;
        this.mercenaryCharacterLimits = mercenaryCharacterLimits;
    }

    public record MercenaryLimit(int requiredLevel, int charactersLimit) { }

    public long getLeaderboardRefreshCooldownMs() {
        return leaderboardRefreshCooldownMs;
    }

    public List<MercenaryLimit> getMercenaryCharacterLimits() {
        return mercenaryCharacterLimits;
    }

    public static SystemConfig defaults() {
        return new SystemConfig(
                1_000 * 60 * 60 * 24,
                List.of(
                        new MercenaryLimit(10, 2),
                        new MercenaryLimit(30, 3),
                        new MercenaryLimit(50, 4)
                )
        );
    }

    @Override
    public String toString() {
        return "SystemConfig{" +
                "leaderboardRefreshCooldownMs=" + leaderboardRefreshCooldownMs +
                ", mercenaryCharacterLimits=" + mercenaryCharacterLimits +
                '}';
    }
}

