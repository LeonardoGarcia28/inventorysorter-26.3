package dev.leonardogarcia.inventoryhudcompat.mixin;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory HUD+ 3.4.36 expects net.neoforged.fml.VersionChecker, removed/moved
 * in NeoForge 26.3.0.58-beta. Its update notification crashes on player join.
 *
 * Skip only that optional notification handler, keeping all HUD functionality.
 */
@Mixin(targets = "dlovin.inventoryhud.neoforge.events.NeoUpdateNotification", remap = false)
public abstract class DisableBrokenUpdateCheckMixin {
    @Inject(method = "onPlayerJoin", at = @At("HEAD"), cancellable = true, remap = false)
    private void inventoryHudCompat$skipLegacyVersionChecker(
            EntityJoinLevelEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
