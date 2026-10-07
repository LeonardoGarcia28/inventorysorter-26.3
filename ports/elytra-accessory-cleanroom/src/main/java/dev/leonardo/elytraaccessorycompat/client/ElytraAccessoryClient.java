package dev.leonardo.elytraaccessorycompat.client;

import com.swacky.ohmega.api.AccessoryHelper;
import dev.leonardo.elytraaccessorycompat.ElytraAccessoryCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT, modid = ElytraAccessoryCompat.MOD_ID)
public final class ElytraAccessoryClient {
    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(ElytraAccessoryCompat.MOD_ID, "equipped_elytra");

    private static final Identifier HOTBAR_OFFHAND_RIGHT =
            Identifier.withDefaultNamespace("hud/hotbar_offhand_right");

    private ElytraAccessoryClient() {}

    @SubscribeEvent
    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, ElytraAccessoryClient::renderElytraHud);
    }

    private static void renderElytraHud(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.gameMode == null) {
            return;
        }

        ItemStack elytra = findEquippedElytra(player);
        if (elytra.isEmpty()) {
            return;
        }

        int screenCenter = graphics.guiWidth() / 2;
        int slotX = screenCenter + 91;
        int itemY = graphics.guiHeight() - 19;

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_OFFHAND_RIGHT,
                slotX,
                graphics.guiHeight() - 23,
                29,
                24);

        graphics.item(player, elytra, slotX + 10, itemY, 77);

        // Vanilla item decorations include the durability bar for damageable items.
        graphics.itemDecorations(minecraft.font, elytra, slotX + 10, itemY);
    }

    private static ItemStack findEquippedElytra(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(Items.ELYTRA)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
