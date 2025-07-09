package com.textbasedgame.merchants;

import com.textbasedgame.items.Item;
import dev.morphia.annotations.ExternalEntity;

@ExternalEntity(target = ItemMerchant.class)
public class ItemMerchant {
    private Item item;
    private long cost;

    public ItemMerchant(Item item, long cost) {
        this.item = item;
        this.cost = cost;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }
}
