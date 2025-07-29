package com.textbasedgame.settings;

import com.textbasedgame.items.ItemRarityEnum;
import dev.morphia.annotations.ExternalEntity;

import java.util.HashMap;
import java.util.Map;

@ExternalEntity(target = RaritiesBonuses.class)
public class RaritiesBonuses {
    private double baseFactor;
    private Map<ItemRarityEnum, Double> rarityBonuses;

public RaritiesBonuses() {
    RaritiesBonuses df = defaults();
    this.rarityBonuses = df.rarityBonuses;
    this.baseFactor = df.baseFactor;
}
    public RaritiesBonuses(double baseFactor, Map<ItemRarityEnum, Double> rarityBonuses) {
        this.baseFactor = baseFactor;
        this.rarityBonuses = rarityBonuses;
    }

    public static RaritiesBonuses defaults() {
        Map<ItemRarityEnum, Double> probs = new HashMap<>();
        probs.put(ItemRarityEnum.COMMON, 1.0);
        probs.put(ItemRarityEnum.UNCOMMON, 1.0);
        probs.put(ItemRarityEnum.RARE, 1.0);
        probs.put(ItemRarityEnum.VERY_RARE, 1.0);
        probs.put(ItemRarityEnum.EPIC, 1.0);
        probs.put(ItemRarityEnum.LEGENDARY, 1.0);
        probs.put(ItemRarityEnum.MYTHIC, 1.0);
        return new RaritiesBonuses(1.0, probs);
    }

    public double getBaseFactor() {
        return baseFactor;
    }

    public Map<ItemRarityEnum, Double> getRarityBonuses() {
        return rarityBonuses;
    }

    public void setBaseFactor(double baseFactor) {
        this.baseFactor = baseFactor;
    }

    public void setRarityBonuses(Map<ItemRarityEnum, Double> rarityBonuses) {
        this.rarityBonuses = rarityBonuses;
    }
}
