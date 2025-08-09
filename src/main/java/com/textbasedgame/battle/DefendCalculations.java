package com.textbasedgame.battle;

import com.textbasedgame.battle.data.AttackReturnData;
import com.textbasedgame.battle.data.DefendReturnData;
import com.textbasedgame.characters.BaseHero;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DefendCalculations {
    private static final Logger logger = LoggerFactory.getLogger(DefendCalculations.class);

    private static final int MAX_DODGE_CHANCE = 50;
    private static final int MAX_PARRY_CHANCE = 50;
    private static final int MAX_BLOCK_CHANCE = 50;

    private static int getCalculatedArmorAbsorption(BaseHero hero, int rawAttackValue) {
        int armor = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.ARMOR);

        //TODO: add proper armor factors
        return (int) (rawAttackValue * 0.5);
    }

    private static boolean isBlocked(BaseHero hero){
        int blockChance = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.BLOCK);
        return RandomUtils.checkPercentageChance(Math.min(blockChance, MAX_BLOCK_CHANCE));
    }
    private static boolean isDodged(BaseHero hero){
        int dodgeChance = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.DODGE);
        return RandomUtils.checkPercentageChance(Math.min(dodgeChance, MAX_DODGE_CHANCE));
    }
    private static boolean isPaired(BaseHero hero){
        int parryChance = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.PARRYING);
        return RandomUtils.checkPercentageChance(Math.min(parryChance, MAX_PARRY_CHANCE));
    }

    private static DefendReturnData.DefendType getDefendType(BaseHero hero) {
        if(isDodged(hero)) return DefendReturnData.DefendType.DODGED;
        else if(isPaired(hero)) return DefendReturnData.DefendType.PAIRED;
        else if(isBlocked(hero)) return DefendReturnData.DefendType.BLOCKED;
        else return DefendReturnData.DefendType.NULL;
    }
    private static DefendReturnData.DefendType getDefendTypeWithoutParried(BaseHero hero) {
        DefendReturnData.DefendType defType = getDefendType(hero);
        if(defType.equals(DefendReturnData.DefendType.PAIRED)) return DefendReturnData.DefendType.NULL;

        return defType;
    }

    private static CombatReturnData executeParryAttack(BaseHero hero, BaseHero pairedHero) {
        //TODO: remove those logger debug later
        logger.debug("Execute parry attack");
        logger.debug("[DEFEND] {} PAIRED attack made by {}, left with: {} hp", hero.getName(), pairedHero.getName(), hero.getHealth());
        AttackReturnData attackData = AttackCalculations.generateAttackValue(hero, false);

        DefendReturnData pairedHeroDefendData = defend(pairedHero, hero, attackData, false);

        return new CombatReturnData(new CombatReturnData.AttackDefendData(attackData, pairedHeroDefendData), null);
    }

    public static DefendReturnData defend(BaseHero defender, BaseHero attacker, AttackReturnData attackData, boolean isParryPossible){
        DefendReturnData.DefendType defendType = isParryPossible ? getDefendType(defender) : getDefendTypeWithoutParried(defender);

        Optional<CombatReturnData> parriedData = Optional.empty();
       int effectiveDamage = switch (defendType){
            case PAIRED -> {
                 CombatReturnData data = executeParryAttack(defender, attacker);
                 parriedData = Optional.of(data);
                 yield 0;
            }
           case BLOCKED,DODGED -> 0;
           case NULL-> {
               int effDamage = getCalculatedArmorAbsorption(defender, attackData.baseValues().value());
               defender.decreaseHealth(effDamage);
               yield effDamage;
          }
        };

        //TODO: remove those logger debug later
        if(defendType.equals(DefendReturnData.DefendType.BLOCKED)) {
            logger.debug("[DEFEND] {} BLOCKED attack made by: {}, left with [{}] hp", defender.getName(), attacker.getName(), defender.getHealth());
        }else if(defendType.equals(DefendReturnData.DefendType.DODGED)){
            logger.debug("[DEFEND] {} DODGED attack made by: {}, left with [{}] hp", defender.getName(), attacker.getName(), defender.getHealth());
        }else if(defendType.equals(DefendReturnData.DefendType.NULL)){
            logger.debug("[DEFEND] {} RECEIVED: {} damage from attack made by: {}, left with [{}] hp", defender.getName(), effectiveDamage, attacker.getName(), defender.getHealth());
        }

        return new DefendReturnData(defender.getId().toHexString(), effectiveDamage, defender.getHealth(), defendType, parriedData);
    }


}
