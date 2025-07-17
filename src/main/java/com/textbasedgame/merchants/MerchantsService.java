package com.textbasedgame.merchants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textbasedgame.items.Item;
import com.textbasedgame.settings.AppConfigManager;
import com.textbasedgame.settings.LootService;
import com.textbasedgame.users.User;
import com.textbasedgame.users.inventory.Inventory;
import com.textbasedgame.utils.AggregationUtils;
import com.textbasedgame.utils.TransactionsUtils;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.transactions.MorphiaSession;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class MerchantsService {
    private record FindItemDataByIdAggregationReturn(String item, int cost) {};
    private final AppConfigManager appConfigManager;
    private final LootService lootService;
    private final Datastore datastore;

    @Autowired
    public MerchantsService(Datastore datastore, AppConfigManager appConfigManager, LootService lootService) {
        this.datastore = datastore;
        this.appConfigManager = appConfigManager;
        this.lootService = lootService;
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
                    new Merchant(user, TransactionsUtils.handleCreatingNewItems(session, items), appConfigManager.getMerchantConfig()));
            session.commitTransaction();

            return createdMerchant;
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    };

    private FindItemDataByIdAggregationReturn findItemDataByIdAggregation(MorphiaSession session, String userId, String itemId) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("user", new ObjectId(userId))),
                new Document("$unwind", "$items"),
                new Document("$match", new Document("items.item", new ObjectId(itemId))),
                new Document("$project", new Document().append("_id", 0).append("items", 1)
                )
        );

        List<Document> result = session.getDatabase()
                .getCollection("merchants")
                .aggregate(pipeline)
                .into(new ArrayList<>());

        Document firstResult = result.get(0);
        ObjectMapper mapper = new ObjectMapper();

        Document itemDoc = (Document) firstResult.get("items");
        if(itemDoc.containsKey("item")) itemDoc.put("item", itemDoc.get("item").toString());

        return mapper.convertValue(AggregationUtils.cleanMongoDocument(itemDoc), FindItemDataByIdAggregationReturn.class);
    }

    public MerchantActionReturn buyItemFromMerchant(String userId, String itemId) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();

            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);

            FindItemDataByIdAggregationReturn foundItemData = this.findItemDataByIdAggregation(session, userId, itemId);

            if(foundItemData.item.isEmpty() || foundItemData.cost() <= 0) {
                return new MerchantActionReturn(false, Optional.empty(),
                        "Cannot find item or cost from merchant. Contact administration");
            }

            Item item = TransactionsUtils.fetchItem(session, new ObjectId(foundItemData.item));
            User user = TransactionsUtils.fetchUser(session, userId);
            if(userInventory == null ||  user == null)
                return new MerchantActionReturn(false, Optional.empty(),
                        "Cannot find inventory or user. Contact administration");

            Merchant.MerchantTransaction boughtItemData = new Merchant.MerchantTransaction(Optional.of(item), foundItemData.cost());
            if(boughtItemData.item().isEmpty())
                return new MerchantActionReturn(false, Optional.empty(), "This item does not exist in merchant commodity");

            if(boughtItemData.cost() > user.getGold())
                return new MerchantActionReturn(false, Optional.empty(), "You do not have enough gold");

            userInventory.addItem(boughtItemData.item().get());

            user.decreaseGold(boughtItemData.cost());

            handleMerchantActionsUpdatesTransaction(session, user.getId(), userInventory, boughtItemData, false);

            session.commitTransaction();

            return new MerchantActionReturn(true, Optional.of(boughtItemData), "Successfully bought item");
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    public MerchantActionReturn sellItemToMerchant(String userId, String itemId) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();

            Inventory userInventory = TransactionsUtils.fetchUserInventory(session, userId);
            User user = TransactionsUtils.fetchUser(session, userId);

            if(userInventory == null || user == null)
                return new MerchantActionReturn(false, Optional.empty(),
                        "Cannot find inventory or user. Contact administration");


            Optional<Item> item = userInventory.getItemById(itemId);
            if(item.isEmpty()) return new MerchantActionReturn(false, Optional.empty(),
                    "This item does not exist in your inventory");


            Merchant.MerchantTransaction sellItemData = new Merchant.MerchantTransaction(item,
                    (long) (item.get().getValue() * appConfigManager.getMerchantConfig().getSellCostValueMultiplier()));
            userInventory.removeItemById(itemId);
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
            List<Item> items = MerchantsUtils.generateMerchantsItems(user, mainCharacterLevel, this.lootService.getCurrentRaritiesBonuses());
            return this.create(user, items);
        }

        Merchant merchant = foundMerchant.get();
        if(merchant.isCommodityExpired()) {
            List<Item> newItems = MerchantsUtils.generateMerchantsItems(user, mainCharacterLevel, this.lootService.getCurrentRaritiesBonuses());

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
            merchant.setNewCommodity(TransactionsUtils.handleCreatingNewItems(session, newItems), appConfigManager.getMerchantConfig());
            session.save(merchant);
            session.commitTransaction();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
}
