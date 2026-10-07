package com.coolerpromc.archerythings.screen.container;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class QuiverContainer extends SimpleContainer {
    public static final int SLOT_COUNT = 27;

    private final ItemStack quiver;

    public QuiverContainer(ItemStack quiver) {
        super(SLOT_COUNT);
        this.quiver = quiver;
        QuiverData contents = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        contents.copyInto(this.getItems());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.quiver.set(ModDataComponents.QUIVER_DATA.get(), QuiverData.fromItems(this.getItems()));
    }
}
