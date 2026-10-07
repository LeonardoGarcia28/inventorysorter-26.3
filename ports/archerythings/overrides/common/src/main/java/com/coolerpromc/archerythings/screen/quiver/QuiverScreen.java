package com.coolerpromc.archerythings.screen.quiver;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.network.packet.ServerBoundSelectQuiverSlotPacket;
import com.coolerpromc.archerythings.platform.Services;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {
    private static final Identifier CONTAINER_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int BUTTON_X_OFFSET = 160;
    private static final int BUTTON_Y_OFFSET = 5;
    private static final int BUTTON_SIZE = 9;
    private Button togglePickupButton;

    public QuiverScreen(QuiverMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 114 + QuiverMenu.QUIVER_ROWS * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int buttonX = this.leftPos + BUTTON_X_OFFSET;
        int buttonY = this.topPos + BUTTON_Y_OFFSET;

        this.togglePickupButton = Button.builder(
                Component.empty(),
                btn -> {
                    boolean current = this.menu.quiver.getOrDefault(ModDataComponents.COLLECT_TO_QUIVER.get(), true);
                    boolean newValue = !current;
                    this.menu.quiver.set(ModDataComponents.COLLECT_TO_QUIVER.get(), newValue);
                    btn.setTooltip(Tooltip.create(getButtonTooltip(newValue)));
                    Services.NETWORK.sendToServer(
                            new com.coolerpromc.archerythings.network.packet.ServerBoundToggleQuiverPickupPacket(
                                    newValue, Optional.ofNullable(this.menu.hand)));
                })
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(getButtonTooltip()))
                .build();

        this.addRenderableWidget(this.togglePickupButton);
    }

    private Component getButtonMessage() {
        return getButtonMessage(this.menu.quiver.getOrDefault(ModDataComponents.COLLECT_TO_QUIVER.get(), true));
    }

    private Component getButtonMessage(boolean collect) {
        return collect
                ? Component.translatable("gui.archerythings.pickup.button.quiver")
                : Component.translatable("gui.archerythings.pickup.button.inventory");
    }

    private Component getButtonTooltip() {
        return getButtonTooltip(this.menu.quiver.getOrDefault(ModDataComponents.COLLECT_TO_QUIVER.get(), true));
    }

    private Component getButtonTooltip(boolean collect) {
        return collect
                ? Component.translatable("gui.archerythings.pickup.tooltip.quiver")
                : Component.translatable("gui.archerythings.pickup.tooltip.inventory");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int topHeight = QuiverMenu.QUIVER_ROWS * 18 + 17;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                leftPos, topPos, 0.0F, 0.0F, imageWidth, topHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                leftPos, topPos + topHeight, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int selected = Math.max(0, Math.min(QuiverMenu.QUIVER_SLOT_COUNT - 1, this.menu.selected()));
        Slot slot = this.menu.slots.get(36 + selected);
        int x = this.leftPos + slot.x - 1;
        int y = this.topPos + slot.y - 1;

        graphics.fill(x, y, x + 18, y + 1, ARGB.color(0xFF, 16755200));
        graphics.fill(x, y + 17, x + 18, y + 18, ARGB.color(0xFF, 16755200));
        graphics.fill(x, y, x + 1, y + 18, ARGB.color(0xFF, 16755200));
        graphics.fill(x + 17, y, x + 18, y + 18, ARGB.color(0xFF, 16755200));

        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        String text = getButtonMessage().getString();
        int textWidth = this.font.width(text);

        graphics.pose().pushMatrix();
        float scale = 0.6f;
        float renderX = this.leftPos + BUTTON_X_OFFSET + (BUTTON_SIZE - textWidth * scale) / 2f;
        float renderY = this.topPos + BUTTON_Y_OFFSET + (BUTTON_SIZE - 9 * scale) / 2f;

        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);

        int color = this.togglePickupButton.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.text(this.font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_9) {
            int key = event.key() - InputConstants.KEY_1;
            selectSlot(key);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            selectSlot((this.menu.selected() + 1) % QuiverMenu.QUIVER_SLOT_COUNT);
            return true;
        }

        if (scrollY > 0) {
            int selected = this.menu.selected() - 1;
            if (selected < 0) {
                selected = QuiverMenu.QUIVER_SLOT_COUNT - 1;
            }
            selectSlot(selected);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void selectSlot(int selected) {
        this.menu.quiver.set(ModDataComponents.SELECTED.get(), selected);
        this.menu.broadcastChanges();
        Services.NETWORK.sendToServer(
                new ServerBoundSelectQuiverSlotPacket(selected, Optional.ofNullable(this.menu.hand)));
    }
}
