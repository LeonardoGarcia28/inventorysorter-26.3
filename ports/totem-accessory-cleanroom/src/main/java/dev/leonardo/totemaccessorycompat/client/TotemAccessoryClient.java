package dev.leonardo.totemaccessorycompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.swacky.ohmega.api.AccessoryHelper;
import dev.leonardo.totemaccessorycompat.TotemAccessoryCompat;
import dev.leonardo.totemaccessorycompat.config.TotemAccessoryClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(value = Dist.CLIENT, modid = TotemAccessoryCompat.MOD_ID)
public final class TotemAccessoryClient {
    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(TotemAccessoryCompat.MOD_ID, "equipped_totem");

    private static final Identifier HOTBAR_OFFHAND_RIGHT =
            Identifier.withDefaultNamespace("hud/hotbar_offhand_right");

    private TotemAccessoryClient() {}

    @SubscribeEvent
    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, TotemAccessoryClient::renderTotemHud);
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new TotemAmuletLayer(renderer));
            }
        }
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

        // HUD order: Elytra -> Totem -> Quiver.
        // Each accessory keeps the original independent 29 px slot spacing.
        int slotX = screenCenter + 91 + (hasEquippedElytra(player) ? 29 : 0);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT, slotX, y, 29, 24);
        graphics.item(player, totem, slotX + 10, graphics.guiHeight() - 19, 77);
    }

    private static boolean hasEquippedElytra(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(Items.ELYTRA)) {
                return true;
            }
        }

        return false;
    }

    private static ItemStack findEquippedTotem(Player player) {
        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static final class TotemAmuletLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private TotemAmuletLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int lightCoords,
                AvatarRenderState state,
                float yRot,
                float xRot
        ) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            Entity entity = minecraft.level.getEntity(state.id);
            if (!(entity instanceof Player player)) {
                return;
            }

            ItemStack totem = findEquippedTotem(player);
            if (totem.isEmpty()) {
                return;
            }

            poseStack.pushPose();

            // This is now a true player render layer, like Elytra/Cape layers.
            // Root + body transforms make the Totem inherit the player's complete
            // model transform and torso animation instead of floating in world space.
            PlayerModel model = this.getParentModel();
            model.root().translateAndRotate(poseStack);
            model.body.translateAndRotate(poseStack);

            double z = state.chestEquipment.isEmpty()
                    ? TotemAccessoryClientConfig.AMULET_Z_NO_CHEST.get()
                    : TotemAccessoryClientConfig.AMULET_Z.get();

            poseStack.translate(
                    TotemAccessoryClientConfig.AMULET_X.get().floatValue(),
                    TotemAccessoryClientConfig.AMULET_Y.get().floatValue(),
                    (float) z
            );

            // Minecraft's living-entity render space is vertically inverted for item models.
            // Rotate the Totem in its own plane so it appears upright without changing
            // its attachment to the animated torso.
            poseStack.rotateDegrees(Axis.ZP, 180.0F);

            float scale = TotemAccessoryClientConfig.AMULET_SCALE.get().floatValue();
            poseStack.scale(scale, scale, scale);

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
                    submitNodeCollector,
                    lightCoords,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    state.outlineColor
            );

            poseStack.popPose();
        }
    }
}
