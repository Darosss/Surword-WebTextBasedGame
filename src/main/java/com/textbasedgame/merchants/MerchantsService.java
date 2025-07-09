package com.textbasedgame.merchants;

import com.textbasedgame.items.Item;
import com.textbasedgame.users.User;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.utils.TransactionsUtils;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.transactions.MorphiaSession;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class MerchantsService {

    private final Datastore datastore;

    @Autowired
    public MerchantsService(Datastore datastore) {
        this.datastore = datastore;
    }

    public record MerchantActionReturn(boolean success, Optional<Merchant.MerchantTransaction> transaction, String message){}

    public Optional<Merchant> findMerchantByUserId(ObjectId userId){
        return Optional.ofNullable(datastore.find(Merchant.class).filter(
                Filters.eq("user", userId)
        ).first());
    }
    public Optional<Merchant> findMerchantByUserId(String userId){
        return findMerchantByUserId(new ObjectId(userId));
    }
    public Merchant create(User user, List<Item> items) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();

            Merchant createdMerchant = session.save(
                    new Merchant(user, TransactionsUtils.handleCreatingNewItems(session, items)));
            session.commitTransaction();

            return createdMerchant;
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    };
    public MerchantActionReturn buyItemFromMerchant(String userId, String itemId) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();


            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            Merchant userMerchant = TransactionsUtils.fetchUserMerchant(session, userId);
            User user = TransactionsUtils.fetchUser(session, userId);
            if(userInventory == null || userMerchant == null || user == null)
                return new MerchantActionReturn(false, Optional.empty(),
                        "Cannot find inventory or user or merchant data. Contact administration");


            Merchant.MerchantTransaction boughItemData = userMerchant.buyItemByItemId(itemId);
            if(boughItemData.item().isEmpty())
                return new MerchantActionReturn(false, Optional.empty(), "This item does not exist in merchant commodity");

            if(boughItemData.cost() > user.getGold())
                return new MerchantActionReturn(false, Optional.empty(), "You do not have enough gold");

            userInventory.addItem(boughItemData.item().get());
            user.decreaseGold(boughItemData.cost());
            handleMerchantActionsUpdatesTransaction(session, user.getId(), userInventory, boughItemData, false);

            session.commitTransaction();

            return new MerchantActionReturn(true, Optional.of(boughItemData), "Successfully bought item");
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    public MerchantActionReturn sellItemToMerchant(String userId, String itemId) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();

            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            Merchant userMerchant = TransactionsUtils.fetchUserMerchant(session, userId);
            User user = TransactionsUtils.fetchUser(session, userId);

            if(userInventory == null || userMerchant == null || user == null)
                return new MerchantActionReturn(false, Optional.empty(),
                        "Cannot find inventory or user or merchant data. Contact administration");


            Optional<Item> item = userInventory.getItemById(itemId);
            if(item.isEmpty()) return new MerchantActionReturn(false, Optional.empty(),
                    "This item does not exist in your inventory");


            Merchant.MerchantTransaction sellItemData = userMerchant.sellItem(item.get());
            userInventory.removeItemById(itemId);
            user.increaseGold(sellItemData.cost());
            handleMerchantActionsUpdatesTransaction(session, user.getId(), userInventory, sellItemData, true);

            session.commitTransaction();

            return new MerchantActionReturn(true, Optional.of(sellItemData), "Successfully sold item");
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    public Merchant update(Merchant merchant) {
        return datastore.save(merchant);
    }


    public void handleMerchantActionsUpdatesTransaction(
            MorphiaSession session, ObjectId userId, Inventory userInventory, Merchant.MerchantTransaction merchantTransaction, boolean isSell
    ) {
        Optional<Item> item = merchantTransaction.item();
        if(item.isEmpty()) return;

        session.find(Inventory.class)
                .filter(Filters.eq("_id", userInventory.getId()))
                .update(
                        new UpdateOptions(),
                        isSell ?
                                Inventory.getMorphiaUpdateRemoveItems(Inventory.getItemsIds(List.of(item.get())), userInventory.getCurrentWeight()) :
                                Inventory.getMorphiaUpdateAddItems(item.get(), userInventory.getCurrentWeight())
                );

        session.find(User.class)
                .filter(Filters.eq("_id", userId))
                .update(
                        new UpdateOptions(),
                        isSell ?
                            User.getMorphiaIncreaseGold(merchantTransaction.cost()) :
                            User.getMorphiaDecreaseGold(merchantTransaction.cost())

                );

        ItemMerchant itemMerchant = new ItemMerchant(item.get(), merchantTransaction.cost());
        session.find(Merchant.class)
                .filter(Filters.eq("user", userId))
                .update(
                        new UpdateOptions(),
                        isSell ?
                                Merchant.getMorphiaUpdateSellItem(itemMerchant) :
                                Merchant.getMorphiaUpdateBuyItem(List.of(itemMerchant))
                );
    }


    public Merchant getOrCreateMerchant(User user, int mainCharacterLevel) throws Exception {
        Optional<Merchant> foundMerchant = this.findMerchantByUserId(user.getId());

        if(foundMerchant.isEmpty()) {
            List<Item> items = MerchantsUtils.generateMerchantsItems(user, mainCharacterLevel);
            return this.create(user, items);
        }

        Merchant merchant = foundMerchant.get();
        if(merchant.isCommodityExpired()) {
            List<Item> newItems = MerchantsUtils.generateMerchantsItems(user, mainCharacterLevel);

            this.handleRefreshCommodity(merchant, newItems);
        }

        return merchant;
    }

    private void handleRefreshCommodity (Merchant merchant, List<Item> newItems) throws Exception {
        try (MorphiaSession session = datastore.startSession()) {
            session.startTransaction();


            for(String itemId: merchant.getItems().keySet()){
                session.find(Item.class)
                        .filter(Filters.eq("id", new ObjectId(itemId)))
                        .delete();
            }
            merchant.setNewCommodity(TransactionsUtils.handleCreatingNewItems(session, newItems));
            session.save(merchant);
            session.commitTransaction();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
}
