package com.textbasedgame.settings;

import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = MerchantConfig.class)
public class MerchantConfig {
    private final long commodityRefreshMS;
    private final double buyCostValueMultiplier;
    private final double sellCostValueMultiplier;

    public MerchantConfig() {
        MerchantConfig df = MerchantConfig.defaults();
        this.commodityRefreshMS = df.commodityRefreshMS;
        this.buyCostValueMultiplier = df.buyCostValueMultiplier;
        this.sellCostValueMultiplier = df.sellCostValueMultiplier;
    }

    public MerchantConfig(long commodityRefreshMS, double buyCostValueMultiplier, double sellCostValueMultiplier) {
        this.commodityRefreshMS = commodityRefreshMS;
        this.buyCostValueMultiplier = buyCostValueMultiplier;
        this.sellCostValueMultiplier = sellCostValueMultiplier;
    }

    public long getCommodityRefreshMS() {
        return commodityRefreshMS;
    }

    public double getBuyCostValueMultiplier() {
        return buyCostValueMultiplier;
    }

    public double getSellCostValueMultiplier() {
        return sellCostValueMultiplier;
    }

    public static MerchantConfig defaults() {
        return new MerchantConfig(
                1000 * 60 * 60 * 24,
                5.0, 1.0
                );
    }

    @Override
    public String toString() {
        return "MerchantConfig{" +
                "commodityRefreshMS=" + commodityRefreshMS +
                ", buyCostValueMultiplier=" + buyCostValueMultiplier +
                ", sellCostValueMultiplier=" + sellCostValueMultiplier +
                '}';
    }
}
