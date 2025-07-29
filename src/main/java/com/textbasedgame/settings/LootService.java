package com.textbasedgame.settings;

import com.textbasedgame.items.ItemRarityEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LootService {
    private static final Logger logger = LoggerFactory.getLogger(LootService.class);
    private final AppConfigManager cfg;
    private final EventModifierCache events;

    public LootService(AppConfigManager cfg, EventModifierCache events) {
        this.cfg = cfg;
        this.events = events;
    }

    public LootConfig getCurrentLootConfig() {
        return new LootConfig(
                this.getCurrentRaritiesBonuses(),
                this.cfg.getLootConfig().getItemChanceBonusMultiplier()
        );
    }

    public RaritiesBonuses getCurrentRaritiesBonuses() {
        RaritiesBonuses lootCfg = cfg.getLootConfig().getRaritiesBonusMultipliers();
        Double baseMultiplier = events.multiplier(ModifierType.BETTER_RARITY);
        Map<ItemRarityEnum, Double> currentRaritiesBonuses = lootCfg.getRarityBonuses();
        double baseValue = lootCfg.getBaseFactor() * baseMultiplier;

        Map<ItemRarityEnum, Double> newMap = new HashMap<>();
        //TODO:::!:!:!:
        for (ItemRarityEnum key: currentRaritiesBonuses.keySet()) {
            double currentValue = currentRaritiesBonuses.get(key);
            switch(key) {
                case MYTHIC -> {
                    currentValue = currentValue * events.multiplier(ModifierType.BETTER_RARITY_MYTHIC);
                }
                case LEGENDARY -> {
                    currentValue = currentValue * events.multiplier(ModifierType.BETTER_RARITY_LEGENDARY);
                }
                case EPIC -> {
                    currentValue = currentValue * events.multiplier(ModifierType.BETTER_RARITY_EPIC);
                }
                case UNCOMMON -> {
                    currentValue = currentValue * events.multiplier(ModifierType.BETTER_RARITY_UNCOMMON);
                }
            }
            newMap.put(key, currentValue);
        }

        logger.debug("getCurrentRaritiesBonuses -> Current data: BaseMultiplier: {}, Map: {}", baseMultiplier, newMap.entrySet().toString());
        return new RaritiesBonuses(baseValue, newMap);
    }
}
