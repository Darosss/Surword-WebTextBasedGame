package com.textbasedgame.merchants;

import com.textbasedgame.items.Item;
import com.textbasedgame.items.ItemTypeEnum;
import com.textbasedgame.items.ItemUtils;
import com.textbasedgame.settings.RaritiesBonuses;
import com.textbasedgame.users.User;
import com.textbasedgame.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MerchantsUtils {
    private MerchantsUtils() {};

    public static List<Item> generateMerchantsItems(User user, int characterLevel, RaritiesBonuses raritiesBonuses) {
        List<Item> generatedItems = new ArrayList<>();
        for (ItemTypeEnum itemType : ItemTypeEnum.values()) {
            ItemTypeEnum.MerchantOptions options = itemType.getMerchantOptions();
            int generateCount = RandomUtils.getRandomValueWithinRange(options.minItems(), options.maxItems());
            for (int i = 0; i < generateCount; i++){
                int itemLevel = RandomUtils.getRandomValueWithinRange(
                        Math.max(1, characterLevel - 5), characterLevel + 12
                );
                generatedItems.add(ItemUtils.generateRandomItemWithoutBaseStats(
                        user, itemLevel, itemType, raritiesBonuses, Optional.empty())
                );

            }

        }

        return generatedItems;
    }


}
