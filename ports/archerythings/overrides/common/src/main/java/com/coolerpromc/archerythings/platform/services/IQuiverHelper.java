package com.coolerpromc.archerythings.platform.services;

import com.coolerpromc.archerythings.compat.TrinketsHelper;
import com.coolerpromc.archerythings.platform.Services;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IQuiverHelper {
    String TRINKETS_MODID = "trinkets_updated";

    boolean isQuiverEquipped(Player player);
    ItemStack getQuiver(Player player);

    default boolean forcePickupToQuiver(Player player) {
        return false;
    }

    /**
     * Called whenever Archery Things mutates an equipped Quiver in-place.
     * Platform integrations may use this to immediately synchronise accessory data.
     */
    default void syncQuiver(Player player, ItemStack quiver) {}

    default boolean isQuiverEquippedCommon(Player player) {
        return Services.PLATFORM.isModLoaded(TRINKETS_MODID) && TrinketsHelper.isQuiverEquipped(player);
    }

    default ItemStack getQuiverCommon(Player player) {
        return TrinketsHelper.getQuiver(player);
    }
}
