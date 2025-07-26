package com.textbasedgame.battle;

import com.textbasedgame.battle.reports.FightReport;
import com.textbasedgame.battle.reports.FightTurnReport;
import com.textbasedgame.battle.data.AttackReturnData;
import com.textbasedgame.characters.BaseHero;
import com.textbasedgame.characters.Character;
import com.textbasedgame.battle.data.DefendReturnData;
import com.textbasedgame.characters.XpUtils;
import com.textbasedgame.enemies.Enemy;
import com.textbasedgame.enemies.EnemyUtils;
import com.textbasedgame.items.Item;
import com.textbasedgame.settings.LootService;
import com.textbasedgame.settings.XpService;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.users.User;
import com.textbasedgame.utils.GoldService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Fight {
    private static final Logger logger = LoggerFactory.getLogger(Fight.class);
    //TODO: later add fight options(bonuses) - for example: loot multiplier from fight, exp multiplier etc. etc;

    public final XpService xpService;
    public final LootService lootService;
    public final GoldService goldService;

    private final int maxTurns;
    private final int baseInitiativePerCycle = 20;
    private final int minimumBaseCapInitiative = 50;
    private int baseMinimumInitiativeForTurn = 50;
    private final FightReport fightReport = new FightReport();
    private final int mainHeroLevel;
    private final Map<ObjectId, BattleDetails<Character>> userHeroesDetails;
    private final Map<ObjectId, BattleDetails<Enemy>> enemyHeroesDetails;

    private final boolean mustKillEnemyToWin;

    private final User user;
    private final List<ObjectId> turnParticipants = new ArrayList<>();

    Fight(XpService xpService,
          LootService lootService,
          GoldService goldService,
          User user, List<Character> characters, List<Enemy> enemies, int maxTurns, int mainHeroLevel, boolean mustKillEnemyToWin) {

        this.calculateMinimumInitiativeOfLevelsMean(
                IntStream.concat(
                        characters.stream().mapToInt(Character::getLevel),
                        enemies.stream().mapToInt(Enemy::getLevel)
                ).toArray()
        );
        this.xpService = xpService;
        this.lootService = lootService;
        this.goldService = goldService;
        this.user = user;
        this.userHeroesDetails = this.prepareUserHeroesDetails(characters);
        this.enemyHeroesDetails = this.prepareEnemiesHeroesDetails(enemies);
        this.maxTurns = maxTurns;
        this.mainHeroLevel = mainHeroLevel;
        this.mustKillEnemyToWin = mustKillEnemyToWin;
        this.fightReport.addInitialParticipantsStatistics(
            Stream.concat(
                    characters.stream().map(Character::getId),
                    enemies.stream().map(BaseHero::getId)
            ).toList());
    }

    Fight(XpService xpService, LootService lootService, GoldService goldService, User user, List<Character> characters, List<Enemy> enemies, int mainHeroLevel, boolean mustKillEnemyToWin){
        this(xpService, lootService, goldService, user, characters, enemies, 50, mainHeroLevel, mustKillEnemyToWin);
    }

    private void calculateMinimumInitiativeOfLevelsMean(int[] levels) {
        List<Integer> results = new ArrayList<>();
        for (int levelVal : levels) {
            int result = (int) (levelVal * 1.2 * 1.8);
            results.add(result);
        }
        int value = results.stream().mapToInt(Integer::intValue).sum();

        this.baseMinimumInitiativeForTurn = Math.max(value, minimumBaseCapInitiative);
    }

    private Map<ObjectId, BattleDetails<Character>>  prepareUserHeroesDetails(List<Character> characters){
        Map<ObjectId, BattleDetails<Character>> heroesDetailsMap = new HashMap<>();
        characters.forEach((charVal) -> {
            heroesDetailsMap.put(charVal.getId(),
                    new BattleDetails<>(charVal, baseInitiativePerCycle, true));
        });
        return heroesDetailsMap;
    }

    private Map<ObjectId, BattleDetails<Enemy>>  prepareEnemiesHeroesDetails(List<Enemy> enemies){
        Map<ObjectId, BattleDetails<Enemy>> heroesDetailsMap = new HashMap<>();
        enemies.forEach((charVal) -> {
            heroesDetailsMap.put(charVal.getId(),
                    new BattleDetails<>(charVal, baseInitiativePerCycle, false));
        });
        return heroesDetailsMap;
    }

    //TODO: make it DRY later
    private BaseHero getMostThreateningUserHero () {
        return this.userHeroesDetails.values().stream()
                .map(BattleDetails::getHero)
                .reduce((hero1, hero2) ->
                        hero1.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.THREAT) >
                                hero2.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.THREAT)? hero1 : hero2)
                .get();
    }
    //TODO: make it DRY later
    private BaseHero getMostThreateningEnemyHero () {
        return this.enemyHeroesDetails.values().stream()
                .map(BattleDetails::getHero)
                .reduce((hero1, hero2) ->
                        hero1.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.THREAT) >
                                hero2.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.THREAT)? hero1 : hero2)
                .get();
    }

    private FightReport.FightStatus checkAndSetFightStatus() {
        long enemiesCount = this.enemyHeroesDetails.size();
        if(enemiesCount == 0) this.fightReport.setStatus(FightReport.FightStatus.PLAYER_WIN);


        long userCharactersCount =  this.userHeroesDetails.size();
        if(userCharactersCount == 0) this.fightReport.setStatus(FightReport.FightStatus.ENEMY_WIN);


        return this.fightReport.getStatus();
        //TODO: condition for draw?
    }

    private boolean isFightOngoing() {
        return this.checkAndSetFightStatus().equals(FightReport.FightStatus.FIGHT);
    }

    private boolean hasFightAvailableLeftTurns(int currentTurn) {
        if(currentTurn< this.maxTurns) return true;

        FightReport.FightStatus fightStatusEnd = this.mustKillEnemyToWin ?
                FightReport.FightStatus.ENEMY_WIN : this.getStatusFightDependsOnDamageDone();


        if(fightStatusEnd.equals(FightReport.FightStatus.PLAYER_WIN)) {
            this.enemyHeroesDetails.values().forEach((hero)->{
                Enemy enemy = (Enemy) hero.getHero();
                this.fightReport.increaseGainedExperience(
                        this.xpService.awardXpFromEnemy(new XpUtils.EncounterContext(this.mainHeroLevel, enemy.getLevel(), enemy.getType(), true))
                );
                this.fightReport.increaseGainedGold(
                        this.goldService.calculateGoldFromEnemy(enemy.getLevel(), enemy.getType(), true)
                );
                List<Item> loot = EnemyUtils.checkLootFromEnemy(
                            new EnemyUtils.CheckLootFromEnemyBaseContext(this.user, enemy.getType(), enemy.getLevel(), true),
                            this.lootService.getCurrentLootConfig()
                        );
                this.fightReport.addLootItems(loot);
            });
        }

        this.handleEndOfFight();
        this.fightReport.setStatus(fightStatusEnd);
        return false;
    }

    private void handleEndOfFight() {
        this.fightReport.setLastTurnToEndOfFight();

        //At end of fight add lastly living heroes
        this.userHeroesDetails.values().forEach((v)->this.fightReport.addToCharacters((Character) v.getHero()));
        this.enemyHeroesDetails.values().forEach((v)->this.fightReport.addToEnemies((Enemy) v.getHero()));
        logger.debug("End of fight!");

    }

    private  <DetailsType extends BaseHero> void handleCycleInitiatives(Map<ObjectId, BattleDetails<DetailsType>> detailsMap) {
        detailsMap.forEach((k, v)->{
            v.addCycleValueToCurrentInitiative();
            while(v.getCurrentInitiative() > this.baseMinimumInitiativeForTurn){
                this.turnParticipants.add(k);
                v.useInitiativePoints(this.baseMinimumInitiativeForTurn);
            }
        });
    }


    private int findLowestInitiativePerCycle(){
        int lowestInitiativeUserCharacters = this.userHeroesDetails.values().stream().map(BattleDetails::getInitiativePerCycle).sorted((a, b)->a - b).toList().get(0);
        int lowestInitiativeEnemiesCharacters = this.enemyHeroesDetails.values().stream().map(BattleDetails::getInitiativePerCycle).sorted((a, b)->a - b).toList().get(0);
        return Math.min(lowestInitiativeUserCharacters, lowestInitiativeEnemiesCharacters);

    }



    public void startFight(){
        int cyclesForTurn = (int) Math.ceil((double) this.baseMinimumInitiativeForTurn / this.findLowestInitiativePerCycle());
        int turnCount = 0;
        int cycleCounter = 0;
        //while health enemy and player > 0 && turnCount < 100 - exclude infinite
        while (this.isFightOngoing() && this.hasFightAvailableLeftTurns(turnCount)){
            this.handleCycleInitiatives(this.userHeroesDetails);
            this.handleCycleInitiatives(this.enemyHeroesDetails);

            if(!this.turnParticipants.isEmpty() && cycleCounter >= cyclesForTurn - 1){
                FightTurnReport turnReport = new FightTurnReport(turnCount);
                //TODO: known bug where hero health =1/0
                // when dies so fast its throwing an error
                logger.debug("************* {} TURN STARTED ************* ", turnCount+1);

                turnCount ++;
                cycleCounter = 0;
                for(ObjectId id: this.turnParticipants){
                    if(this.userHeroesDetails.containsKey(id)){
                        //TODO: improve here, make DRY
                        BattleDetails<Character> currentHeroDetails = this.userHeroesDetails.get(id);
                        this.handleHeroTurn(currentHeroDetails, turnReport, true);

                    }else if(this.enemyHeroesDetails.containsKey(id)){
                        //TODO: improve here, make DRY
                        BattleDetails<Enemy> currentHeroDetails = this.enemyHeroesDetails.get(id);
                        this.handleHeroTurn(currentHeroDetails, turnReport, false);
                    }

                    if(!this.isFightOngoing()){
                        //true == end of fight
                        this.fightReport.addTurnReport(turnReport);
                        this.handleEndOfFight();
                        return;
                    }
                };
                turnParticipants.clear();
                this.fightReport.addTurnReport(turnReport);
            }else{
                cycleCounter++;
            }
        }
    }



    private <DetailsType extends BaseHero> void handleHeroTurn(BattleDetails<DetailsType> heroDetails, FightTurnReport currentTurnReport, boolean isUserTurn) {
        //TODO: logs to remove
        if(heroDetails.isUserCharacter()) logger.debug("** User turn ** - {} : ({})", heroDetails.getHero().getName(), heroDetails.getHero().getId());
        else logger.debug("** Enemy turn ** - {} : ({})", heroDetails.getHero().getName(), heroDetails.getHero().getId());

        BaseHero heroToAttack = isUserTurn ? getMostThreateningEnemyHero() : getMostThreateningUserHero();
        CombatReturnData turnData = this.attackAndDefend(heroDetails.getHero(),
                heroToAttack);

        this.checkAndHandleDeaths(isUserTurn, heroDetails.getHero().getId(), heroToAttack.getId());
        this.fightReport.increaseStatisticsBasedOnTurn(
                heroDetails.getHero().getId(), heroToAttack.getId(), turnData);
        currentTurnReport.addTurnAction(turnData);
    }

    private void checkAndHandleUserCharacterDeath(ObjectId heroId) {
        Character character = (Character) this.userHeroesDetails.get(heroId).getHero();
        if(character.getHealth() <= 0){
            this.fightReport.addToCharacters(character);
            this.userHeroesDetails.remove(heroId);

        }
    }
    private void checkAndHandleEnemyDeath(ObjectId heroId) {
        Enemy enemy = (Enemy) this.enemyHeroesDetails.get(heroId).getHero();
        if(enemy.getHealth() <= 0){
            this.fightReport.increaseGainedExperience(
                    this.xpService.awardXpFromEnemy(new XpUtils.EncounterContext(this.mainHeroLevel, enemy.getLevel(), enemy.getType(), false))
                    );
            this.fightReport.increaseGainedGold(
                    this.goldService.calculateGoldFromEnemy(enemy.getLevel(), enemy.getType(), false)
            );

            List<Item> loot = EnemyUtils.checkLootFromEnemy(
                    new EnemyUtils.CheckLootFromEnemyBaseContext(this.user, enemy.getType(), enemy.getLevel(), false),
                    this.lootService.getCurrentLootConfig()
            );
            this.fightReport.addLootItems(loot);

            this.fightReport.addToEnemies(enemy);
            this.enemyHeroesDetails.remove(heroId);

        }
    }


    public void checkAndHandleDeaths(boolean isUserTurn, ObjectId attackerId, ObjectId defenderId) {
        if (isUserTurn) {
            checkAndHandleUserCharacterDeath(attackerId);
            checkAndHandleEnemyDeath(defenderId);
        } else {
            checkAndHandleUserCharacterDeath(defenderId);
            checkAndHandleEnemyDeath(attackerId);
        }
    }

    private CombatReturnData attackAndDefend (BaseHero attacker, BaseHero defender){
        AttackReturnData attackData = AttackCalculations.generateAttackValue(attacker, true);
        DefendReturnData defendData = DefendCalculations.defend(defender, attacker, attackData, true);

        CombatReturnData.AttackDefendData doubledAttackData = attackData.withDoubledAttack() ?
                new CombatReturnData.AttackDefendData(
                        AttackCalculations.generateAttackValue(attacker, false),
                        DefendCalculations.defend(defender, attacker, attackData, false)
                ) : null;

        return new CombatReturnData(new CombatReturnData.AttackDefendData(attackData, defendData),
                Optional.ofNullable(doubledAttackData));

    }


    private FightReport.FightStatus getStatusFightDependsOnDamageDone() {
      Optional<ObjectId> participantId = this.fightReport.findParticipantIdByHighestDamageDone();


      if(participantId.isPresent() && this.userHeroesDetails.containsKey(participantId.get())) {
          return FightReport.FightStatus.PLAYER_WIN;
      }




      return FightReport.FightStatus.ENEMY_WIN;
    }
    public FightReport getFightReport() {
        return this.fightReport;
    }
}



