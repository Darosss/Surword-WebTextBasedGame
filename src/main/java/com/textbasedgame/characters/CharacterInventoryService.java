package com.textbasedgame.characters;

import com.textbasedgame.characters.equipment.CharacterEquipment;
import com.textbasedgame.characters.equipment.CharacterEquipmentFieldsEnum;
import com.textbasedgame.characters.equipment.Equipment.UnEquipItemResult;
import com.textbasedgame.characters.equipment.Equipment.EquipItemResult;
import com.textbasedgame.characters.equipment.Equipment.UseConsumableItemResult;

import com.textbasedgame.items.*;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.utils.TransactionsUtils;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.transactions.MorphiaSession;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CharacterInventoryService {
    private static final Logger logger = LoggerFactory.getLogger(CharacterInventoryService.class);

    private final Datastore datastore;
    //TODO: think about those:
    // - should transactions get only ids, or objects.
    @Autowired
    public CharacterInventoryService(Datastore datastore) {
        this.datastore = datastore;
    }
    public UnEquipItemResult unEquipItem (ObjectId userId, ObjectId characterId, CharacterEquipmentFieldsEnum slot) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();
            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            Character character = TransactionsUtils.fetchCharacter(session, characterId, userId, Character.class);

            if (userInventory == null || character == null)  return new UnEquipItemResult(false,
                    "Cannot find user inventory and/or character. Contact administration", Optional.empty()
            );
            UnEquipItemResult transactionDone = this.handleUnEquipTransaction(session, slot, userInventory, character);
            if (transactionDone.success()) {
                session.commitTransaction();
            } else {
                session.abortTransaction();
            }
            return transactionDone;

        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    private UnEquipItemResult handleUnEquipTransaction(
            MorphiaSession session, CharacterEquipmentFieldsEnum slot,
            Inventory userInventory, Character character
        ){
        CharacterEquipment equipment = character.getEquipment();

        UnEquipItemResult unEquippedItemData = equipment.unEquipItem(slot);

        if (!unEquippedItemData.success() || unEquippedItemData.item().isEmpty()) return unEquippedItemData;

        boolean itemAddedToInv = userInventory.addItem(unEquippedItemData.item().get());

        if(!itemAddedToInv)
            return new UnEquipItemResult(false,
                    "Couldn't remove item and add to inventory. Try again latter" ,
                    Optional.empty()
            );

        this.calculateStatisticsAndSave(session, unEquippedItemData.item().get(), character, userInventory, false);


        return unEquippedItemData;
    }

    public EquipItemResult  equipItem (ObjectId userId, ObjectId characterId, Item item, CharacterEquipmentFieldsEnum slot) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();
            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            Character character = TransactionsUtils.fetchCharacter(session, characterId, userId, Character.class);


            if (userInventory == null || character == null)   return new EquipItemResult(false,
                    "Cannot find user inventory and/or character. Contact administration"
            );

            EquipItemResult transactionDone = this.handleEquipTransaction(session, item, slot, userInventory, character);
            if(transactionDone.success()) {
                session.commitTransaction();
            }else {
                session.abortTransaction();
            }
            return transactionDone;
        }catch(Exception e){
            logger.error("Error occurred in equipItem", e);
            throw new Exception(e.getMessage());
        }
    }

    //TODO: think about item id, item here xd and character
    private EquipItemResult  handleEquipTransaction (
            MorphiaSession session, Item item, CharacterEquipmentFieldsEnum slot,
            Inventory userInventory, Character character
    ){
        Optional<Item> itemToEquip = userInventory.removeItemById(item.getId());

        if (itemToEquip.isEmpty()) return new EquipItemResult(false, "Item does not exist.");

        EquipItemResult equippedData = character.getEquipment().equipItem(slot, item);

        if(equippedData.success()) this.calculateStatisticsAndSave(session, itemToEquip.get(), character, userInventory, true);

        return equippedData;
    }

    public UseConsumableItemResult useConsumableItem(Inventory inventory, Character character, ItemConsumable item) throws Exception {
        if(!item.getType().equals(ItemTypeEnum.CONSUMABLE)) return new UseConsumableItemResult(false, "Item is not consumable item", Optional.empty());
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();


            UseConsumableItemResult result = this.handleUseConsumableTransaction(session, item, character, inventory);
            if(result.success()){
                session.commitTransaction();

            }else {
                session.abortTransaction();
            }
            return result;

        }catch(Exception e){
            logger.error("Error occurred in useConsumableItem", e);
            throw new Exception("Something went wrong when trying to use consumable item");
        }
    }

    private UseConsumableItemResult handleUseConsumableTransaction(MorphiaSession session, ItemConsumable item, Character character, Inventory inventory){
        int hpGain = item.getHpGain();
        if(hpGain > 0) character.increaseHealth(item.getHpGain());
        else character.decreaseHealth(item.getHpGain());
        Optional<Item> usedItem = inventory.removeItemById(item.getId());

        if (usedItem.isEmpty()) return new UseConsumableItemResult(false, "Item does not exist.", Optional.empty());

        try {
            session.find(Character.class)
                    .filter(Filters.eq("_id", character.getId()))
                    .update(
                            new UpdateOptions(),
                            Character.getMorphiaSetCharacterHealth(character.getHealth())
                    );
            session.find(Inventory.class)
                    .filter(Filters.eq("_id", inventory.getId()))
                    .update(
                            new UpdateOptions(),
                            Inventory.getMorphiaUpdateRemoveItems(List.of(usedItem.get().getId()), inventory.getCurrentWeight())
                    );

            return new UseConsumableItemResult(true, "Successfully used item", Optional.of(character.getHealth()));
        }catch(Exception e){
            return new UseConsumableItemResult(false, "Couldn't use item", Optional.empty());
        }
    }

    public EquipItemResult useMercenaryItemOnMercenaryCharacter(ObjectId userId, ObjectId characterId, ItemMercenary item) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();
            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            MercenaryCharacter character = TransactionsUtils.fetchCharacter(session, characterId, userId, MercenaryCharacter.class);

            if (userInventory == null || character == null)   return new EquipItemResult(false,
                    "Cannot find user inventory and/or character. Contact administration"
            );

            EquipItemResult transactionDone = this.handleUseMercenaryItemTransaction(session, item, character, userInventory);
            if(transactionDone.success()) {
                session.commitTransaction();
            }else {
                session.abortTransaction();
            }
            return transactionDone;
        }catch(Exception e){
            logger.error("Error occurred in useMercenaryItemOnMercenaryCharacter", e);
            throw new Exception("Something went wrong when trying to use mercenary item");
        }

    }
    private EquipItemResult handleUseMercenaryItemTransaction(MorphiaSession session, ItemMercenary item, MercenaryCharacter character, Inventory inventory){
        Optional<Item> itemToEquip = inventory.removeItemById(item.getId());
        if (itemToEquip.isEmpty()) return new EquipItemResult(false, "Item does not exist.");
        session.find(Character.class)
                .filter(Filters.eq("_id", character.getId()))
                .update(
                        new UpdateOptions(),
                        Character.getMorphiaSetCharacterStats(character.getStats())
                );
        session.find(Inventory.class)
                .filter(Filters.eq("_id", inventory.getId()))
                .update(
                        new UpdateOptions(),
                        Inventory.getMorphiaUpdateRemoveItems(List.of(itemToEquip.get().getId()), inventory.getCurrentWeight())
                );

        return new EquipItemResult(true, "Successfully used mercenary item");
    }

    public UnEquipItemResult unEquipMercenaryItemFromMercenaryCharacter(ObjectId userId, ObjectId characterId) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();
            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            MercenaryCharacter character = TransactionsUtils.fetchCharacter(session, characterId, userId, MercenaryCharacter.class);
            if (userInventory == null && character == null)
                return new UnEquipItemResult(false,
                        "Cannot find user inventory and/or character. Contact administration",
                        Optional.empty());

            UnEquipItemResult transactionDone = this.handleUnEquipMercenaryItemTransaction(session, character, userInventory);
            if (transactionDone.success()) {
                session.commitTransaction();
            } else {
                session.abortTransaction();
            }
            return transactionDone;


        }catch(Exception e){
            e.printStackTrace();
            throw new Exception("Something went wrong when trying to un equip mercenary item");
        }
    }
    private UnEquipItemResult handleUnEquipMercenaryItemTransaction(MorphiaSession session, MercenaryCharacter character, Inventory inventory){
        ItemMercenary mercenaryItem = character.getMercenary();
        inventory.addItem(mercenaryItem);
        character.setMercenary(null);
        session.find(Character.class)
                .filter(Filters.eq("_id", character.getId()))
                .update(
                        new UpdateOptions(),
                        Character.getMorphiaSetCharacterStats(character.getStats())
                );
        session.find(Inventory.class)
                .filter(Filters.eq("_id", inventory.getId()))
                .update(
                        new UpdateOptions(),
                        Inventory.getMorphiaUpdateAddItems(mercenaryItem, inventory.getCurrentWeight())
                );

        return new UnEquipItemResult(true, "Successfully un equipped mercenary item", Optional.of(mercenaryItem));
    }

    private void calculateStatisticsAndSave(MorphiaSession session, Item item, Character character, Inventory userInventory, boolean isEquip) {
        character.calculateStatisticByItem(item, isEquip);
        session.find(Character.class)
                .filter(Filters.eq("_id", character.getId()))
                .update(
                        new UpdateOptions(),
                        Character.getMorphiaSetCharacterStats(character.getStats())
                );
        session.find(Inventory.class)
                .filter(Filters.eq("_id", userInventory.getId()))
                .update(
                        new UpdateOptions(),
                        isEquip ?
                            Inventory.getMorphiaUpdateRemoveItems(Inventory.getItemsIds(List.of(item)), userInventory.getCurrentWeight()) :
                            Inventory.getMorphiaUpdateAddItems(item, userInventory.getCurrentWeight())
                );

        session.find(CharacterEquipment.class)
                .filter(Filters.eq("character", character.getId()))
                .update(
                    new UpdateOptions(),
                    CharacterEquipment.getMorphiaSetSlots(character.getEquipment().getSlots())
                );


    }
}
