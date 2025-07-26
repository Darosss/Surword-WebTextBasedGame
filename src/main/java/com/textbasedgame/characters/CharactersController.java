package com.textbasedgame.characters;

import com.textbasedgame.auth.AuthenticationFacade;
import com.textbasedgame.auth.LoggedUserService;
import com.textbasedgame.auth.SecuredRestController;
import com.textbasedgame.characters.equipment.CharacterEquipmentFieldsEnum;
import com.textbasedgame.characters.equipment.Equipment.UseConsumableItemResult;
import com.textbasedgame.characters.equipment.Equipment.EquipItemResult;
import com.textbasedgame.characters.equipment.Equipment.UnEquipItemResult;
import com.textbasedgame.common.ResourceNotFoundException;
import com.textbasedgame.items.*;
import com.textbasedgame.response.CustomResponse;
import com.textbasedgame.settings.AppConfigManager;
import com.textbasedgame.settings.SystemConfig;
import com.textbasedgame.statistics.BaseStatisticsNamesEnum;
import com.textbasedgame.statistics.StatisticsUtils;
import com.textbasedgame.users.User;
import com.textbasedgame.users.UserService;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.users.inventory.InventoryService;
import org.apache.coyote.BadRequestException;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController("characters")
@RequestMapping("characters")
public class CharactersController implements SecuredRestController {
    private final CharacterService service;
    private final ItemService itemService;
    private final AuthenticationFacade authenticationFacade;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final LoggedUserService loggedUserService;
    private final AppConfigManager appConfigManager;
    private final CharacterInventoryService characterInventoryService;

    @Autowired
    public CharactersController(CharacterService characterService, ItemService itemService,
                                UserService userService,
                                AuthenticationFacade authenticationFacade,
                                CharacterInventoryService characterInventoryService,
                                InventoryService inventoryService,
                                LoggedUserService loggedUserService,
                                AppConfigManager appConfigManager) {
        this.service = characterService;
        this.itemService = itemService;
        this.userService = userService;
        this.authenticationFacade = authenticationFacade;
        this.inventoryService = inventoryService;
        this.characterInventoryService = characterInventoryService;
        this.loggedUserService = loggedUserService;
        this.appConfigManager = appConfigManager;
    }

