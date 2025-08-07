package com.textbasedgame.characters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.textbasedgame.statistics.*;
import org.bson.types.ObjectId;

import java.util.Map;

public class BaseHero {
    private ObjectId baseHeroId;
    private String name;
    private Integer level = 1;
    private Integer health = 1;

    private HeroStatisticsObject stats;
    protected void updateHealthBasedOnMaxHealth(){
        this.health = this.stats.getMaxHealthEffValue();
    }
    public BaseHero(){};
    public BaseHero(String name) {
        this.name = name;
        this.level = 1;
        this.stats = new HeroStatisticsObject(true);
        this.updateHealthBasedOnMaxHealth();
    }
    public BaseHero(String name, int level) {
        this.name = name;
        this.level = level;
        this.stats = new HeroStatisticsObject(true);
    }
    public BaseHero(String name, int level,
                    Map<BaseStatisticsNamesEnum, Integer> baseStatistics) {
        this.name = name;
        this.level = level;
        this.stats = new HeroStatisticsObject(true);
        this.setStatisticsByNameIntegerMap(baseStatistics, StatisticsUtils.StatisticUpdateType.VALUE);
        this.updateHealthBasedOnMaxHealth();

    }
    public BaseHero(String name, int level,
                    Map<BaseStatisticsNamesEnum, Integer> baseStatistics,
                    Map<AdditionalStatisticsNamesEnum, Integer> additionalStatistics
    ) {
        this.name = name;
        this.level = level;
        this.stats = new HeroStatisticsObject(true);
        this.setStatisticsByNameIntegerMap(baseStatistics, StatisticsUtils.StatisticUpdateType.VALUE);
        this.setAdditionalStatisticsByNameIntegerMap(additionalStatistics, StatisticsUtils.StatisticUpdateType.VALUE);
        this.updateHealthBasedOnMaxHealth();
    }

    public Integer getLevel() {
        return level;
    }
    public Integer getHealth() {
        return health;
    }

    protected void setHealth(int newHealth) {
        if(newHealth > 0) this.health = Math.min(newHealth, this.stats.getMaxHealthEffValue());
        else this.health = 0;
    }

    public void decreaseHealth(int value) {
        this.setHealth(this.health - value);
    }

    public void increaseHealth(int value){
        this.setHealth(this.health + value);
    }
    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void setAdditionalStatisticsByNameIntegerMap(Map<AdditionalStatisticsNamesEnum, Integer> additionalStatistics, StatisticsUtils.StatisticUpdateType updateType){
        additionalStatistics.forEach((k, v)-> this.stats.increaseAdditionalStatistic(k, v, updateType));
    }
    protected void setStatisticsByNameIntegerMap(Map<BaseStatisticsNamesEnum, Integer> statistics, StatisticsUtils.StatisticUpdateType updateType){
        statistics.forEach((k, v)-> this.stats.increaseStatistic(k, v, updateType));
    }

    public int getAdditionalStatEffective(AdditionalStatisticsNamesEnum name) {
        return this.stats.getAdditionalStatistics().get(name).getEffectiveValue();
    }

    public HeroStatisticsObject getStats() {
        return stats;
    }

    public ObjectId getId() {
        return baseHeroId;
    }
    protected void setId(ObjectId id){
        this.baseHeroId = id;
    }

    @JsonProperty("id")
    public String getIdString() {
        if(this.getId() == null) return "";
        return this.getId().toHexString();
    }
}
