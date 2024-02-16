package com.example.characters;
import com.example.characters.equipment.CharacterEquipment;
import com.example.items.Item;
import com.example.statistics.*;
import com.example.users.User;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.Map;

@Entity("characters")
public class Character  extends BaseHero {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    private ObjectId id;

    @JsonIgnore
    @Reference(idOnly = true, lazy=true)
    private User user;

    @JsonIgnoreProperties("character")
    @Reference
    private CharacterEquipment equipment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Character() {
        super();
    }

    public Character(String name, User user, CharacterEquipment equipment) {
        super(name);
        //TODO: make parameter and depend on class change basics
        //For set equipment here and character in equipment
        this.equipment = equipment;
        this.user = user;
    }
    public Character(String name, User user, CharacterEquipment equipment, int level) {
        this(name, user, equipment);
        this.level = level;
    }

    //TODO: one of constructor should have = class type which will constitute what and how many default stats there are
    public Character(String name, User user, CharacterEquipment equipment, int level,
                     Map<BaseStatisticsNamesEnum, Integer> baseStatistics
                     ) {
        super(name, level, baseStatistics);

        //TODO: tbh its for debug, ignore
        //For set equipment here and character in equipment
        this.equipment = equipment;
        this.user = user;
    }

    public Character(String name, User user, CharacterEquipment equipment, int level,
                     Map<BaseStatisticsNamesEnum, Integer> baseStatistics,
                     Map<AdditionalStatisticsNamesEnum, Integer> additionalStatistics
    ) {
        super(name, level, baseStatistics, additionalStatistics);
        //TODO: tbh its for debug, ignore
        //For set equipment here and character in equipment
        this.equipment = equipment;
        this.user = user;
    }

    public void calculateStatisticByItem(Item item, boolean isEquip){
           item.getStatistics().getAdditionalStatistics().forEach((k, v)->{
            AdditionalStatisticsNamesEnum castedKey  = AdditionalStatisticsNamesEnum.valueOf(k);
            if(isEquip){
                this.stats.updateAdditionalStatisticsOnEquip(castedKey, v.getValue(), v.getPercentageValue());
            } else {
                this.stats.updateAdditionalStatisticsOnUnEquip(castedKey, v.getValue(), v.getPercentageValue());
            }
        });
        item.getStatistics().getBaseStatistics().forEach((k, v)->{
            BaseStatisticsNamesEnum castedKey  = BaseStatisticsNamesEnum.valueOf(k);
            if (isEquip) {
                this.stats.updateStatisticsOnEquip(castedKey, v.getValue(), v.getPercentageValue());
            } else {
                this.stats.updateStatisticsOnUnEquip(castedKey, v.getValue(), v.getPercentageValue());
            }
        });
    }
    @Override
    public String toString() {
        return "Character{" +
                "id=" + id +
                ", user=" + user +
                ", equipment=" + equipment +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    @Override
    public ObjectId getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CharacterEquipment getEquipment() {
        return equipment;
    }
    public User getUser() {
        return user;
    }



}
