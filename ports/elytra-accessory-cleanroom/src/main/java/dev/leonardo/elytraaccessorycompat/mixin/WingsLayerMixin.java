package dev.leonardo.elytraaccessorycompat.mixin;

import com.swacky.ohmega.api.AccessoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
    @Redirect(
            method = "submit",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private ItemStack elytraAccessory263$useOhmegaElytraForWings(HumanoidRenderState state) {
        ItemStack chest = state.chestEquipment;

        // Preserve vanilla / modded gliders equipped in the normal chest slot.
        if (!chest.isEmpty() && chest.has(DataComponents.GLIDER)) {
            return chest;
        }

        if (!(state instanceof AvatarRenderState avatarState)) {
            return chest;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return chest;
        }

        Entity entity = minecraft.level.getEntity(avatarState.id);
        if (!(entity instanceof Player player)) {
            return chest;
        }

        for (ItemStack stack : AccessoryHelper.getAccessoryStacks(player)) {
            if (stack.is(Items.ELYTRA)) {
                return stack;
            }
        }

        return chest;
    }
}
