package dev.leonardo.totemaccessorycompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.swacky.ohmega.api.AccessoryHelper;
import dev.leonardo.totemaccessorycompat.TotemAccessoryCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.LightCoordsUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT, modid = TotemAccessoryCompat.MOD_ID)
public final class TotemAccessoryClient {
    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(TotemAccessoryCompat.MOD_ID, "equipped_totem");

    private static final Identifier HOTBAR_OFFHAND_LEFT =
            Identifier.withDefaultNamespace("hud/hotbar_offhand_left");

    private TotemAccessoryClient() {}

    @SubscribeEvent
    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, TotemAccessoryClient::renderTotemHud);
    }

    private static void renderTotemHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return;
        }

        ItemStack totem = findEquippedTotem(player);
        if (totem.isEmpty()) {
            return;
        }

        int screenCenter = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 23;

        // Vanilla offhand background is 29x24. Place our visual accessory slot
        // directly to the left of where the left/offhand slot appears.
        int slotX = screenCenter - 91 - 29 - 29;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT, slotX, y, 29, 24);

        // Match vanilla's +3px inset for the item inside an offhand slot.
        graphics.item(player, totem, slotX + 3, graphics.guiHeight() - 19, 77);
    }

    @SubscribeEvent
    public static void renderTotemAmulet(RenderPlayerEvent.Post<?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        int entityId = event.getRenderState().id;
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack totem = findEquippedTotem(player);
        if (totem.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Front of the upper chest / base of the neck.
        // Player render origin is at the feet, and the model faces local -Z.
        poseStack.translate(0.0F, 1.38F, -0.245F);
        poseStack.scale(0.22F, 0.22F, 0.22F);

        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                renderState,
                totem,
                ItemDisplayContext.FIXED,
                player.level(),
                player,
                player.getId()
        );

        renderState.submit(
                poseStack,
                event.getSubmitNodeCollector(),
                event.getRenderState().lightCoords,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                0
        );

        poseStack.popPose();
    }

    private static ItemStack findEquippedTotem(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
