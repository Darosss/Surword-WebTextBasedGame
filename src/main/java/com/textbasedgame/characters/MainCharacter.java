package com.textbasedgame.characters;

import com.textbasedgame.characters.equipment.CharacterEquipment;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.statistics.BaseStatisticsNamesEnum;
import com.textbasedgame.users.User;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;

import java.util.Map;

public class MainCharacter extends Character {
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

    public static UpdateOperator getMorphiaSetExperience(long value) {
        return UpdateOperators.set("experience", value);
    }

    public void increaseExperience(long value) {
        this.experience += value;
    }

    public void decreaseExperience(long value) {
        this.experience -= value;
    }


    public Long getExperience() {
        return experience;
    }

}
