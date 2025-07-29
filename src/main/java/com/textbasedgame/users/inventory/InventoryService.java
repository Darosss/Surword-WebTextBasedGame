package com.textbasedgame.users.inventory;
import com.textbasedgame.items.Item;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filters;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

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

    public void addItemsToInventory(ObjectId inventoryId, List<Item> newItems, Float newWeight) {
        datastore.find(Inventory.class)
            .filter(Filters.eq("_id", inventoryId))
            .update(
                    new UpdateOptions(),
                    Inventory.getMorphiaUpdateAddItems(newItems, newWeight)
            );

    }

    public Inventory getUserInventory(ObjectId userId) {
        return this.datastore.find(Inventory.class)
                .filter(Filters.eq("user", userId))
                .iterator(new FindOptions().projection().exclude("user")).tryNext();
    }
    public Inventory getUserInventory(String userId) {
        return this.getUserInventory(new ObjectId(userId));
    }

}
