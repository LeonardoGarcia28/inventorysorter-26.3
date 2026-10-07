package com.coolerpromc.archerythings.compat;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.item.ModItems;
import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.api.IAccessory;
import com.swacky.ohmega.api.event.AccessoryOverrideTypesEvent;
import com.swacky.ohmega.common.accessorytype.AccessoryType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class OhmegaHelper {
    // Tracks only the last Quiver contents hash seen on the logical server.
    // Weak keys avoid retaining disconnected players.
    private static final Map<ServerPlayer, Integer> LAST_QUIVER_HASH = new WeakHashMap<>();

    private static final IAccessory QUIVER_ACCESSORY = new IAccessory() {
        @Override
        public void tick(@NonNull Player player, @NonNull ItemStack stack) {
            if (player instanceof ServerPlayer serverPlayer) {
                syncContentsIfChanged(serverPlayer, stack);
            }
        }

        @Override
        public void onEquip(@NonNull Player player, @NonNull ItemStack stack) {
            if (player instanceof ServerPlayer serverPlayer) {
                LAST_QUIVER_HASH.put(serverPlayer, getContentsHash(stack));
            }
        }

        @Override
        public void onUnequip(@NonNull Player player, @NonNull ItemStack stack) {
            if (player instanceof ServerPlayer serverPlayer) {
                LAST_QUIVER_HASH.remove(serverPlayer);
            }
        }
    };

    private OhmegaHelper() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(OhmegaHelper::onOverrideAccessoryTypes);
    }

    private static void onOverrideAccessoryTypes(AccessoryOverrideTypesEvent event) {
        Item quiver = ModItems.QUIVER.get();

        if (!AccessoryHelper.isItemAccessoryBound(quiver)) {
            AccessoryHelper.bindAccessory(quiver, QUIVER_ACCESSORY);
        }

        event.overrideRemaps.put(quiver, AccessoryType.UTILITY.get());
    }

    private static int getContentsHash(ItemStack stack) {
        QuiverData data = stack.getOrDefault(
                ModDataComponents.QUIVER_DATA.get(),
                QuiverData.EMPTY
        );
        return data.hashCode();
    }

    private static void syncContentsIfChanged(ServerPlayer player, ItemStack stack) {
        int currentHash = getContentsHash(stack);
        Integer previousHash = LAST_QUIVER_HASH.put(player, currentHash);

        // First observation establishes the baseline. No packet is necessary.
        if (previousHash == null || previousHash == currentHash) {
            return;
        }

        int slot = AccessoryHelper.getSlot(stack);
        if (slot < 0) {
            return;
        }

        // Let Ohmega know that an in-place ItemStack component changed.
        AccessoryHelper.getContainer(player).onContentsChanged(slot);

        // Send only this accessory slot to its owner. The HUD reads the client-side
        // Ohmega stack, so this refreshes xN on the next tick after pickup.
        AccessoryHelper.syncSlots(
                player,
                new int[]{slot},
                List.of(stack.copy()),
                List.of(player)
        );
    }

    public static boolean isQuiverEquipped(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(ModItems.QUIVER.get())) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack getQuiver(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(ModItems.QUIVER.get())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
