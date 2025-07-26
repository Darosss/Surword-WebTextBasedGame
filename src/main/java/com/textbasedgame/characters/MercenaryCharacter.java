package com.textbasedgame.characters;

import com.textbasedgame.characters.equipment.CharacterEquipment;
import com.textbasedgame.items.Item;
import com.textbasedgame.items.ItemMercenary;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.statistics.BaseStatisticsNamesEnum;
import com.textbasedgame.statistics.StatisticsUtils;
import com.textbasedgame.users.User;
import dev.morphia.annotations.Reference;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;

import javax.annotation.Nullable;

public class MercenaryCharacter extends Character{
    @Reference(idOnly = true, lazy = true)
    private ItemMercenary mercenary;
    public MercenaryCharacter() {
    }

    //TODO: find and improve boolean asNew -> this was done to prevent morphia to use this constructor and use 0-arguments instead
    public MercenaryCharacter(String name, User user, CharacterEquipment equipment, boolean asNew) {
        super(name, user, equipment);
    }

    public static UpdateOperator getMorphiaSetCharacterMercenary(@Nullable ItemMercenary character) {
        if(character == null) return UpdateOperators.unset("mercenary");
        else return UpdateOperators.set("mercenary", character);
    }

    public void setMercenary(@Nullable ItemMercenary mercenaryValue) {
        if(mercenaryValue == null) {
            this.setLevel(1);
            this.setStatisticsOnUnEquipMercenary();
        }else {
            this.setLevel(mercenaryValue.getLevel());
            this.setStatisticsOnEquipMercenary(mercenaryValue);
        }
        this.mercenary = mercenaryValue;
    }

    public ItemMercenary getMercenary() {
        return mercenary;
    }

    private void setStatisticsOnEquipMercenary(ItemMercenary equippedMercenary) {
        equippedMercenary.getStatistics().getBaseStatistics().forEach((k,v)->{
            this.getStats().updateStatistic(BaseStatisticsNamesEnum.valueOf(k), v.getValue(), StatisticsUtils.StatisticUpdateType.VALUE);
        });
        equippedMercenary.getStatistics().getAdditionalStatistics().forEach((k,v)->{
            this.getStats().updateAdditionalStatistic(AdditionalStatisticsNamesEnum.valueOf(k), v.getValue(), StatisticsUtils.StatisticUpdateType.VALUE);
        });
    }
    private void setStatisticsOnUnEquipMercenary() {
        this.getStats().getAdditionalStatistics().forEach((k,v)->
                this.getStats().updateAdditionalStatistic(k, 1, StatisticsUtils.StatisticUpdateType.VALUE));
        this.getStats().getStatistics().forEach((k,v)->
                this.getStats().updateStatistic(k, 1, StatisticsUtils.StatisticUpdateType.VALUE));
    }

    @Override
    public void calculateStatisticByItem(Item item, boolean isEquip) {
        super.calculateStatisticByItem(item, isEquip);
        //Note: mercenary always have 100% hp. It's not decreasing after fight.
        this.updateHealthBasedOnMaxHealth();
    }
}
