package dev.leonardogarcia.inventoryhudcompat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Unofficial compatibility add-on for Inventory HUD+ on NeoForge 26.3.
 * Does not modify or contain code from the original Inventory HUD+ jar.
 */
@Mod(InventoryHudCompat.MOD_ID)
public final class InventoryHudCompat {
    public static final String MOD_ID = "inventoryhudcompat";

    public InventoryHudCompat(IEventBus modEventBus, ModContainer modContainer) {
        // Mixin registered by neoforge.mods.toml; no additional events needed.
    }
}
