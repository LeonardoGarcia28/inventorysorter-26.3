package com.coolerpromc.archerythings;

import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.key.ModKeyMappings;
import com.coolerpromc.archerythings.model.ModModelLayers;
import com.coolerpromc.archerythings.model.quiver.QuiverLayer;
import com.coolerpromc.archerythings.model.quiver.QuiverLegModel;
import com.coolerpromc.archerythings.model.quiver.QuiverModel;
import com.coolerpromc.archerythings.network.packet.ServerBoundQuiverMenuPacket;
import com.coolerpromc.archerythings.platform.Services;
import com.coolerpromc.archerythings.screen.ModMenuTypes;
import com.coolerpromc.archerythings.screen.quiver.QuiverScreen;
import com.coolerpromc.archerythings.util.ClientHandler;
import com.swacky.ohmega.api.AccessoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = Constants.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public class ArcheryThingsClient {
    private static final Identifier HUD_LAYER = Constants.id("quiver_ammo");
    private static final Identifier HOTBAR_OFFHAND_RIGHT =
            Identifier.withDefaultNamespace("hud/hotbar_offhand_right");

    public ArcheryThingsClient(ModContainer container) {}

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ModKeyMappings.ARCHERY_THINGS_CATEGORY);
        event.register(ModKeyMappings.OPEN_QUIVER_MENU.get());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyMappings.OPEN_QUIVER_MENU.get().consumeClick()) {
            Services.NETWORK.sendToServer(new ServerBoundQuiverMenuPacket());
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ClientHandler.onItemTooltip(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.QUIVER_MENU.get(), QuiverScreen::new);
    }

    @SubscribeEvent
    public static void onEntityRenderersRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.QUIVER, QuiverModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.QUIVER_LEG, QuiverLegModel::createLegLayer);
    }

    @SubscribeEvent
    public static void onEntityRenderersAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skinType : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skinType);
            if (renderer != null) {
                renderer.addLayer(new QuiverLayer<>(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        BuiltInRegistries.ITEM.stream().forEach(item -> event.register(item, ClientHandler::onRegisterItemDecoration));
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, ArcheryThingsClient::renderQuiverHud);
    }

    private static void renderQuiverHud(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.gameMode == null || !Services.QUIVER.isQuiverEquipped(player)) {
            return;
        }

        ItemStack quiver = Services.QUIVER.getQuiver(player);
        if (quiver.isEmpty()) {
            return;
        }

        QuiverData data = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        int totalArrows = 0;

        for (ItemStack stack : data.nonEmptyItems()) {
            if (stack.getItem() instanceof ArrowItem) {
                totalArrows += stack.getCount();
            }
        }

        boolean hasElytra = false;
        boolean hasTotem = false;

        for (ItemStack accessory : AccessoryHelper.getAccessoryStacks(player)) {
            if (accessory.is(Items.ELYTRA)) {
                hasElytra = true;
            } else if (accessory.is(Items.TOTEM_OF_UNDYING)) {
                hasTotem = true;
            }
        }

        int screenCenter = graphics.guiWidth() / 2;

        // HUD order: Elytra -> Totem -> Quiver.
        // Restore the original separated accessory slots: 29 px per preceding slot.
        int precedingSlots = (hasElytra ? 1 : 0) + (hasTotem ? 1 : 0);
        int slotX = screenCenter + 91 + precedingSlots * 29;
        int slotY = graphics.guiHeight() - 23;

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_OFFHAND_RIGHT,
                slotX,
                slotY,
                29,
                24);

        graphics.item(
                player,
                Items.ARROW.getDefaultInstance(),
                slotX + 10,
                graphics.guiHeight() - 19,
                77);

        String count = "x" + totalArrows;
        float scale = 0.65F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                slotX + 27 - minecraft.font.width(count) * scale,
                graphics.guiHeight() - 8);
        graphics.pose().scale(scale, scale);
        graphics.text(minecraft.font, count, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }
}
