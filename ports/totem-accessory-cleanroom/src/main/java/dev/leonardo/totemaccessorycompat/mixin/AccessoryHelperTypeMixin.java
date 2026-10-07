package dev.leonardo.totemaccessorycompat.mixin;

import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.common.accessorytype.AccessoryType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AccessoryHelper.class, remap = false)
public abstract class AccessoryHelperTypeMixin {
    @Inject(method = "getType", at = @At("HEAD"), cancellable = true, remap = false)
    private static void totemAccessory263$forceTotemType(
            Item item,
            CallbackInfoReturnable<AccessoryType> cir
    ) {
        if (item == Items.TOTEM_OF_UNDYING) {
            cir.setReturnValue(AccessoryType.UTILITY.get());
        }
    }
}
