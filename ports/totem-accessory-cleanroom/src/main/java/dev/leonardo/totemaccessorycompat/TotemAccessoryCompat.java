package dev.leonardo.totemaccessorycompat;

import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.api.IAccessory;
import com.swacky.ohmega.api.event.AccessoryOverrideTypesEvent;
import com.swacky.ohmega.common.accessorytype.AccessoryTypeManager;
import com.swacky.ohmega.common.dataattachment.AccessoryContainer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@Mod(TotemAccessoryCompat.MOD_ID)
public final class TotemAccessoryCompat {
    public static final String MOD_ID = "totem_accessory_263";
    public static final Identifier TOTEM_TYPE_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "totem");

    private static final IAccessory TOTEM_ACCESSORY = new IAccessory() {};

    public TotemAccessoryCompat(IEventBus modBus) {
        AccessoryHelper.bindAccessory(Items.TOTEM_OF_UNDYING, TOTEM_ACCESSORY);

        modBus.addListener(TotemAccessoryCompat::overrideAccessoryType);
        NeoForge.EVENT_BUS.addListener(TotemAccessoryCompat::onLivingDeath);
    }

    private static void overrideAccessoryType(AccessoryOverrideTypesEvent event) {
        event.overrideRemaps.put(
                Items.TOTEM_OF_UNDYING,
                com.swacky.ohmega.common.accessorytype.AccessoryType.UTILITY.get()
        );
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        AccessoryContainer container = AccessoryHelper.getContainer(player);

        for (int slot = 0; slot < container.getSlots(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (!stack.is(Items.TOTEM_OF_UNDYING)) {
                continue;
            }

            DeathProtection protection = stack.get(DataComponents.DEATH_PROTECTION);
            if (protection == null) {
                continue;
            }

            ItemStack usedTotem = stack.copy();
            stack.shrink(1);
            container.onContentsChanged(slot);

            player.awardStat(Stats.ITEM_USED.get(usedTotem.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(player, usedTotem);
            usedTotem.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);

            player.setHealth(1.0F);
            protection.applyEffects(usedTotem, player);
            player.level().broadcastEntityEvent(player, (byte) 35);

            event.setCanceled(true);
            return;
        }
    }
}
