package com.coolerpromc.archerythings.platform;

import com.coolerpromc.archerythings.compat.OhmegaHelper;
import com.coolerpromc.archerythings.platform.services.IQuiverHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NeoForgeQuiverHelper implements IQuiverHelper {
    @Override
    public boolean isQuiverEquipped(Player player) {
        return (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player))
                || isQuiverEquippedCommon(player);
    }

    @Override
    public ItemStack getQuiver(Player player) {
        if (Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player)) {
            return OhmegaHelper.getQuiver(player);
        }
        if (isQuiverEquippedCommon(player)) {
            return getQuiverCommon(player);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean forcePickupToQuiver(Player player) {
        return Services.PLATFORM.isModLoaded("ohmega") && OhmegaHelper.isQuiverEquipped(player);
    }
}
