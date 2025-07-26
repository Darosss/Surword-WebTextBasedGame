package com.textbasedgame.utils;

import com.textbasedgame.enemies.EnemyType;
import com.textbasedgame.settings.AppConfigManager;
import com.textbasedgame.settings.EventModifierCache;
import com.textbasedgame.settings.GoldConfig;
import com.textbasedgame.settings.ModifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoldService {
    private static final Logger logger = LoggerFactory.getLogger(GoldService.class);
    private final AppConfigManager cfg;
    private final EventModifierCache events;
    private GoldService(AppConfigManager cfg, EventModifierCache events) {
        this.cfg = cfg;
        this.events = events;
    }

    public long calculateGoldFromEnemy(int enemyLevel, EnemyType enemyType, boolean survived) {
        GoldConfig currentConfigs = this.cfg.getGoldConfigs();
        double enemyTypeBonus = enemyType.getBonusGold();
        double eventsMultiplier = events.multiplier(ModifierType.GOLD);
        long gold = (long) Math.max(1, (enemyLevel * currentConfigs.getBaseGoldPerEnemyLevel()) * enemyTypeBonus * eventsMultiplier);

        logger.debug("Enemy level: {}, enemyType: {} -> {}, survived: {}, gold: {}, goldIsAlive: {}:",
                enemyLevel, enemyType, enemyTypeBonus, survived, gold, (int) Math.max(1, (gold + (gold * (currentConfigs.getEnemyDefeatSurvivedAdjustGold())))) );

        return  !survived ?
                gold :
                (int) Math.max(1, (gold + (gold * (currentConfigs.getEnemyDefeatSurvivedAdjustGold()))));
    }
}
