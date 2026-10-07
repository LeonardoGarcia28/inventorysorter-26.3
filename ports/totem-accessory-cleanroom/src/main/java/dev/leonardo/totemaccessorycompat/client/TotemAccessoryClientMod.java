package dev.leonardo.totemaccessorycompat.client;

import dev.leonardo.totemaccessorycompat.TotemAccessoryCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TotemAccessoryCompat.MOD_ID, dist = Dist.CLIENT)
public final class TotemAccessoryClientMod {
    public TotemAccessoryClientMod(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
