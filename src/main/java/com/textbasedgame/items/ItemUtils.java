package com.textbasedgame.items;

import com.textbasedgame.items.statistics.*;
import com.textbasedgame.settings.RaritiesBonuses;
import com.textbasedgame.users.User;
import com.textbasedgame.utils.RandomUtils;
import org.springframework.data.util.Pair;

import java.util.*;

public class ItemUtils {
    public record GenerateItemNameDesc(String name, String description) {}
    private static final UniqueNamesConfig uniqueNamesConfig = new UniqueNamesConfig();
    private ItemUtils() {}


    public static double getItemLevelFactorValueStatistics(int itemLevel) {
        double ITEM_STATISTIC_FACTOR_PER_LEVEL = 1.5;
        return (ITEM_STATISTIC_FACTOR_PER_LEVEL * itemLevel);
    }
    public static double getItemLeveFactorPercentStatistics(int itemLevel) {
        double ITEM_STATISTIC_PERCENT_FACTOR_PER_LEVEL = 0.25;
        return (ITEM_STATISTIC_PERCENT_FACTOR_PER_LEVEL * itemLevel);
    }

    public static int getItemValueBasedOnRarityLevel(int level, ItemRarityEnum rarity) {
        double BASE_ITEM_COST = 100.0; double BASE_ITEM_COST_PER_LEVEL = 50;
        double itemValue = BASE_ITEM_COST + (level * BASE_ITEM_COST_PER_LEVEL);

        for(ItemRarityEnum value: ItemRarityEnum.values()){
            itemValue += itemValue * value.getBonusCostValue();
            if(rarity.equals(value)) break;
        }

        return (int) itemValue;
    }
    public static ItemTypeEnum getRandomItemType() {

        Random random = new Random();
        ItemTypeEnum[] values =ItemTypeEnum.values();
        return values[random.nextInt(values.length)];
    }

    public static ItemPrefixesEnum getRandomItemPrefix(){
        return RandomUtils.getRandomItemFromArray( ItemPrefixesEnum.values());

    }
    public static ItemSuffixesEnum getRandomItemSuffix() {
        return RandomUtils.getRandomItemFromArray( ItemSuffixesEnum.values());
    }

    private static String genNameFromSuffixPrefix(ItemPrefixesEnum prefix, ItemSuffixesEnum suffix, ItemsSubtypes subtype) {
        return prefix.getRandomName() + suffix.getRandomName()  + " " + subtype.name().toLowerCase();
    }

    public static GenerateItemNameDesc generateItemNameDesc(Optional<String> overrideName, ItemRarityEnum rarity, ItemPrefixesEnum prefix, ItemSuffixesEnum suffix, ItemsSubtypes subtype){
      String name =overrideName.orElse("");
        return switch(rarity) {
          case EPIC -> new GenerateItemNameDesc(!name.isEmpty() ? name : prefix.getRandomName() + " " + subtype.name().toLowerCase(), "");
          case LEGENDARY -> new GenerateItemNameDesc(!name.isEmpty() ? name : genNameFromSuffixPrefix(prefix, suffix, subtype), "Legendary " + subtype.name().toLowerCase());
          case MYTHIC -> {
              UniqueNamesConfig.NamesByCategory names = uniqueNamesConfig.pickUniqueName(subtype);
              String currentName = !name.isEmpty() ? name : names.name().isEmpty() ? genNameFromSuffixPrefix(prefix, suffix, subtype) : names.name();
              String currentDesc = names.description().isEmpty() ? "Mythic " + subtype.name().toLowerCase() : names.description();
              yield new GenerateItemNameDesc(currentName, currentDesc);
          }
          default -> new GenerateItemNameDesc(!name.isEmpty() ? name : subtype.name().toLowerCase(), "");
      };
    }

    public static ItemRarityEnum getRandomRarityItem(RaritiesBonuses bonuses) {
        double randomDouble = RandomUtils.getRandomValueWithinRange(0, 1.0);
        ItemRarityEnum[] values =ItemRarityEnum.values();

        for(int i = values.length - 1; i >= 0; i--){
            ItemRarityEnum currentRarity = values[i];
            double baseProbability = currentRarity.getProbability();
            double rarityBonus     = bonuses.getRarityBonuses().getOrDefault(currentRarity, 1.0);
            double adjustedModifier = Math.max(0.1, bonuses.getBaseFactor() * rarityBonus);
            double probabilityWithBonus = Math.min(1.0, baseProbability * adjustedModifier);
            if(randomDouble <= probabilityWithBonus) return currentRarity;
        }

        return ItemRarityEnum.COMMON;
    }

