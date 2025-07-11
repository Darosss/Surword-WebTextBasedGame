package com.textbasedgame.skirmishes;

import com.textbasedgame.battle.BattleManagerService;
import com.textbasedgame.battle.reports.FightReport;
import com.textbasedgame.characters.Character;
import com.textbasedgame.characters.CharacterService;
import com.textbasedgame.characters.MainCharacter;
import com.textbasedgame.characters.MercenaryCharacter;
import com.textbasedgame.enemies.Enemy;
import com.textbasedgame.enemies.EnemyType;
import com.textbasedgame.enemies.EnemyUtils;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.users.User;
import com.textbasedgame.utils.RandomUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChallengesService {
    private final BattleManagerService battleManagerService;
    private final CharacterService characterService;

    public record CommonReturnData(Skirmish skirmish, FightReport report){}
    public record HandleCurrentChallengeReturn (String message, Optional<CommonReturnData> data){}
    public record HandleDungeonReturn (String message, Optional<CommonReturnData> data, Optional<Dungeons.DungeonData> dungeonData){}

    @Autowired
    public ChallengesService(BattleManagerService battleManagerService, CharacterService characterService) {
        this.battleManagerService = battleManagerService;
        this.characterService = characterService;
    }

    public HandleCurrentChallengeReturn getAndHandleCurrentChallenge(Skirmish skirmish, User user) throws Exception {
        Skirmish.ChosenChallenge chosenChallenge = skirmish.getChosenChallenge();

        //Note: I'm not sure that throwing an error here is good idea. - think later
        if(chosenChallenge == null)
            return new HandleCurrentChallengeReturn("There is no current challenges at all", Optional.empty());

        if(!skirmish.isChallengeTimeCompleted())
            return new HandleCurrentChallengeReturn("Challenge not yet completed", Optional.empty());

        FightReport fightReport = this.completeFinishedChallenge(user, skirmish);
        return new HandleCurrentChallengeReturn(
                "Challenge completed!",
                Optional.of(new CommonReturnData(skirmish, fightReport)));
    }

    public HandleDungeonReturn handleDungeonFight(Skirmish skirmish, User user, int dungeonLevel, int waitTime) throws Exception {
        Dungeons dungeons = skirmish.getDungeons();
        if(!skirmish.getDungeons().canStartFight())
            return new HandleDungeonReturn(
                    "You need to wait till: "+skirmish.getDungeons().getCanFightDate() + " to start a dungeon fight",
                    Optional.empty(), Optional.empty());
        if(dungeonLevel <=0)
            return new HandleDungeonReturn("Dungeon level must be >=1", Optional.empty(), Optional.empty());
        if(dungeonLevel > dungeons.getCurrentLevel())
            return new HandleDungeonReturn(
                    "You can't start a fight with dungeon level higher than current maximum level",
                    Optional.empty(), Optional.empty()
            );

        FightReport fightReport = this.completeStartedDungeonFight(user, dungeonLevel);

        LocalDateTime currentTime = LocalDateTime.now();

        String  responseMessage = "Your dungeon attempt failed";
        //TODO: change to plusMinutes - changed for plusSeconds for debug
        Optional<Dungeons.DungeonData> dungeonData = Optional.empty();
        dungeons.setCanFightDate(currentTime.plusSeconds(waitTime));
        if(fightReport.getStatus().equals(FightReport.FightStatus.PLAYER_WIN)) {
            if (dungeonLevel == dungeons.getCurrentLevel()) {
                dungeonData =Optional.of(new Dungeons.DungeonData(dungeons.getCurrentLevel(), currentTime));
                dungeons.addCompletedDungeon(dungeonData.get());
                dungeons.increaseCurrentLevel();
            }
        }
        return new HandleDungeonReturn(responseMessage,
                Optional.of(new CommonReturnData(skirmish, fightReport)),
                dungeonData
                );
    }


    private MainCharacter getUserMainCharacter(ObjectId userId) throws Exception {
        Optional<MainCharacter> foundCharacter = this.characterService.findMainCharacterByUserId(userId);
        if(foundCharacter.isEmpty()) throw new Exception("Something went wrong - no found character");

        return foundCharacter.get();
    }
    private MainCharacter getUserMainCharacter(String userId) throws Exception {
       return getUserMainCharacter(new ObjectId(userId));
    }

    private FightReport completeFinishedChallenge(User user, Skirmish skirmish) throws Exception {
        Optional<SkirmishData> challenge = skirmish.getChosenChallengeData();

        if(challenge.isEmpty()) return null;

        MainCharacter mainCharacter = this.getUserMainCharacter(user.getId());

        Enemy createdEnemy = this.prepareChallengeEnemy(challenge.get().getDifficulty(), mainCharacter.getLevel());

        FightReport fightReport = this.battleManagerService.performFight(user, mainCharacter, createdEnemy, mainCharacter.getLevel());
        this.handleOnFinishFight(fightReport, mainCharacter, true);

        return fightReport;
    }

    private FightReport completeStartedDungeonFight(User user, int dungeonLevel) throws Exception {
        MainCharacter mainCharacter = this.getUserMainCharacter(user.getId());
        List<MercenaryCharacter> mercenaries = this.characterService.findUserMercenaries(user.getId());
        List<Enemy> createdEnemies = this.prepareDungeonsEnemies(dungeonLevel);

        List<Character> characters = new ArrayList<>(mercenaries);
        characters.add(mainCharacter);

        FightReport fightReport = this.battleManagerService.performTeamFight(user, characters, createdEnemies, mainCharacter.getLevel());

        this.handleOnFinishFight(fightReport, mainCharacter, false);
        return fightReport;
    }

    private void handleOnFinishFight(FightReport report, MainCharacter mainCharacter, boolean updateMainHeroHP){
        MainCharacter.LevelUpLogicReturn leveledUp = mainCharacter.gainExperience(report.getGainedExperience());

        Optional<Integer> hp = updateMainHeroHP ? Optional.of(mainCharacter.getHealth()): leveledUp.gainedLevels() > 0 ? Optional.of(mainCharacter.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.MAX_HEALTH)) : Optional.empty();
        this.characterService.handlePostFightUpdate(mainCharacter.getId().toString(),
                mainCharacter.getExperience(), mainCharacter.getLevel(), hp);
    }


    private Enemy prepareChallengeEnemy(EnemySkirmishDifficulty difficulty, int playerLevel){
        EnemyUtils.LevelRange levelRanges = EnemyUtils.getEnemyLevelRanges(difficulty, playerLevel);
        EnemyType enemyType = EnemyUtils.getEnemyTypeBasedOnSkirmishDifficulty(difficulty);
        return EnemyUtils.createRandomEnemyBasedOnLevel(
                "Challenge enemy "+enemyType , RandomUtils.getRandomValueWithinRange(levelRanges.min(), levelRanges.max()),
                enemyType, EnemyUtils.getEnemyStatsMultiplier(enemyType)
        );
    }
    private List<Enemy> prepareDungeonsEnemies(int dungeonLevel){
        List<Enemy> enemies = new ArrayList<>();

        int countOfEnemies = EnemyUtils.getCountOfDungeonEnemiesBasedOnDungeonLevel(dungeonLevel);
        for(int i = 0; i < countOfEnemies; i++){
            int enemyLevel = RandomUtils.getRandomValueWithinRange(
                    Math.max(1, dungeonLevel - 5), dungeonLevel+5
            );
            EnemyType enemyType = EnemyUtils.getEnemyTypeBasedOnDungeonLevel(dungeonLevel);
            enemies.add(EnemyUtils.createRandomEnemyBasedOnLevel(
                    "Dungeon enemy "+enemyType, enemyLevel,
                    enemyType, EnemyUtils.getEnemyStatsMultiplier(enemyType)
            ));
        }

        return enemies;
    }

}
