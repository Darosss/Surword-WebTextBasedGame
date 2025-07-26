package com.textbasedgame.characters;

import com.textbasedgame.settings.AppConfigManager;
import com.textbasedgame.settings.XpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MainCharacterService {
    private final AppConfigManager cfg;
    public record LevelUpLogicReturn(int gainedLevels) {}


    public MainCharacterService(AppConfigManager cfg, XpService xpService) {
        this.cfg = cfg;
        this.xpService = xpService;
    }

    private static final Logger logger = LoggerFactory.getLogger(MainCharacterService.class);
    XpService xpService;

    private boolean levelUp(MainCharacter character) {
        character.setLevel(character.getLevel() + 1);
        character.updateHealthBasedOnMaxHealth();
        logger.debug("Level up! New level: {}", character.getLevel());
        return true;
    }

    private LevelUpLogicReturn checkLevelUp(MainCharacter character) {
        long expToLevelUp = xpService.getNeededXpForNextLevel(character.getLevel());
        int gainedLevels = 0;

        while (character.getExperience() >= expToLevelUp) {
            character.decreaseExperience(expToLevelUp);
            boolean levelUp = levelUp(character);
            expToLevelUp = XpUtils.calculateExpToNextLevel(character.getLevel(), cfg.getXpConfig());
            if (levelUp) gainedLevels++;
        }
        return new LevelUpLogicReturn(gainedLevels);

    }
    public LevelUpLogicReturn gainExperience(MainCharacter character, long experiencePoints) {
        if (experiencePoints > 0) {
            character.increaseExperience(experiencePoints);
            return checkLevelUp(character);
        }
        return new LevelUpLogicReturn(0);
    }
}