    public static int getConsumableItemHpGain(int level, ItemRarityEnum rarity, ItemsSubtypes subtype){
        double hpGain = subtype.getHealthGainPerLevel() * level;
        for(ItemRarityEnum value: ItemRarityEnum.values()){
            hpGain += hpGain * (value.getBonusValue()/100);
            if(rarity.equals(value)) break;
        }

        return (int) hpGain;
    }

    public static Item generateItemWithoutBaseStats(
            User user, String itemName, String description, ItemTypeEnum type, ItemsSubtypes subtype, int level, ItemRarityEnum rarity, ItemPrefixesEnum prefix, ItemSuffixesEnum suffix
    ) {
        return generateItem(user, itemName, description, type, subtype, level, rarity, prefix,suffix);
    }

    private static Item generateItem(
            User user,
            String itemName, String description, ItemTypeEnum type, ItemsSubtypes subtype, int level,  ItemRarityEnum rarity,
            ItemPrefixesEnum prefix, ItemSuffixesEnum suffix
    ){

        Pair<Float, Float> weightRange = subtype.getWeightRange();
        float itemWeight = RandomUtils.getRandomValueWithinRange(weightRange.getFirst(), weightRange.getSecond());
        int itemValue = getItemValueBasedOnRarityLevel(level, rarity);
        switch (type){
            case CONSUMABLE -> {
                return new ItemConsumable(itemName, user, description,
                        level, itemValue,
                        rarity, itemWeight, subtype);
            }
            case MERCENARY -> {
                CharacterRace race = RandomUtils.getRandomItemFromArray(CharacterRace.values());
                return new ItemMercenary(itemName, user, description,
                        level, getItemValueBasedOnRarityLevel(level, rarity),
                        rarity, itemWeight, subtype,
                        race
                );
            }
        }
        return new ItemWearable(itemName, user, description,
                level, getItemValueBasedOnRarityLevel(level, rarity), type,
                subtype, rarity, itemWeight,prefix, suffix, new HashMap<>(), new HashMap<>());

    }

    public static Item generateRandomItemWithoutBaseStats(User user, int itemLevel, ItemTypeEnum itemType,
                                                          RaritiesBonuses raritiesBonuses, Optional<String> overrideName){
        ItemsSubtypes subtype = RandomUtils.getRandomItemFromArray(itemType.getSubtypes());
        ItemRarityEnum rarity = getRandomRarityItem(raritiesBonuses);
        ItemPrefixesEnum randomPrefix = getRandomItemPrefix();
        ItemSuffixesEnum randomSuffix = getRandomItemSuffix();
        GenerateItemNameDesc nameDescData = generateItemNameDesc(overrideName, rarity, randomPrefix, randomSuffix, subtype);
        return  generateItemWithoutBaseStats(user, nameDescData.name, nameDescData.description, itemType, subtype, itemLevel,
                rarity, randomPrefix, randomSuffix
        );
    };



    public static Item generateRandomItem(User user, RaritiesBonuses raritiesBonuses){
        ItemTypeEnum randomItemType = getRandomItemType();
        return generateRandomItemWithoutBaseStats(user, RandomUtils.getRandomValueWithinRange(1,100), randomItemType, raritiesBonuses, Optional.empty());
    }
    public static List<Item> generateRandomItems(User user, int count, RaritiesBonuses raritiesBonuses) {
        List<Item> generatedItems = new ArrayList<>();

        for (int i = 0; i<= count; i ++){
            generatedItems.add(generateRandomItem(user, raritiesBonuses));
        }
    return generatedItems;
    }

    public static <KeyType extends String> Map<KeyType, ItemStatisticsObject> getMergedItemStatisticsObjectMaps(Map<KeyType, ItemStatisticsObject> destination, Map<KeyType, ItemStatisticsObject> source) {
        Map<KeyType, ItemStatisticsObject> newMap = new HashMap<>();
        source.forEach((sourceKey, sourceValue) -> {
            mergeHelper(newMap, sourceKey, sourceValue);
        });
        destination.forEach((sourceKey, destinationValue) -> {
            mergeHelper( newMap, sourceKey, destinationValue);
        });

        return newMap;
    }

    private static <KeyType extends String> void mergeHelper(Map<KeyType, ItemStatisticsObject> mapToMerge, KeyType currentKey, ItemStatisticsObject currentValue) {
        if (mapToMerge.containsKey(currentKey)) {
            ItemStatisticsObject mapToMergeVal = mapToMerge.get(currentKey);
            int summedValue = currentValue.getValue() + mapToMergeVal.getValue();
            float summedPercentageValue = currentValue.getPercentageValue() + mapToMergeVal.getPercentageValue();

            mapToMerge.put(currentKey,
                    new ItemStatisticsObject(currentKey, summedValue, summedPercentageValue)
            );
        } else {
            mapToMerge.put(currentKey, new ItemStatisticsObject(
                    currentValue.getName(), currentValue.getValue(), currentValue.getPercentageValue()
            ));
        }
    }
}
