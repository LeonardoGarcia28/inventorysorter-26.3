package com.coolerpromc.archerythings.model.quiver;

import com.coolerpromc.archerythings.Constants;
import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.item.custom.ModQuiverItem;
import com.coolerpromc.archerythings.model.ModModelLayers;
import com.coolerpromc.archerythings.util.LivingEntityRenderStateAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class QuiverLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final Identifier TEXTURE = Constants.id("textures/entity/quiver.png");
    private static final Identifier LEG_TEXTURE = Constants.id("textures/entity/quiver_leg.png");

    private final QuiverModel<S> model;
    private final QuiverLegModel legModel;

    public QuiverLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new QuiverModel<>(modelSet.bakeLayer(ModModelLayers.QUIVER), RenderTypes::entityCutout);
        this.legModel = new QuiverLegModel(modelSet.bakeLayer(ModModelLayers.QUIVER_LEG), RenderTypes::entityCutout);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            int packedLight,
            S renderState,
            float yRot,
            float partialTick
    ) {
        ItemStack accessoryQuiver = ((LivingEntityRenderStateAccessor) renderState).archerythings$getQuiverStack();

        if (isQuiverLike(renderState.chestEquipment)) {
            renderBackQuiver(poseStack, nodeCollector, packedLight, renderState, renderState.chestEquipment);
        }

        if (isQuiverLike(renderState.legsEquipment)) {
            renderLegQuiver(poseStack, nodeCollector, packedLight, renderState, renderState.legsEquipment);
        }

        // Ohmega/Trinkets accessory Quivers deliberately use the mod's existing
        // leg presentation instead of the back presentation.
        if (accessoryQuiver != null && !accessoryQuiver.isEmpty()) {
            renderLegQuiver(poseStack, nodeCollector, packedLight, renderState, accessoryQuiver);
        }
    }

    private boolean isQuiverLike(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ModQuiverItem || stack.has(ModDataComponents.STORED_QUIVER.get()));
    }

    private ItemStack unwrap(ItemStack stack) {
        if (stack.has(ModDataComponents.STORED_QUIVER.get())) {
            return stack.get(ModDataComponents.STORED_QUIVER.get()).stack();
        }
        return stack;
    }

    private void renderBackQuiver(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            int packedLight,
            S renderState,
            ItemStack parent
    ) {
        boolean stored = parent.has(ModDataComponents.STORED_QUIVER.get());
        ItemStack quiver = unwrap(parent);

        float z = stored ? 0.13F : 0.06875F;
        float crouchZ = stored ? 0.028F : -0.04875F;

        ItemStackRenderState arrowState = selectedArrowState(quiver);
        int color = DyedItemColor.getOrDefault(quiver, DyedItemColor.LEATHER_COLOR);

        poseStack.pushPose();
        if (renderState.isCrouching) {
            poseStack.rotateDegrees(Axis.XP, 30);
            poseStack.translate(0.0F, -0.053125F, crouchZ);
        } else {
            poseStack.translate(0.0F, -0.053125F, z);
        }

        nodeCollector.submitModel(
                this.model,
                renderState,
                poseStack,
                RenderTypes.entityCutout(TEXTURE),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                color,
                null,
                renderState.outlineColor);
        poseStack.popPose();

        poseStack.pushPose();
        if (renderState.isCrouching) {
            poseStack.rotateDegrees(Axis.XP, 30);
            poseStack.translate(-0.3F, 0.0F, 0.2F);
        } else {
            poseStack.translate(-0.3F, 0.0F, 0.325F);
        }
        poseStack.rotateDegrees(Axis.ZP, 85);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        arrowState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }

    private void renderLegQuiver(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            int packedLight,
            S renderState,
            ItemStack parent
    ) {
        boolean stored = parent.has(ModDataComponents.STORED_QUIVER.get());
        ItemStack quiver = unwrap(parent);

        float z = stored ? 0.13F : 0.06875F;
        ItemStackRenderState arrowState = selectedArrowState(quiver);
        int color = DyedItemColor.getOrDefault(quiver, DyedItemColor.LEATHER_COLOR);

        poseStack.pushPose();
        if (renderState.isCrouching) {
            poseStack.rotateDegrees(Axis.XP, 30);
            poseStack.translate(0.0F, -0.053125F, -0.01F);
        } else {
            poseStack.translate(0.0F, -0.053125F, z);
        }

        nodeCollector.submitModel(
                this.legModel,
                renderState,
                poseStack,
                RenderTypes.entityCutout(LEG_TEXTURE),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                color,
                null,
                renderState.outlineColor);
        poseStack.popPose();

        poseStack.pushPose();
        if (renderState.isCrouching) {
            poseStack.rotateDegrees(Axis.XP, 30);
            poseStack.translate(-0.4F, 0.85F, 0.05F);
        } else {
            poseStack.translate(-0.4F, 0.75F, 0.05F);
        }
        poseStack.rotateDegrees(Axis.ZP, 90);
        poseStack.rotateDegrees(Axis.XN, 90);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        arrowState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }

    private ItemStackRenderState selectedArrowState(ItemStack quiver) {
        QuiverData data = quiver.getOrDefault(ModDataComponents.QUIVER_DATA.get(), QuiverData.EMPTY);
        int selected = quiver.getOrDefault(ModDataComponents.SELECTED.get(), 0);

        ItemStackRenderState state = new ItemStackRenderState();
        if (selected >= 0 && selected < data.getSlots()) {
            ItemModelResolver resolver = new ItemModelResolver(Minecraft.getInstance().getModelManager());
            resolver.updateForTopItem(
                    state,
                    data.getStackInSlot(selected),
                    ItemDisplayContext.FIXED,
                    null,
                    null,
                    1);
        }

        return state;
    }
}
