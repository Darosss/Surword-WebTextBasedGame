package com.textbasedgame.characters;

import com.textbasedgame.enemies.EnemyType;
import com.textbasedgame.settings.XpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class XpUtils {

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(XpUtils.class);

    public record EncounterContext(int playerLevel, int enemyLevel, EnemyType type, boolean survived) {}


    public static long calculateExperienceFromEnemy(EncounterContext ctx, XpConfig config, double additionalMultiplier) {

        long baseExperience = config.getEnemyDefeatBaseXp();
        int levelDifference = ctx.enemyLevel - ctx.playerLevel;
        double enemyDifferenceFactor = levelDifference >= 0 ? Math.max(1, levelDifference + (0.2 * levelDifference) ) :( 1.0 + ( (double) levelDifference / 5.0) );

        double enemyTypeBonus = ctx.type.getBonusExperience();


        double bonusExpForDifferenceLevel = (baseExperience * enemyDifferenceFactor) + (enemyTypeBonus * baseExperience / 2);
        double scalingFactor = Math.max(1,Math.pow(ctx.enemyLevel+(config.getFactorBaseXp() * ctx.enemyLevel), config.getEnemyDefeatFactorExponentXp()));

        long experience = (long) Math.max(1, ((baseExperience * scalingFactor * enemyTypeBonus) + bonusExpForDifferenceLevel));

        logger.debug("Scaling factor: {}, experience: {}, enemyLevel: {}, playerLevel: {}, enemyDifferenceFactor: {}, bonusExpForDifferenceLevel: {}, levelDifference: {}, additionalMultiplier: {}",
                scalingFactor, experience, ctx.enemyLevel, ctx.playerLevel, enemyDifferenceFactor, bonusExpForDifferenceLevel, levelDifference, additionalMultiplier);

        long baseXp = (ctx.survived)
                ? (long) Math.max(1, experience + (experience * config.getEnemyDefeatSurvivedAdjustXp()))
                : experience;
        return (long)(baseXp * additionalMultiplier);
    }

    public static long calculateExpToNextLevel(int currentLevel, XpConfig config) {

        double levelScalingFactor = currentLevel / config.getScalingFactorXp();
        double levelFactor = Math.pow(
                currentLevel+(config.getFactorBaseXp()*currentLevel),
                (config.getFactorExponentXp() + levelScalingFactor)
        );
        long neededExp = (long) (config.getBasePerLevelXp() * levelFactor);
        logger.debug("Needed: {} exp for {} level ", neededExp, currentLevel);

        return neededExp;
    }

}
