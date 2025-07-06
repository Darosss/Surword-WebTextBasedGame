package com.textbasedgame.users.inventory;

import com.textbasedgame.items.Item;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class InventoryService {
    private final Datastore datastore;

    @Autowired
    public InventoryService(Datastore datastore) {
        this.datastore = datastore;
    }

    public Inventory create(Inventory inventory) {
        return this.datastore.save(inventory);
    }

    public Inventory update(Inventory inventory) {
        return this.datastore.save(inventory);
    }

    public void addItemsToInventory(ObjectId inventoryId, Map<String, Item> newItems, Float newWeight) {
        datastore.find(Inventory.class)
                .filter(Filters.eq("_id", inventoryId)).update(
                        new UpdateOptions(),
                        UpdateOperators.set("items", newItems),
                        UpdateOperators.set("currentWeight", newWeight)
                );
    }

    public Inventory getUserInventory(ObjectId userId) {
        return this.datastore.find(Inventory.class).filter(Filters.eq("user", userId)).first();
    }
    public Inventory getUserInventory(String userId) {
        return this.getUserInventory(new ObjectId(userId));
    }

}
