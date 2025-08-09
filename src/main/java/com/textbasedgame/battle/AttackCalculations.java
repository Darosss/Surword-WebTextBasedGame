package com.textbasedgame.battle;

import com.textbasedgame.battle.data.AttackBase;
import com.textbasedgame.battle.data.AttackDebuffs;
import com.textbasedgame.battle.data.AttackReturnData;
import com.textbasedgame.characters.BaseHero;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class AttackCalculations {
    private static final Logger logger = LoggerFactory.getLogger(AttackCalculations.class);

    private static final double CRITIC_SCALING_FACTOR = 0.05;
    private static final double LETHAL_CRITIC_SCALING_FACTOR = 1.1;



    private static AttackBase.AttackStrengthWithBonusDamage getCalculatedAttackStrength(BaseHero hero){
        //TODO: add additional statistics (Critic damage, lethal critic damage - with base like 15%, 30%)
        if(isLethalCritic(hero)) return new AttackBase.AttackStrengthWithBonusDamage(AttackBase.AttackStrength.LETHAL, 30);
        if(isCritic(hero)) return new AttackBase.AttackStrengthWithBonusDamage(AttackBase.AttackStrength.CRITIC, 15);
        else return new AttackBase.AttackStrengthWithBonusDamage(AttackBase.AttackStrength.NORMAL, 0);
    }

    private static double calculateCriticChance(BaseHero hero){
        int criticValue = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.CRITIC);
        int bonusCriticPerLevel = Math.max(1, hero.getLevel() - 20);
        double criticChance =(criticValue + ((double) hero.getLevel() / bonusCriticPerLevel)) / (1 + CRITIC_SCALING_FACTOR * hero.getLevel());
        return Math.min(criticChance, 50);
    }
    private static boolean isCritic(BaseHero hero) {
        double criticChance = calculateCriticChance(hero);
        logger.debug("Critic chance: {}", criticChance);
        return RandomUtils.checkPercentageChance(criticChance);
    }

    private static double calculateLethalCriticChance(BaseHero hero){
        int criticValue = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.LETHAL_CRITIC);
        int bonusCriticPerLevel = Math.max(1, hero.getLevel() - 20);

        double lethalChance = (criticValue + ((double) hero.getLevel() / bonusCriticPerLevel)) / (1 + LETHAL_CRITIC_SCALING_FACTOR * hero.getLevel());
        return Math.min(lethalChance, 25);
    }
    private static boolean isLethalCritic(BaseHero hero) {
        double lethalCriticChance = calculateLethalCriticChance(hero);
        logger.debug("Lethal critic chance: {}", lethalCriticChance);
        return RandomUtils.checkPercentageChance(lethalCriticChance);
    }

    private static int getCalculatedAttackValue(BaseHero hero, double criticBonus ) {
        int minDmg = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.MIN_DAMAGE);
        int maxDmg = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.MAX_DAMAGE);

        double minDmgWithBonus = (minDmg + (minDmg * (criticBonus / 100)));
        double maxDmgWithBonus = (maxDmg + (maxDmg * (criticBonus / 100)));
        return (int) RandomUtils.getRandomValueWithinRange(minDmgWithBonus, maxDmgWithBonus);
    }

    private static boolean isDoubledAttack(BaseHero hero) {
        int doubleAttackChance = hero.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.DOUBLE_ATTACK);
        return RandomUtils.checkPercentageChance(doubleAttackChance);
    }

    private static AttackBase getAttackBaseValues(BaseHero hero) {
        AttackBase.AttackStrengthWithBonusDamage attackStrength = getCalculatedAttackStrength(hero);
        int attackValue = getCalculatedAttackValue(hero, attackStrength.percentBonusDamage());

        return new AttackBase(attackValue, attackStrength);
    }

    //TODO: add debuffs
    @Nullable
    private static AttackDebuffs getAttackDebuffs() {
        return null;
    }

    private int getAndCalculateAdditionalAttackValues() {
        return 0;
    }

    public static AttackReturnData generateAttackValue(BaseHero hero, boolean doubleAttackPossible/*TODO: add some optionals from fight?*/){
        AttackBase attackBase = getAttackBaseValues(hero);
        boolean doubledAttack = doubleAttackPossible && isDoubledAttack(hero);
        AttackDebuffs attackDebuffs = getAttackDebuffs();
        return new AttackReturnData(hero.getId().toHexString(), attackBase, doubledAttack, attackDebuffs);
    }



}
