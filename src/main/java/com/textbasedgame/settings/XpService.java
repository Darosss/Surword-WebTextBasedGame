package com.textbasedgame.settings;

import com.textbasedgame.characters.XpUtils;
import org.springframework.stereotype.Service;

@Service
public class XpService {
    private final AppConfigManager cfg;
    private final EventModifierCache events;

    public XpService(AppConfigManager cfg, EventModifierCache events) {
        this.cfg = cfg;
        this.events = events;
    }

    public long awardXpFromEnemy(XpUtils.EncounterContext ctx) {
        XpConfig xpCfg = cfg.getXpConfig();
        Double xpMultiplier = events.multiplier(ModifierType.XP);

        return XpUtils.calculateExperienceFromEnemy(ctx,xpCfg, xpMultiplier);
    }

    public long getNeededXpForNextLevel(int currentLevel) {
        return XpUtils.calculateExpToNextLevel(currentLevel, cfg.getXpConfig());
    }

}
