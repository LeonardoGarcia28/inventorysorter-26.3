package dev.leonardo.totemaccessorycompat.mixin;

import dev.leonardo.totemaccessorycompat.TotemAccessoryCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.swacky.ohmega.config.OhmegaConfig$Server", remap = false)
public abstract class OhmegaSlotTypesMixin {
    @Inject(method = "slotTypes", at = @At("RETURN"), cancellable = true, remap = false)
    private static void totemAccessory263$appendTotemSlot(CallbackInfoReturnable<List<String>> cir) {
        List<String> original = cir.getReturnValue();
        String totemId = TotemAccessoryCompat.TOTEM_TYPE_ID.toString();

        if (original.contains(totemId)) {
            return;
        }

        ArrayList<String> extended = new ArrayList<>(original);
        extended.add(totemId);
        cir.setReturnValue(List.copyOf(extended));
    }
}
