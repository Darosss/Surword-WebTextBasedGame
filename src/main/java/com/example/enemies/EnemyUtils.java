package com.example.enemies;

import com.example.items.Item;
import com.example.items.ItemTypeEnum;
import com.example.items.ItemUtils;
import com.example.skirmishes.EnemySkirmishDifficulty;
import com.example.statistics.AdditionalStatisticsNamesEnum;
import com.example.statistics.BaseStatisticsNamesEnum;
import com.example.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnemyUtils {
    private static final Logger logger = LoggerFactory.getLogger(EnemyUtils.class);

    public record LevelRange(int min, int max){}

    public record ItemProbabilityLoot(double percent, int minItems, int maxItems){};
    public static LevelRange getEnemyLevelRanges(EnemySkirmishDifficulty difficulty, int characterLevel){
        return switch (difficulty){
            case NOOB -> new LevelRange(1, Math.max(1, characterLevel/2));
            case WEAKER -> new LevelRange(Math.max(1, characterLevel-10), Math.max(1, characterLevel-2) );
            case EQUAL -> new LevelRange(characterLevel, characterLevel);
            case FAIR -> new LevelRange(Math.max(1, characterLevel-1),Math.max(1, characterLevel+1) );
            case STRONGER -> new LevelRange(characterLevel+5, characterLevel+10);
            case IMPOSSIBLE -> new LevelRange(characterLevel+10, characterLevel*2);
        };
    }


    public static double getEnemyStatsMultiplier(EnemyType type){
        return switch (type){
            case COMMON -> 0.5;
            case UNCOMMON -> 1;
            case RARE -> 1.2;
            case EPIC -> 2;
            case BOSS -> 3;
            case ANCESTOR -> 5;
        };
    }

    public static Enemy createRandomEnemy(String name, int level, EnemyType enemyType,
                      Map<BaseStatisticsNamesEnum, Integer> baseStatistics,
                      Map<AdditionalStatisticsNamesEnum, Integer> additionalStatistics
    ){
        return new Enemy(name, level,enemyType, baseStatistics, additionalStatistics);
    }

    public static Enemy createRandomEnemyBasedOnLevel(String name, int level, EnemyType type, double statsMultiplier){
        int multipliedStat = (int) (level * statsMultiplier);
        return createRandomEnemy(name, level, type,
                Map.of(
                        BaseStatisticsNamesEnum.STRENGTH, multipliedStat,
                        BaseStatisticsNamesEnum.DEXTERITY, multipliedStat,
                        BaseStatisticsNamesEnum.CHARISMA, multipliedStat,
                        BaseStatisticsNamesEnum.CONSTITUTION, multipliedStat,
                        BaseStatisticsNamesEnum.INTELLIGENCE, multipliedStat,
                        BaseStatisticsNamesEnum.LUCK, multipliedStat
                ),
                Map.of(
                        AdditionalStatisticsNamesEnum.INITIATIVE, multipliedStat,
                        AdditionalStatisticsNamesEnum.MAX_HEALTH, multipliedStat
                )
        );
    }


    public static EnemyType getEnemyTypeDependsOnProbability() {
        return getEnemyTypeDependsOnProbability(1);
    }
    public static EnemyType getEnemyTypeDependsOnProbability(double additionalBonusMultiplier) {
        double randomDouble = RandomUtils.getRandomValueWithinRange(0, 1.0);
        for(EnemyType value: EnemyType.values()){
            double probabilityWithBonus = Math.min(1.0, value.getProbability() * Math.max(0.1, additionalBonusMultiplier));
            if(randomDouble <= probabilityWithBonus) return value;

        }

        return EnemyType.COMMON;
    }

    public static EnemyType getEnemyTypeBasedOnDungeonLevel(int dungeonLevel) {
        double additionalBonusMultiplier = 3.1;
        if(dungeonLevel % 10 == 0) additionalBonusMultiplier = 5.0;

        return getEnemyTypeDependsOnProbability(additionalBonusMultiplier);
    }

    public static int getCountOfDungeonEnemiesBasedOnDungeonLevel(int dungeonLevel) {
        List<Pair<Integer, Double>> countEnemiesData = new ArrayList<>(
                List.of(
                        Pair.of(4, 0.2),
                        Pair.of(3, 0.5),
                        Pair.of(2, 1.0)
                )
        );

        for(Pair<Integer, Double> data : countEnemiesData) {
            double percentChance = data.getSecond();
            if(RandomUtils.checkPercentageChance(percentChance + (percentChance * dungeonLevel) )) return data.getFirst();
        }

        return 1;
    }

    public static EnemyType getEnemyTypeBasedOnSkirmishDifficulty(EnemySkirmishDifficulty difficulty){
        return getEnemyTypeDependsOnProbability(getMultiplierForEnemyType(difficulty));
    }

    private static double getMultiplierForEnemyType(EnemySkirmishDifficulty difficulty){
        return switch (difficulty){
            case NOOB ->  2.0;
            case WEAKER -> 0.8;
            case EQUAL -> 1.1;
            case FAIR ->  1.5;
            case STRONGER -> 2.2;
            case IMPOSSIBLE -> 3;
        };
    }

    private static ItemProbabilityLoot getItemProbabilityLootByEnemyType(EnemyType type, boolean isAlive){
        double isAlivePercentAdjust = isAlive ? 0.8 : 1;
        return switch (type){
            case COMMON -> new ItemProbabilityLoot(15 * isAlivePercentAdjust, 1, 1);
            case UNCOMMON -> new ItemProbabilityLoot(28 * isAlivePercentAdjust, 1, 1);
            case RARE -> new ItemProbabilityLoot(42 * isAlivePercentAdjust, 1, 2);
            case EPIC -> new ItemProbabilityLoot(60 * isAlivePercentAdjust, 1, 2);
            case BOSS -> new ItemProbabilityLoot(100 * isAlivePercentAdjust, 2, 3);
            case ANCESTOR -> new ItemProbabilityLoot(100 * isAlivePercentAdjust, 3, 6);
        };
    }

    public static List<Item> checkLootFromEnemy(EnemyType type, int enemyLevel, boolean isAlive){
        List<Item> items = new ArrayList<>();
        ItemProbabilityLoot itemProbData = getItemProbabilityLootByEnemyType(type, isAlive);
        logger.debug("Item prob data, percent: {}, minItems: {}, maxItems: {}", itemProbData.percent(), itemProbData.minItems(), itemProbData.maxItems());
        if(RandomUtils.checkPercentageChance(itemProbData.percent())) {
            int numberOfItems = RandomUtils.getRandomValueWithinRange(itemProbData.minItems(), itemProbData.maxItems());
            for (int i = 0; i< numberOfItems; i++){
                int itemLevel = RandomUtils.getRandomValueWithinRange(Math.max(1, enemyLevel - 5), enemyLevel + 8);
                ItemTypeEnum itemType = ItemUtils.getRandomItemType();
                //TODO: make random name generator for items
                items.add(ItemUtils.generateRandomItemWithoutBaseStats(
                        itemType + " from enemy: "+type+" | level: "+enemyLevel, itemLevel, itemType)
                );
            }

            for (Item item : items) {
                logger.debug("Looted item. Item level: {}, itemName: {}", item.getLevel(), item.getName());
            }
        }else {
            logger.debug("Didn't loot any items from enemy type: {}, level: {}, isAlive: {}", type, enemyLevel, isAlive);
        }
        return items;
    }
}
