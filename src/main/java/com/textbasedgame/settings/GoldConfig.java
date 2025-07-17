package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = GoldConfig.class)
public class GoldConfig {
    private final long baseGoldPerEnemyLevel;
    private final double enemyDefeatSurvivedAdjustGold;


    public GoldConfig(long baseGoldPerEnemyLevel, double enemyDefeatSurvivedAdjustGold) {
        this.baseGoldPerEnemyLevel = baseGoldPerEnemyLevel;
        this.enemyDefeatSurvivedAdjustGold = enemyDefeatSurvivedAdjustGold;
    }

    public long getBaseGoldPerEnemyLevel() {
        return baseGoldPerEnemyLevel;
    }

    public double getEnemyDefeatSurvivedAdjustGold() {
        return enemyDefeatSurvivedAdjustGold;
    }

    public static GoldConfig defaults() {
        return new GoldConfig(33L, -0.3);
    }

    @Override
    public String toString() {
        return "GoldConfig{" +
                "baseGoldPerEnemyLevel=" + baseGoldPerEnemyLevel +
                ", enemyDefeatSurvivedAdjustGold=" + enemyDefeatSurvivedAdjustGold +
                '}';
    }
}
