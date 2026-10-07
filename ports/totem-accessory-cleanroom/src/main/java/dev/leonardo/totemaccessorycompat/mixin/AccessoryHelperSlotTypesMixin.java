package dev.leonardo.totemaccessorycompat.mixin;

import com.google.common.collect.ImmutableList;
import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.common.accessorytype.AccessoryType;
import com.swacky.ohmega.common.accessorytype.AccessoryTypeManager;
import dev.leonardo.totemaccessorycompat.TotemAccessoryCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AccessoryHelper.class, remap = false)
public abstract class AccessoryHelperSlotTypesMixin {
    @Inject(method = "getSlotTypes", at = @At("RETURN"), cancellable = true, remap = false)
    private static void totemAccessory263$appendDedicatedTotemSlot(
            CallbackInfoReturnable<ImmutableList<AccessoryType>> cir
    ) {
        ImmutableList<AccessoryType> original = cir.getReturnValue();
        AccessoryType totemType = AccessoryTypeManager.get(TotemAccessoryCompat.TOTEM_TYPE_ID);

        for (AccessoryType type : original) {
            if (type.getId().equals(TotemAccessoryCompat.TOTEM_TYPE_ID)) {
                return;
            }
        }

        ImmutableList.Builder<AccessoryType> builder =
                ImmutableList.builderWithExpectedSize(original.size() + 1);
        builder.addAll(original);
        builder.add(totemType);
        cir.setReturnValue(builder.build());
    }
}
