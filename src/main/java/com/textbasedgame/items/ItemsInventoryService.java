package com.textbasedgame.items;

import com.textbasedgame.users.User;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.users.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
public class ItemsInventoryService {
    private final InventoryService inventoryService;
    private final ItemService itemService;

    @Autowired
    public ItemsInventoryService(InventoryService inventoryService, ItemService itemService) {
        this.inventoryService = inventoryService;
        this.itemService = itemService;
    }

    @Async
    public CompletableFuture<Boolean> handleOnNewUserItems(User user, List<Item> items){
        Inventory userInventory = this.inventoryService.getUserInventory(user.getId());
        List<Item> createdItems = this.itemService.create(items);
        for(Item item: createdItems) {
            userInventory.addItem(item);
        }
        this.inventoryService.addItemsToInventory(userInventory.getId(),
                createdItems,
                userInventory.getCurrentWeight());
        return CompletableFuture.completedFuture(true);
    }
}


