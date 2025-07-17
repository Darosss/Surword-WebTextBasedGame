package com.textbasedgame.items;

import com.textbasedgame.auth.AuthenticationFacade;
import com.textbasedgame.auth.LoggedUserService;
import com.textbasedgame.auth.SecuredRestController;
import com.textbasedgame.response.CustomResponse;
import com.textbasedgame.settings.LootService;
import com.textbasedgame.users.User;
import com.textbasedgame.users.UserService;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.users.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

//CONTROLLER FOR DEBUGGING
//EASILY ADD RANDOM ITEMS
//ALL METHODS PUBLIC FOR EASY DEBUG ACCESS
@RestController
public class ItemDebugDataGeneratorController implements SecuredRestController {
    private final ItemService service;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final AuthenticationFacade authenticationFacade;
    private final LoggedUserService loggedUserService;
    private final LootService lootService;

    @Autowired
    public ItemDebugDataGeneratorController(ItemService service, UserService userService,
                                            InventoryService inventoryService, AuthenticationFacade authenticationFacade,
                                            LoggedUserService loggedUserService, LootService lootService) {
        this.service = service;
        this.userService = userService;
        this.inventoryService = inventoryService;
        this.authenticationFacade = authenticationFacade;
        this.loggedUserService = loggedUserService;
        this.lootService = lootService;
    }

    @GetMapping("items/")
    public CustomResponse<List<Item>> getItems(){
        return new CustomResponse<>(HttpStatus.OK, service.findAll());
    }

    @PostMapping("items/debug/create-with-random-data/{countOfItems}/")
    public CustomResponse<List<Item>> generateItemWithRandomData(@PathVariable int countOfItems) throws Exception {
        User loggedUser = loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);
        Inventory inventory = this.inventoryService.getUserInventory(loggedUser.getId());
        List<Item> items = this.service.create(ItemUtils.generateRandomItems(loggedUser, countOfItems, lootService.getCurrentRaritiesBonuses()));
        for (Item item : items) {
            inventory.addItem(item);
        }
        this.inventoryService.update(inventory);

        return new CustomResponse<>(HttpStatus.OK, items);
    }

    @PostMapping("items/debug/createItem/{level}/{type}")
    public CustomResponse<Item> generateItemWithLevelAndType(
            @PathVariable int level,
            @PathVariable ItemTypeEnum type
    ) throws Exception {
        User loggedUser = loggedUserService.getLoggedUserDetails(this.authenticationFacade, this.userService);
        Inventory inventory = this.inventoryService.getUserInventory(loggedUser.getId());
        Item item = service.create(ItemUtils.generateRandomItemWithoutBaseStats(
                loggedUser, level, type, this.lootService.getCurrentRaritiesBonuses(), Optional.empty()));
        inventory.addItem(item);
        this.inventoryService.update(inventory);

        return new CustomResponse<>(HttpStatus.OK, item);
    }

    @DeleteMapping("items/debug/delete-all")
    public void deleteAllItems() {
        this.service.removeAllItems();
    }
}
