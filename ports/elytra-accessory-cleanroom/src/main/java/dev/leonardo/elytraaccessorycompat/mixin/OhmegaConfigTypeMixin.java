package dev.leonardo.elytraaccessorycompat.mixin;

import net.neoforged.fml.config.ModConfig;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.swacky.ohmega.common.Ohmega", remap = false)
public abstract class OhmegaConfigTypeMixin {
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/neoforged/fml/config/ModConfig$Type;SERVER:Lnet/neoforged/fml/config/ModConfig$Type;",
                    opcode = Opcodes.GETSTATIC
            ),
            remap = false,
            require = 1
    )
    private ModConfig.Type elytraAccessory263$redirectServerConfigType() {
        return ModConfig.Type.SYNCED;
    }
}
