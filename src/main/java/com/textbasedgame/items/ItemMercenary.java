package com.textbasedgame.items;

import com.textbasedgame.items.statistics.ItemStatistics;
import com.textbasedgame.items.statistics.ItemStatisticsObject;
import com.textbasedgame.users.User;

import java.util.HashMap;
import java.util.Map;

public class ItemMercenary extends Item{
    public ItemMercenary(){};

    public ItemMercenary(String name, User user, String description, Integer level,
                         Integer value, ItemRarityEnum rarity, float weight,
                         ItemsSubtypes subtype,
                         CharacterRace race

    ){
        super(name, user, description, level, value, ItemTypeEnum.MERCENARY, subtype, rarity, weight);
        this.race = race;

        Map<String, ItemStatisticsObject> baseStatsModified = new HashMap<>();
        Map<String, ItemStatisticsObject> additionalStatsModified = new HashMap<>();

        race.getStartStatistics().entrySet().stream().map((entry)->baseStatsModified.put(
                entry.getKey().toString(),
                new ItemStatisticsObject(entry.getKey().toString(), (int) (entry.getValue() * level), 0))
        );
        race.getStartAdditionalStatistics().entrySet().stream().map((entry)->additionalStatsModified.put(
                entry.getKey().toString(),
                new ItemStatisticsObject(entry.getKey().toString(), (int) (entry.getValue() * level), 0))
        );
        this.statistics = new ItemStatistics(baseStatsModified, additionalStatsModified, level, rarity, this.getSubtype());
    }


    public CharacterRace getRace() {
        return race;
    }

    public void setRace(CharacterRace race) {
        this.race = race;
    }

    @Override
    public String toString() {
        return "ItemMercenary{" +
                "race=" + race +
                ", " + super.toString() +
                '}';
    }
}
