package com.textbasedgame.merchants;

import com.textbasedgame.items.Item;
import com.textbasedgame.settings.MerchantConfig;
import com.textbasedgame.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.morphia.annotations.*;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity("merchants")
public class Merchant {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    private ObjectId id;

    @Reference(idOnly = true, lazy = true)
    @JsonIgnore
    private User user;

    @JsonIgnoreProperties("item.user")
    private List<ItemMerchant> items = new ArrayList<>();

    @Transient
    private Map<String, ItemMerchant> itemMap = new HashMap<>();

    private LocalDateTime commodityRefreshAt;

    Merchant() {}

    public record MerchantTransaction(Optional<Item> item, long cost){}
    public Merchant(User user, List<Item> itemsList, MerchantConfig configs) {
        this.user = user;
        this.setNewCommodity(itemsList, configs);
    }

    public void syncItemsToList() {
        this.items = new ArrayList<>(this.itemMap.values());
    }

    @PostLoad
    public void rebuildItemsMap() {
        this.itemMap = this.items == null ? new HashMap<>() : this.items.stream()
                .collect(Collectors.toMap((itemMerch)->itemMerch.getItem().getId().toString(), Function.identity()));
    }

    public static UpdateOperator getMorphiaUpdateBuyItem(List<ItemMerchant> itemsMerchant) {
        return UpdateOperators.pullAll("items", itemsMerchant);
    }

    public static UpdateOperator getMorphiaUpdateSellItem(ItemMerchant itemMerchant) {
        return UpdateOperators.addToSet("items", itemMerchant);
    }

    public ObjectId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Map<String, ItemMerchant> getItems() {
        return this.itemMap;
    }

    public LocalDateTime getCommodityRefreshAt() {
        return commodityRefreshAt;
    }

    public MerchantTransaction buyItemByItemId(String itemId) {
        if(!this.itemMap.containsKey(itemId)) return new MerchantTransaction(Optional.empty(), 0);

        ItemMerchant boughtItem = this.itemMap.remove(itemId);

        this.syncItemsToList();
        return new MerchantTransaction(Optional.ofNullable(boughtItem.getItem()),
                boughtItem.getCost());
    }

    public MerchantTransaction sellItem(Item item) {
        String itemIdString = item.getId().toString();
        long soldItemCost = item.getValue();
        ItemMerchant itemMerchant = new ItemMerchant(item, soldItemCost);
        this.itemMap.put(itemIdString, itemMerchant);

        this.syncItemsToList();
        return new MerchantTransaction(Optional.of(itemMerchant.getItem()), itemMerchant.getCost());
    }

    public boolean isCommodityExpired(){
        return this.commodityRefreshAt.isBefore(LocalDateTime.now());
    }

   public void setNewCommodity(List<Item> newItems, MerchantConfig configs) {
       this.itemMap.clear();
       for(Item item: newItems) {
           String itemIdString = item.getId().toString();
           long currentItemCost = (long) (item.getValue() * configs.getBuyCostValueMultiplier());
           this.itemMap.put(itemIdString, new ItemMerchant(item, currentItemCost));
       }
       this.syncItemsToList();
       this.commodityRefreshAt = LocalDateTime.now().plusSeconds(configs.getCommodityRefreshMS() / 60);
   }

}
