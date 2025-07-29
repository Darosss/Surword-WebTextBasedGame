package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = LootConfig.class)
public class LootConfig {

    private final RaritiesBonuses raritiesBonusMultipliers;
    private final double itemChanceBonusMultiplier;

    LootConfig() {
        LootConfig df = LootConfig.defaults();
        this.raritiesBonusMultipliers= df.raritiesBonusMultipliers;
        this.itemChanceBonusMultiplier = df.itemChanceBonusMultiplier;
    }

    LootConfig(RaritiesBonuses raritiesBonuses, double itemChanceBonusMultiplier) {
        this.raritiesBonusMultipliers= raritiesBonuses;
        this.itemChanceBonusMultiplier = itemChanceBonusMultiplier;
    }


    public double getItemChanceBonusMultiplier() {
        return itemChanceBonusMultiplier;
    }

    public RaritiesBonuses getRaritiesBonusMultipliers() {
        return raritiesBonusMultipliers;
    }


    public static LootConfig defaults() {
        return new LootConfig(RaritiesBonuses.defaults(),1);
    }

    @Override
    public String toString() {
        return "LootConfig{" +
                "raritiesBonusMultiplier=" + raritiesBonusMultipliers +
                ", itemChanceBonusMultiplier=" + itemChanceBonusMultiplier +
                '}';
    }
}
