package com.textbasedgame.settings;

import com.textbasedgame.items.ItemRarityEnum;
import dev.morphia.annotations.ExternalEntity;

import java.util.EnumMap;
import java.util.Map;

@ExternalEntity(target = LootConfig.class)
public class LootConfig {

    public record RaritiesBonuses(double baseFactor, Map<ItemRarityEnum, Double> rarityBonuses) {
        @Override
        public String toString() {
            return "RaritiesBonuses{" +
                    "baseFactor=" + baseFactor +
                    ", rarityBonuses=" + rarityBonuses +
                    '}';
        }

        public static RaritiesBonuses defaults() {
            EnumMap<ItemRarityEnum, Double> probs = new EnumMap<>(ItemRarityEnum.class);
            probs.put(ItemRarityEnum.COMMON, 1.0);
            probs.put(ItemRarityEnum.UNCOMMON, 1.0);
            probs.put(ItemRarityEnum.RARE, 1.0);
            probs.put(ItemRarityEnum.VERY_RARE, 1.0);
            probs.put(ItemRarityEnum.EPIC, 1.0);
            probs.put(ItemRarityEnum.LEGENDARY, 1.0);
            probs.put(ItemRarityEnum.MYTHIC, 1.0);
            return new RaritiesBonuses(1.0, probs);
        }
    }
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
