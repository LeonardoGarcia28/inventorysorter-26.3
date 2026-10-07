package com.coolerpromc.archerythings.platform;

import com.coolerpromc.archerythings.compat.OhmegaHelper;
import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.network.packet.ClientBoundQuiverSyncPacket;
import com.coolerpromc.archerythings.platform.services.IQuiverHelper;
import com.swacky.ohmega.api.AccessoryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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

    @Override
    public void syncQuiver(Player player, ItemStack quiver) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !Services.PLATFORM.isModLoaded("ohmega")) {
            return;
        }

        int slot = AccessoryHelper.getSlot(quiver);
        if (slot < 0) {
            return;
        }

        // Mark the Ohmega slot dirty for its normal tick sync and also send the
        // updated stack immediately so HUD counters/components change this tick.
        AccessoryHelper.getContainer(player).onContentsChanged(slot);
        AccessoryHelper.syncSlots(
                serverPlayer,
                new int[]{slot},
                List.of(quiver),
                serverPlayer.level().getPlayers(other -> true));

        // Do not wait for Ohmega's accessory sync cycle before updating Archery
        // Things client state. Send the Quiver's own data component immediately.
        QuiverData data = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        int selected = quiver.getOrDefault(ModDataComponents.SELECTED.get(), 0);
        Services.NETWORK.sendToPlayer(
                serverPlayer,
                new ClientBoundQuiverSyncPacket(data, selected));
    }
}
