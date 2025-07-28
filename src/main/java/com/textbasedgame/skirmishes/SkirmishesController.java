package com.textbasedgame.skirmishes;

import com.textbasedgame.auth.AuthenticationFacade;
import com.textbasedgame.auth.LoggedUserService;
import com.textbasedgame.auth.SecuredRestController;
import com.textbasedgame.battle.reports.FightReport;
import com.textbasedgame.characters.MainCharacter;
import com.textbasedgame.items.Item;
import com.textbasedgame.items.ItemsInventoryService;
import com.textbasedgame.response.CustomResponse;
import com.textbasedgame.settings.AppConfigManager;
import com.textbasedgame.statistics.AdditionalStatisticsNamesEnum;
import com.textbasedgame.users.User;
import com.textbasedgame.users.UserService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController("skirmishes")
public class SkirmishesController implements SecuredRestController {
    //TODO: move somewhere - maybe into user or sth
    private final AuthenticationFacade authenticationFacade;
    private final UserService userService;
    private final SkirmishesService service;
    private final ChallengesService challengesService;
    private final ItemsInventoryService itemsInventoryService;
    private final LoggedUserService loggedUserService;
    private final AppConfigManager appConfigManager;


    @Autowired
    public SkirmishesController(AuthenticationFacade authenticationFacade,
                                SkirmishesService skirmishesService,
                                UserService userService, ChallengesService challengesService,
                                ItemsInventoryService itemsInventoryService,
                                LoggedUserService loggedUserService,
                                AppConfigManager appConfigManager) {
        this.authenticationFacade = authenticationFacade;
        this.userService = userService;
        this.service = skirmishesService;
        this.challengesService = challengesService;
        this.itemsInventoryService = itemsInventoryService;
        this.loggedUserService = loggedUserService;
        this.appConfigManager = appConfigManager;
    }

