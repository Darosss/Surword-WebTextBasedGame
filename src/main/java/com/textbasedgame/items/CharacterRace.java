package com.textbasedgame.items;

import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.statistics.BaseStatisticsNamesEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum CharacterRace {
    HUMAN(
            new HashMap<>(
                    Map.of(
                            BaseStatisticsNamesEnum.STRENGTH, 5.0,
                            BaseStatisticsNamesEnum.DEXTERITY, 5.0,
                            BaseStatisticsNamesEnum.INTELLIGENCE, 5.0,
                            BaseStatisticsNamesEnum.CONSTITUTION, 5.0,
                            BaseStatisticsNamesEnum.CHARISMA, 5.0,
                            BaseStatisticsNamesEnum.LUCK, 5.0
                    )
            ),
            new HashMap<>(
                    Map.of(
                            AdditionalStatisticsNamesEnum.MAX_DAMAGE, 5.0,
                            AdditionalStatisticsNamesEnum.MAX_HEALTH, 5.0,
                            AdditionalStatisticsNamesEnum.INITIATIVE, 5.0
                    )
            )
    ),

    ELF(
            new HashMap<>(
                    Map.of(
                            BaseStatisticsNamesEnum.STRENGTH, 4.0,
                            BaseStatisticsNamesEnum.DEXTERITY, 7.0,
                            BaseStatisticsNamesEnum.INTELLIGENCE, 6.0,
                            BaseStatisticsNamesEnum.CONSTITUTION, 4.0,
                            BaseStatisticsNamesEnum.CHARISMA, 6.0,
                            BaseStatisticsNamesEnum.LUCK, 5.0
                    )
            ),
            new HashMap<>(
                    Map.of(
                            AdditionalStatisticsNamesEnum.MAX_DAMAGE, 4.5,
                            AdditionalStatisticsNamesEnum.DODGE, 6.5,
                            AdditionalStatisticsNamesEnum.INITIATIVE, 7.0
                    )
            )
    ),

    DWARF(
            new HashMap<>(
                    Map.of(
                            BaseStatisticsNamesEnum.STRENGTH, 7.0,
                            BaseStatisticsNamesEnum.DEXTERITY, 4.0,
                            BaseStatisticsNamesEnum.INTELLIGENCE, 4.5,
                            BaseStatisticsNamesEnum.CONSTITUTION, 8.0,
                            BaseStatisticsNamesEnum.CHARISMA, 3.5,
                            BaseStatisticsNamesEnum.LUCK, 5.0
                    )
            ),
            new HashMap<>(
                    Map.of(
                            AdditionalStatisticsNamesEnum.MAX_DAMAGE, 6.5,
                            AdditionalStatisticsNamesEnum.ARMOR, 7.5,
                            AdditionalStatisticsNamesEnum.BLOCK, 6.5
                    )
            )
    ),

    ORC(
            new HashMap<>(
                    Map.of(
                            BaseStatisticsNamesEnum.STRENGTH, 8.0,
                            BaseStatisticsNamesEnum.DEXTERITY, 5.0,
                            BaseStatisticsNamesEnum.INTELLIGENCE, 3.5,
                            BaseStatisticsNamesEnum.CONSTITUTION, 7.0,
                            BaseStatisticsNamesEnum.CHARISMA, 3.0,
                            BaseStatisticsNamesEnum.LUCK, 4.5
                    )
            ),
            new HashMap<>(
                    Map.of(
                            AdditionalStatisticsNamesEnum.MAX_DAMAGE, 7.5,
                            AdditionalStatisticsNamesEnum.CRITIC, 5.5,
                            AdditionalStatisticsNamesEnum.THREAT, 7.0
                    )
            )
    ),

    HALFLING(
            new HashMap<>(
                    Map.of(
                            BaseStatisticsNamesEnum.STRENGTH, 3.5,
                            BaseStatisticsNamesEnum.DEXTERITY, 7.5,
                            BaseStatisticsNamesEnum.INTELLIGENCE, 5.5,
                            BaseStatisticsNamesEnum.CONSTITUTION, 4.0,
                            BaseStatisticsNamesEnum.CHARISMA, 6.0,
                            BaseStatisticsNamesEnum.LUCK, 8.0
                    )
            ),
            new HashMap<>(
                    Map.of(
                            AdditionalStatisticsNamesEnum.MIN_DAMAGE, 4.5,
                            AdditionalStatisticsNamesEnum.DODGE, 7.0,
                            AdditionalStatisticsNamesEnum.INITIATIVE, 6.5
                    )
            )
    );

    private final Map<BaseStatisticsNamesEnum, Double> startStatistics;
    private final Map<AdditionalStatisticsNamesEnum, Double> startAdditionalStatistics;

    CharacterRace(
            Map<BaseStatisticsNamesEnum, Double> startStatistics,
            Map<AdditionalStatisticsNamesEnum, Double> startAdditionalStatistics
    ) {
        this.startStatistics = startStatistics;
        this.startAdditionalStatistics = startAdditionalStatistics;
    }

    public Map<BaseStatisticsNamesEnum, Double> getStartStatistics() {
        return startStatistics;
    }

    public Map<AdditionalStatisticsNamesEnum, Double> getStartAdditionalStatistics() {
        return startAdditionalStatistics;
    }

        public String getImagePath(ItemRarityEnum rarity, ItemsSubtypes type) {
            String baseDir = "images/mercs/";
            String prefix = type.name().toLowerCase() + "_" + rarity.name().toLowerCase();

            Path dir = Paths.get(baseDir);
            try (Stream<Path> files = Files.list(dir)) {
                List<String> matching = files
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.startsWith(prefix) && name.endsWith(".webp"))
                        .toList();

                if (!matching.isEmpty()) {
                    return baseDir + matching.get((int) (Math.random() * matching.size()));
                }

                // Fallback to base type images
                String basePrefix = type.name().toLowerCase();
                List<String> defaultMatches = Files.list(dir)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.startsWith(basePrefix) && name.endsWith(".webp"))
                        .toList();

                if (!defaultMatches.isEmpty()) {
                    return baseDir + defaultMatches.get((int) (Math.random() * defaultMatches.size()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return baseDir + "default.webp";
        }
}
