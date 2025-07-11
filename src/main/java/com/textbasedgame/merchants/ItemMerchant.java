package com.textbasedgame.merchants;

import com.textbasedgame.items.Item;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.Reference;

@ExternalEntity(target = ItemMerchant.class)
public class ItemMerchant {
    @Reference(idOnly = true, lazy = true)
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


    @Override
    public String toString() {
        return "ItemMerchant{" +
                "item=" + item +
                ", cost=" + cost +
                '}';
    }
}