    @GetMapping("/your-mercenaries")
    public CustomResponse<List<MercenaryCharacter>> findYourMercenaries() throws Exception {
        return new CustomResponse<>(HttpStatus.OK,
                this.service.findUserMercenaries(this.authenticationFacade.getJwtTokenPayload().id())
        );
    }
    @GetMapping("/your-main-character")
    public CustomResponse<MainCharacter> findYourMainCharacter() throws Exception {
        Optional<MainCharacter> character = this.service.findMainCharacterByUserId(this.authenticationFacade.getJwtTokenPayload().id());

        return character.map((mainChar)->new CustomResponse<>(HttpStatus.OK, mainChar))
                .orElseThrow(()->new BadRequestException("You do not have main character yet"));
    }
    @GetMapping("/your-characters-ids")
    public CustomResponse<List<String>> getCharactersIds() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        List<String> charactersIds =loggedUser.getCharacters().stream().map((character->character.getId().toString())).toList();
        return new CustomResponse<>(HttpStatus.OK, charactersIds);
    }

    @GetMapping("/user/{userId}/characters-ids")
    public CustomResponse<List<String>> getUserCharactersIds(@PathVariable String userId) throws Exception {
        Optional<User> user = this.userService.findOneById(userId);

        return user.map((userInst)->new CustomResponse<>(HttpStatus.OK, userInst.getCharacters().stream().map((character->character.getId().toString())).toList()))
                .orElseThrow(()->new BadRequestException("User not found"));
    }

    @GetMapping("/{characterId}")
    public CustomResponse<Character> findYourCharacterById(@PathVariable String characterId) throws BadRequestException {
        Optional<Character> character = this.service.findById(characterId);

        return character.map((mainChar)->new CustomResponse<>(HttpStatus.OK, mainChar))
                .orElseThrow(()-> new BadRequestException("Character with id: "+ characterId + " not found"));
    }

    @GetMapping("debug/all")
    public CustomResponse<List<Character>> findAll()  {
        return new CustomResponse<>(HttpStatus.OK, this.service.findAll(Character.class));
    }

    @PostMapping("/create")
    public CustomResponse<Optional<MainCharacter>> createMainCharacter() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        if(this.service.findMainCharacterByUserId(loggedUser.getId().toString()).isPresent()){
            throw new BadRequestException("User already have main character");
        }

        CharacterService.CreateCharacterReturn<MainCharacter> createdCharacterReturn = service.createCharacter(MainCharacter.class, loggedUser, loggedUser.getUsername());

        if(!createdCharacterReturn.success()) throw new BadRequestException(createdCharacterReturn.message());

        return new CustomResponse<>(HttpStatus.CREATED, createdCharacterReturn.message(), createdCharacterReturn.character());
    }

    @PostMapping("/create-mercenary")
    public CustomResponse<Optional<MercenaryCharacter>> createMercenaryCharacter() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);

        Optional<MainCharacter> mainCharacter = this.service.findMainCharacterByUserId(loggedUser.getId().toString());

        if(mainCharacter.isEmpty()) throw new Exception("Something went wrong. Not found main character");

        boolean canCreateMercenary = false;
        String errorMessage = "You have exceed a limit";
        int charactersCount = loggedUser.getCharacters().size();
        for (SystemConfig.MercenaryLimit charsLimit : this.appConfigManager.getSystemConfig().getMercenaryCharacterLimits()) {
            if(charactersCount >= charsLimit.charactersLimit()) continue;
            if(mainCharacter.get().getLevel() >= charsLimit.requiredLevel()) {
                canCreateMercenary = true;
                break;
            }else {
                errorMessage ="You have too low level to create mercenary, need at least: "+charsLimit.requiredLevel();
                break;
            }
        }

        if(!canCreateMercenary) throw new BadRequestException(errorMessage);

        CharacterService.CreateCharacterReturn<MercenaryCharacter> createdCharacterReturn = this.service.createCharacter(MercenaryCharacter.class, loggedUser, "Mercenary "+(charactersCount+1));

        if( !createdCharacterReturn.success()) throw new BadRequestException(createdCharacterReturn.message());
        return new CustomResponse<>(HttpStatus.CREATED, createdCharacterReturn.message(), createdCharacterReturn.character());


    }

    @PostMapping("/use-consumable/{itemId}")
    public CustomResponse<UseConsumableItemResult> useConsumable(
            @PathVariable String itemId
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();

        Optional<MainCharacter> mainCharacter = this.service.findMainCharacterByUserId(loggedUserId);
        Optional<ItemConsumable> itemToUse = this.itemService.findOne(itemId, ItemConsumable.class);

        Inventory inventory = this.inventoryService.getUserInventory(loggedUserId);

        if(itemToUse.isEmpty()) throw new ResourceNotFoundException("Item", itemId);
        if(mainCharacter.isEmpty()) throw new ResourceNotFoundException("Main character does not exist");

        UseConsumableItemResult usedItem = this.characterInventoryService.useConsumableItem(inventory, mainCharacter.get(),  itemToUse.get());
        return new CustomResponse<>(HttpStatus.OK, "Successfully used item", usedItem);
    };
    @PostMapping("/equip/{characterId}/{itemId}/{slot}")
    public CustomResponse<Boolean> equipCharacterItem(
            @PathVariable String characterId,
            @PathVariable String itemId,
            @PathVariable CharacterEquipmentFieldsEnum slot
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();
        Optional<ItemWearable> itemToEquip = this.itemService.findOne(itemId, ItemWearable.class);

        if(itemToEquip.isEmpty()) throw new ResourceNotFoundException("Item", itemId);

        EquipItemResult returnData = this.characterInventoryService.equipItem(
                new ObjectId(loggedUserId),
                new ObjectId(characterId),
                itemToEquip.get(), slot
        );

        if(returnData.success()) return new CustomResponse<>(HttpStatus.OK, returnData.message(), true);

        throw new BadRequestException(returnData.message());
    }

    @PostMapping("/un-equip/{characterId}/{slot}")
    public CustomResponse<Item> unEquipCharacterItem(
            @PathVariable String characterId,
            @PathVariable CharacterEquipmentFieldsEnum slot
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();

        UnEquipItemResult returnData = this.characterInventoryService.unEquipItem(new ObjectId(loggedUserId),  new ObjectId(characterId), slot);
        if(returnData.success() && returnData.item().isPresent()) return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.item().get());

        throw new BadRequestException(returnData.message());
    }


    @PostMapping("/equip-mercenary/{characterId}/{itemId}")
    public CustomResponse<Boolean> equipMercenary(
            @PathVariable String characterId,
            @PathVariable String itemId
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();
        Optional<ItemMercenary> itemToEquip = this.itemService.findOne(itemId, ItemMercenary.class);

        if(itemToEquip.isEmpty()) throw new ResourceNotFoundException("Mercenary item", itemId);

        EquipItemResult returnData = this.characterInventoryService.useMercenaryItemOnMercenaryCharacter(
                new ObjectId(loggedUserId), new ObjectId(characterId), itemToEquip.get());

        if(returnData.success()) return new CustomResponse<>(HttpStatus.OK, returnData.message(), true);

        throw new BadRequestException(returnData.message());
    }
    @PostMapping("/un-equip-mercenary/{characterId}")
    public CustomResponse<Item> unEquipMercenary(
            @PathVariable String characterId
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();

        UnEquipItemResult returnData = this.characterInventoryService.unEquipMercenaryItemFromMercenaryCharacter(
                new ObjectId(loggedUserId), new ObjectId(characterId)
        );
        if(returnData.success() && returnData.item().isPresent()) return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.item().get());

        throw new BadRequestException(returnData.message());

    }
    @PatchMapping("/train-statistic/{statisticName}/{addValue}")
    public CustomResponse<Boolean> trainStatistic(@PathVariable BaseStatisticsNamesEnum statisticName,
                                  @PathVariable int addValue
                                  ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();
        Optional<MainCharacter> foundCharacter = this.service.findMainCharacterByUserId(loggedUserId);

        if(foundCharacter.isEmpty()) throw new ResourceNotFoundException("Main character not found");
        if(!foundCharacter.get().getUser().getId().equals(new ObjectId(loggedUserId)))
            throw new BadRequestException("You can train only your character");

        MainCharacter characterInst = foundCharacter.get();
        characterInst.getStats().increaseStatistic(statisticName, addValue, StatisticsUtils.StatisticUpdateType.VALUE);
        this.service.update(characterInst);
        return new CustomResponse<>(HttpStatus.OK, "Successfully trained statistic", true);
    }

    @PatchMapping("/debug/subtract-statistic/{statisticName}/{subtractValue}")
    public CustomResponse<Boolean> debugSubtractTrainStatistic(@PathVariable BaseStatisticsNamesEnum statisticName,
                                  @PathVariable int subtractValue
    ) throws Exception {
        String loggedUserId = this.authenticationFacade.getJwtTokenPayload().id();
        Optional<MainCharacter> foundCharacter = this.service.findMainCharacterByUserId(loggedUserId);

        if(foundCharacter.isEmpty()) throw new ResourceNotFoundException("Main character not found");
        if(!foundCharacter.get().getUser().getId().equals(new ObjectId(loggedUserId)))
           throw new BadRequestException("You can only subtract stats from your character");

        MainCharacter characterInst = foundCharacter.get();
        characterInst.getStats().decreaseStatistic(statisticName, subtractValue, StatisticsUtils.StatisticUpdateType.VALUE);
        this.service.update(characterInst);
        return new CustomResponse<>(HttpStatus.OK, "Successfully subtracted statistic",true);

    }

    //TODO: remove this endpoint.
    @DeleteMapping("debug/delete-all")
    public CustomResponse<Boolean> deleteAllItems() {
        this.service.removeAllCharactersAndEquipments();

        return new CustomResponse<>(HttpStatus.OK, "[DEBUG]: Successfully removed all characters", true);
    }
}
