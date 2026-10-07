package com.coolerpromc.archerythings.screen.quiver;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.item.ModItems;
import com.coolerpromc.archerythings.platform.Services;
import com.coolerpromc.archerythings.screen.ModMenuTypes;
import com.coolerpromc.archerythings.screen.container.QuiverContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class QuiverMenu extends AbstractContainerMenu {
    public static final int QUIVER_ROWS = 3;
    public static final int QUIVER_COLUMNS = 9;
    public static final int QUIVER_SLOT_COUNT = QUIVER_ROWS * QUIVER_COLUMNS;

    public final ItemStack quiver;
    private final QuiverContainer container;
    public final Player player;
    public final InteractionHand hand;

    public QuiverMenu(int id, Inventory inventory, Player player) {
        this(id, inventory, player, null);
    }

    public QuiverMenu(int id, Inventory inventory, Player player, @Nullable InteractionHand hand) {
        super(ModMenuTypes.QUIVER_MENU.get(), id);

        if (hand != null && player.getItemInHand(hand).is(ModItems.QUIVER.get())) {
            quiver = player.getItemInHand(hand);
        } else if (hand != null && player.getItemInHand(hand).has(ModDataComponents.STORED_QUIVER.get())) {
            quiver = player.getItemInHand(hand).get(ModDataComponents.STORED_QUIVER.get()).stack();
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.QUIVER.get())) {
            quiver = player.getItemBySlot(EquipmentSlot.CHEST);
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).has(ModDataComponents.STORED_QUIVER.get())) {
            quiver = player.getItemBySlot(EquipmentSlot.CHEST).get(ModDataComponents.STORED_QUIVER.get()).stack();
        } else if (player.getItemBySlot(EquipmentSlot.LEGS).has(ModDataComponents.STORED_QUIVER.get())) {
            quiver = player.getItemBySlot(EquipmentSlot.LEGS).get(ModDataComponents.STORED_QUIVER.get()).stack();
        } else if (Services.QUIVER.isQuiverEquipped(player)) {
            quiver = Services.QUIVER.getQuiver(player);
        } else {
            quiver = ItemStack.EMPTY;
        }

        this.container = new QuiverContainer(quiver);
        this.player = player;
        this.hand = hand;

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        for (int row = 0; row < QUIVER_ROWS; row++) {
            for (int col = 0; col < QUIVER_COLUMNS; col++) {
                int index = col + row * QUIVER_COLUMNS;
                this.addSlot(new Slot(container, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof ArrowItem || stack.getItem() instanceof FireworkRocketItem;
                    }
                });
            }
        }
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int QUIVER_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, QUIVER_FIRST_SLOT_INDEX, QUIVER_FIRST_SLOT_INDEX + QUIVER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < QUIVER_FIRST_SLOT_INDEX + QUIVER_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return (!quiver.isEmpty() && player.getInventory().contains(quiver))
                || player.getItemInHand(InteractionHand.MAIN_HAND).has(ModDataComponents.STORED_QUIVER.get())
                || player.getItemBySlot(EquipmentSlot.CHEST).has(ModDataComponents.STORED_QUIVER.get())
                || player.getItemBySlot(EquipmentSlot.LEGS).has(ModDataComponents.STORED_QUIVER.get())
                || Services.QUIVER.isQuiverEquipped(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }

    public int selected() {
        return quiver.getOrDefault(ModDataComponents.SELECTED.get(), 0);
    }
}
