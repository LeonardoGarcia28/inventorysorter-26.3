package com.coolerpromc.archerythings.mixin;

import com.coolerpromc.archerythings.util.ArrowHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Shadow
    protected abstract ItemStack getPickupItem();

    @Inject(method = "tryPickup", at = @At("HEAD"), cancellable = true)
    private void onTryPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide() || player.getAbilities().instabuild) {
            return;
        }

        ItemStack arrowStack = this.getPickupItem();
        if (arrowStack.isEmpty()) {
            return;
        }

        ItemStack quiver = ArrowHandler.getQuiverForPickup(player);
        if (quiver.isEmpty()) {
            return;
        }

        boolean inserted = ArrowHandler.insertIntoQuiver(quiver, arrowStack);

        // Projectile arrows are normally a single item. If the Quiver accepted it,
        // consume the projectile; if the Quiver is full, leave it in the world.
        if (inserted && arrowStack.isEmpty()) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }
}