    @GetMapping("/your-skirmishes")
    public CustomResponse<Skirmish> getYourSkirmishes() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);
        return new CustomResponse<>(HttpStatus.OK, this.service.getOrCreateSkirmish(loggedUser, 2));
    }


    @GetMapping("/current-challenge")
    public CustomResponse<FightReport> getChallengeStatus() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        Skirmish foundSkirmish = this.service.getOrCreateSkirmish(loggedUser, 2);

        ChallengesService.HandleCurrentChallengeReturn returnData =
                this.challengesService.getAndHandleCurrentChallenge(foundSkirmish, loggedUser);

        if(returnData.data().isEmpty()) throw new BadRequestException(returnData.message());

        ChallengesService.CommonReturnData data = returnData.data().get();
        //TODO: iterateCount - should be get from user collection(for example some users can have more than 2)
        data.skirmish().generateChallenges(2);
        this.service.updateNewChallenges(data.skirmish().getId(), data.skirmish().getChallenges());

        this.onSuccessFight(loggedUser, data.report().getLoot(), data.report().getGainedGold());

        return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.data().get().report());

    }
    @PostMapping("/start-challenge/{challengeId}")
    public CustomResponse<Boolean> startSkirmishChallenge(
            @PathVariable String challengeId
    ) throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        MainCharacter mainChar =  loggedUser.getMainCharacter().get();
        double minHP = mainChar.getAdditionalStatEffective(AdditionalStatisticsNamesEnum.MAX_HEALTH) * 0.1;
        if(mainChar.getHealth() <= minHP)
            throw new BadRequestException(String.format("To start a skirmish you need at least %f HP", minHP));

        LocalDateTime challengeFinishTimestamp = LocalDateTime.now().plusSeconds(this.appConfigManager.getSkirmishOpts().getChallengeCooldownMS() / 1000);

        Skirmish foundSkirmish = this.service.getOrCreateSkirmish(loggedUser, 2);
        Skirmish.ChosenChallenge chosenChallenge = new Skirmish.ChosenChallenge(challengeId, challengeFinishTimestamp);

        if(foundSkirmish.getChosenChallenge() != null)
            throw new BadRequestException("Wait for a current challenge to finish");



        foundSkirmish.setChosenChallenge(chosenChallenge);
        this.service.updateSetChosenChallenge(foundSkirmish.getId(), chosenChallenge);

        return new CustomResponse<>(HttpStatus.OK, "Challenge successfully started", true);
    }

    @PostMapping("/cancel-current-challenge")
    public CustomResponse<Boolean> cancelCurrentChallenge() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        Skirmish foundSkirmish = this.service.getOrCreateSkirmish(loggedUser, 2);

        if(foundSkirmish.getChosenChallengeData().isEmpty())
            throw new BadRequestException("There is no started challenge at all");

        foundSkirmish.setChosenChallenge(null);
        this.service.updateUnsetChosenChallenge(foundSkirmish.getId());

        return new CustomResponse<>(HttpStatus.OK, "Successfully canceled challenge", true);
    }
    @GetMapping("/dungeons")
    public CustomResponse<Dungeons> getDungeons() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);
        Skirmish skirmish = this.service.getOrCreateSkirmish(loggedUser, 2);

        return new CustomResponse<>(HttpStatus.OK, skirmish.getDungeons());
    }

    @PostMapping("/dungeons/start-a-fight/{dungeonLevel}")
    public CustomResponse<FightReport> startDungeonFight(@PathVariable int dungeonLevel) throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        Skirmish foundSkirmish = this.service.getOrCreateSkirmish(loggedUser, 2);

        ChallengesService.HandleDungeonReturn returnData =
        this.challengesService.handleDungeonFight(
                foundSkirmish, loggedUser,
                dungeonLevel,  this.appConfigManager.getSkirmishOpts().getDungeonCooldownMS() / 1000
        );
        if(returnData.data().isEmpty()) throw new BadRequestException(returnData.message());

        ChallengesService.CommonReturnData data = returnData.data().get();
        Skirmish skirmish = data.skirmish();
        Dungeons dungeon = skirmish.getDungeons();
        if(data.report().getStatus().equals(FightReport.FightStatus.PLAYER_WIN)
                && returnData.dungeonData().isPresent()
        ) {
            this.service.updateOnDungeonSuccessFight(
                    skirmish.getId(),
                    new SkirmishesService.UpdateOnDungeonSuccessFight(
                            dungeon.getCurrentLevel(),
                            dungeon.getCanFightDate(),
                            returnData.dungeonData().get())
            );
        }else {
            this.service.updateOnDungeonFailedFight(
                    skirmish.getId(),
                    new SkirmishesService.UpdateOnDungeonFailedFight(
                            dungeon.getCanFightDate()
                    )
            );
        }
        this.onSuccessFight(loggedUser, data.report().getLoot(), data.report().getGainedGold());
        return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.data().get().report());
    }

    @PostMapping("/debug/generate-new-challenges")
    public CustomResponse<Skirmish> generateNewChallenges() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        return new CustomResponse<>(HttpStatus.OK, this.service.generateNewChallengesForUser(loggedUser, 2));
    }
    @GetMapping("/debug/delete-existing-create-debug-data")
    public CustomResponse<Skirmish> createDebug() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        Optional<Skirmish> foundSkirmish = this.service.findOneByUserId(loggedUser.getId());
        foundSkirmish.ifPresent(skirmish -> this.service.removeById(skirmish.getId()));
        return new CustomResponse<>(HttpStatus.OK, this.service.create(loggedUser, 2));
    }

    //Note: this handles the handleOnNewUserItems -> async function without awaiting by default.
    @Async
    public CompletableFuture<Void> onSuccessFight(User user, List<Item> lootedItems, long gainedGold) {
        this.itemsInventoryService.handleOnNewUserItems(user, lootedItems);

        user.increaseGold(gainedGold);
        this.userService.update(user);
        return CompletableFuture.completedFuture(null);
    }

}
