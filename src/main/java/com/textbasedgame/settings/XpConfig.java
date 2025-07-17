package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = XpConfig.class)
public class XpConfig {
    private final long basePerLevelXp;
    private final double scalingFactorXp;
    private final double factorBaseXp;
    private final double factorExponentXp;

    private final long enemyDefeatBaseXp;
    private final double enemyDefeatFactorBaseXp;
    private final double enemyDefeatFactorExponentXp;

    private final double enemyDefeatSurvivedAdjustXp;

    public XpConfig(long basePerLevelXp,
                    double scalingFactorXp,
                    double factorBaseXp,
                    double factorExponentXp,
                    long enemyDefeatBaseXp,
                    double enemyDefeatFactorBaseXp,
                    double enemyDefeatFactorExponentXp,
                    double enemyDefeatSurvivedAdjustXp) {
        this.basePerLevelXp = basePerLevelXp;
        this.scalingFactorXp = scalingFactorXp;
        this.factorBaseXp = factorBaseXp;
        this.factorExponentXp = factorExponentXp;
        this.enemyDefeatBaseXp = enemyDefeatBaseXp;
        this.enemyDefeatFactorBaseXp = enemyDefeatFactorBaseXp;
        this.enemyDefeatFactorExponentXp = enemyDefeatFactorExponentXp;
        this.enemyDefeatSurvivedAdjustXp = enemyDefeatSurvivedAdjustXp;
    }


    public long getBasePerLevelXp() {
        return basePerLevelXp;
    }

    public double getScalingFactorXp() {
        return scalingFactorXp;
    }

    public double getFactorBaseXp() {
        return factorBaseXp;
    }

    public double getFactorExponentXp() {
        return factorExponentXp;
    }

    public long getEnemyDefeatBaseXp() {
        return enemyDefeatBaseXp;
    }

    public double getEnemyDefeatFactorBaseXp() {
        return enemyDefeatFactorBaseXp;
    }

    public double getEnemyDefeatFactorExponentXp() {
        return enemyDefeatFactorExponentXp;
    }

    public double getEnemyDefeatSurvivedAdjustXp() {
        return enemyDefeatSurvivedAdjustXp;
    }

    public static XpConfig defaults() {
        return new XpConfig(100L, 30.0, 0.9,
                1.1, 50, 1.2, 0.3,
                -0.3 );
    }

    @Override
    public String toString() {
        return "XpConfig{" +
                "basePerLevelXp=" + basePerLevelXp +
                ", scalingFactorXp=" + scalingFactorXp +
                ", factorBaseXp=" + factorBaseXp +
                ", factorExponentXp=" + factorExponentXp +
                ", enemyDefeatBaseXp=" + enemyDefeatBaseXp +
                ", enemyDefeatFactorBaseXp=" + enemyDefeatFactorBaseXp +
                ", enemyDefeatFactorExponentXp=" + enemyDefeatFactorExponentXp +
                ", enemyDefeatSurvivedAdjustXp=" + enemyDefeatSurvivedAdjustXp +
                '}';
    }
}
