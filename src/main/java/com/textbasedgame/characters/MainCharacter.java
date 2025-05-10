package com.textbasedgame.characters;

import com.textbasedgame.characters.equipment.CharacterEquipment;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.statistics.BaseStatisticsNamesEnum;
import com.textbasedgame.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MainCharacter extends Character {
    private static final Logger logger = LoggerFactory.getLogger(MainCharacter.class);

    private Long experience;

    public MainCharacter() {
    };
    //TODO: find and improve boolean asNew -> this was done to prevent morphia to use this constructor and use 0-arguments instead
    public MainCharacter(String name, User user, CharacterEquipment equipment, boolean asNew) {
        super(name, user, equipment);
        this.experience = 0L;
    }

    public MainCharacter(String name, User user, CharacterEquipment equipment, int level,
                         Map<BaseStatisticsNamesEnum, Integer> baseStatistics,
                         Map<AdditionalStatisticsNamesEnum, Integer> additionalStatistics){
        super(name, user, equipment, level, baseStatistics, additionalStatistics);
    }
    public void gainExperience(long experiencePoints) {
        if (experiencePoints > 0) {
            this.experience += experiencePoints;
            this.checkLevelUp();
        }
    }
    public long getExpToLevelUp () {
        return ExperienceUtils.calculateExpToNextLevel(this.getLevel());
    }

    private void checkLevelUp() {
        long expToLevelUp = this.getExpToLevelUp();
        while (this.experience >= expToLevelUp) {
            this.experience -= expToLevelUp;
            this.levelUp();
            expToLevelUp = ExperienceUtils.calculateExpToNextLevel(this.getLevel());
        }
    }
    private void levelUp() {
        this.setLevel(this.getLevel() + 1);
        this.updateHealthBasedOnMaxHealth();
        logger.debug("Level up! New level: {}", this.getLevel());
    }

    public Long getExperience() {
        return experience;
    }
}
