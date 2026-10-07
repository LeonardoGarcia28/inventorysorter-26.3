package com.coolerpromc.archerythings.util;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.item.ModItems;
import com.coolerpromc.archerythings.platform.Services;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class ArrowHandler {
    public static final int QUIVER_SLOT_COUNT = 27;

    public static void onArrowLoose(Player player, ItemStack bow, Level level, boolean hasAmmo) {
        ItemStack chestEquipment = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legEquipment = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack quiverLike = ItemStack.EMPTY;

        if (chestEquipment.is(ModItems.QUIVER.get()) && hasArrow(chestEquipment)) {
            quiverLike = chestEquipment;
        } else if (chestEquipment.has(ModDataComponents.STORED_QUIVER.get())
                && hasArrow(chestEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack())) {
            quiverLike = chestEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
        } else if (legEquipment.has(ModDataComponents.STORED_QUIVER.get())
                && hasArrow(legEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack())) {
            quiverLike = legEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
        } else if (Services.QUIVER.isQuiverEquipped(player)) {
            quiverLike = Services.QUIVER.getQuiver(player);
        }

        if (quiverLike.isEmpty()) {
            return;
        }

        QuiverData contents = quiverLike.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        int selected = resolveAmmoSlot(quiverLike, contents);

        if (selected < 0 || !hasAmmo) {
            return;
        }

        NonNullList<ItemStack> updatedItems =
                NonNullList.withSize(Math.max(QUIVER_SLOT_COUNT, contents.getSlots()), ItemStack.EMPTY);
        contents.copyInto(updatedItems);

        ItemStack arrow = updatedItems.get(selected);
        if (!arrow.isEmpty()
                && bow.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .getLevel(level.holderLookup(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.INFINITY)) != 1) {
            arrow.shrink(1);
            if (arrow.isEmpty()) {
                updatedItems.set(selected, ItemStack.EMPTY);
            }

            quiverLike.set(ModDataComponents.QUIVER_DATA.get(), QuiverData.fromItems(updatedItems));
            Services.QUIVER.syncQuiver(player, quiverLike);
        }
    }

    public static ItemStack getProjectileFromQuiver(LivingEntity livingEntity, ItemStack ammo) {
        if (livingEntity instanceof Player player) {
            ItemStack chestEquipment = player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack legEquipment = player.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack quiverLike = ItemStack.EMPTY;

            if (chestEquipment.is(ModItems.QUIVER.get()) && hasArrow(chestEquipment)) {
                quiverLike = chestEquipment;
            } else if (chestEquipment.has(ModDataComponents.STORED_QUIVER.get())
                    && hasArrow(chestEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack())) {
                quiverLike = chestEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
            } else if (legEquipment.has(ModDataComponents.STORED_QUIVER.get())
                    && hasArrow(legEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack())) {
                quiverLike = legEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
            } else if (Services.QUIVER.isQuiverEquipped(player)) {
                quiverLike = Services.QUIVER.getQuiver(player);
            }

            if (!quiverLike.isEmpty()) {
                QuiverData contents =
                        quiverLike.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
                int selected = resolveAmmoSlot(quiverLike, contents);

                if (selected >= 0) {
                    Services.QUIVER.syncQuiver(player, quiverLike);
                    return contents.getStackInSlot(selected);
                }

                player.sendOverlayMessage(Component.translatable("message.archerythings.no_quiver_slot"));
            }
        }

        return ammo;
    }

    /**
     * Keep the explicit orange selection when it contains ammo. If it is empty,
     * automatically fall through to the first non-empty slot among all 27.
     */
    private static int resolveAmmoSlot(ItemStack quiver, QuiverData contents) {
        int selected = quiver.getOrDefault(ModDataComponents.SELECTED.get(), 0);

        if (selected >= 0 && selected < contents.getSlots()
                && !contents.getStackInSlot(selected).isEmpty()) {
            return selected;
        }

        int limit = Math.min(QUIVER_SLOT_COUNT, contents.getSlots());
        for (int i = 0; i < limit; i++) {
            if (!contents.getStackInSlot(i).isEmpty()) {
                if (selected != i) {
                    quiver.set(ModDataComponents.SELECTED.get(), i);
                }
                return i;
            }
        }

        return -1;
    }

    private static boolean hasArrow(ItemStack quiverLike) {
        if (quiverLike.isEmpty()) {
            return false;
        }

        ItemStack quiver = quiverLike;
        if (quiverLike.has(ModDataComponents.STORED_QUIVER.get())) {
            quiver = quiverLike.get(ModDataComponents.STORED_QUIVER.get()).stack();
        }

        if (quiver.getItem() != ModItems.QUIVER.get()) {
            return false;
        }

        QuiverData contents = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        int selected = quiver.getOrDefault(ModDataComponents.SELECTED.get(), 0);

        if (selected >= 0 && selected < contents.getSlots()
                && !contents.getStackInSlot(selected).isEmpty()) {
            return true;
        }

        int limit = Math.min(QUIVER_SLOT_COUNT, contents.getSlots());
        for (int i = 0; i < limit; i++) {
            if (!contents.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPickupEnabled(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() != ModItems.QUIVER.get() && !stack.has(ModDataComponents.STORED_QUIVER.get())) {
            return false;
        }

        ItemStack quiver = stack;
        if (stack.has(ModDataComponents.STORED_QUIVER.get())) {
            quiver = stack.get(ModDataComponents.STORED_QUIVER.get()).stack();
        }

        return quiver.getOrDefault(ModDataComponents.COLLECT_TO_QUIVER.get(), true);
    }

    public static ItemStack getQuiverForPickup(Player player) {
        ItemStack chestEquipment = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legEquipment = player.getItemBySlot(EquipmentSlot.LEGS);

        if (chestEquipment.is(ModItems.QUIVER.get()) && isPickupEnabled(chestEquipment)) {
            return chestEquipment;
        }

        if (chestEquipment.has(ModDataComponents.STORED_QUIVER.get())) {
            ItemStack inner = chestEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
            if (isPickupEnabled(inner)) {
                return chestEquipment;
            }
        }

        if (legEquipment.has(ModDataComponents.STORED_QUIVER.get())) {
            ItemStack inner = legEquipment.get(ModDataComponents.STORED_QUIVER.get()).stack();
            if (isPickupEnabled(inner)) {
                return legEquipment;
            }
        }

        if (Services.QUIVER.isQuiverEquipped(player)) {
            ItemStack q = Services.QUIVER.getQuiver(player);
            if (Services.QUIVER.forcePickupToQuiver(player) || isPickupEnabled(q)) {
                return q;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean insertIntoQuiver(ItemStack parentOrQuiver, ItemStack arrowStack) {
        ItemStack quiver = parentOrQuiver;
        boolean isNested = parentOrQuiver.has(ModDataComponents.STORED_QUIVER.get());

        if (isNested) {
            quiver = parentOrQuiver.get(ModDataComponents.STORED_QUIVER.get()).stack();
        }

        QuiverData contents = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        NonNullList<ItemStack> updatedItems =
                NonNullList.withSize(Math.max(QUIVER_SLOT_COUNT, contents.getSlots()), ItemStack.EMPTY);
        contents.copyInto(updatedItems);

        boolean insertedAny = false;

        for (int i = 0; i < QUIVER_SLOT_COUNT; i++) {
            ItemStack existing = updatedItems.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, arrowStack)) {
                int space = Math.min(existing.getMaxStackSize(), 64) - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, arrowStack.getCount());
                    existing.grow(toAdd);
                    arrowStack.shrink(toAdd);
                    insertedAny = true;

                    if (arrowStack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (!arrowStack.isEmpty()) {
            for (int i = 0; i < QUIVER_SLOT_COUNT; i++) {
                if (updatedItems.get(i).isEmpty()) {
                    int amount = Math.min(
                            arrowStack.getCount(),
                            Math.min(arrowStack.getMaxStackSize(), 64));
                    ItemStack inserted = arrowStack.copy();
                    inserted.setCount(amount);
                    updatedItems.set(i, inserted);
                    arrowStack.shrink(amount);
                    insertedAny = true;

                    if (arrowStack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (insertedAny) {
            quiver.set(ModDataComponents.QUIVER_DATA.get(), QuiverData.fromItems(updatedItems));

            if (isNested) {
                parentOrQuiver.set(
                        ModDataComponents.STORED_QUIVER.get(),
                        new com.coolerpromc.archerythings.component.data.StoredQuiver(quiver));
            }
        }

        return insertedAny;
    }
}
