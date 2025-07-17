package com.textbasedgame.settings;

import com.textbasedgame.items.ItemRarityEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

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

    public LootConfig.RaritiesBonuses getCurrentRaritiesBonuses() {
        LootConfig.RaritiesBonuses lootCfg = cfg.getLootConfig().getRaritiesBonusMultipliers();
        Double baseMultiplier = events.multiplier(ModifierType.BETTER_RARITY);
        Map<ItemRarityEnum, Double> currentRaritiesBonuses = lootCfg.rarityBonuses();
        double baseValue = lootCfg.baseFactor() * baseMultiplier;

        Map<ItemRarityEnum, Double> newMap = currentRaritiesBonuses.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry ->{
                            switch(entry.getKey()) {
                                case MYTHIC -> {
                                    return entry.getValue() * events.multiplier(ModifierType.BETTER_RARITY_MYTHIC);
                                }
                                case LEGENDARY -> {
                                    return entry.getValue() * events.multiplier(ModifierType.BETTER_RARITY_LEGENDARY);
                                }
                                case EPIC -> {
                                    return entry.getValue() * events.multiplier(ModifierType.BETTER_RARITY_EPIC);
                                }
                                case UNCOMMON -> {
                                    return entry.getValue() * events.multiplier(ModifierType.BETTER_RARITY_UNCOMMON);
                                }
                            }
                            return entry.getValue();
                        }
                ));

        logger.debug("getCurrentRaritiesBonuses -> Current data: BaseMultiplier: {}, Map: {}", baseMultiplier, newMap.entrySet().toString());
        return new LootConfig.RaritiesBonuses(baseValue, newMap);
    }
}
