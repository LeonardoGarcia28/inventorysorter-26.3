package dev.leonardo.elytraaccessorycompat;

import com.swacky.ohmega.api.AccessoryHelper;
import com.swacky.ohmega.api.IAccessory;
import com.swacky.ohmega.api.event.AccessoryOverrideTypesEvent;
import com.swacky.ohmega.common.accessorytype.AccessoryType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod(ElytraAccessoryCompat.MOD_ID)
public final class ElytraAccessoryCompat {
    public static final String MOD_ID = "elytra_accessory_263";

    private static final Identifier GLIDE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "elytra_gliding_flight");

    private static final AttributeModifier GLIDE_MODIFIER = new AttributeModifier(
            GLIDE_MODIFIER_ID,
            1.0D,
            AttributeModifier.Operation.ADD_VALUE
    );

    // Server-side durability watcher. Weak keys avoid retaining disconnected players.
    // We only send a sync when the numeric damage value actually changes.
    private static final Map<ServerPlayer, Integer> LAST_SYNCED_DAMAGE = new WeakHashMap<>();

    private static final IAccessory ELYTRA_ACCESSORY = new IAccessory() {
        @Override
        public void tick(@NonNull Player player, @NonNull ItemStack stack) {
            updateGlidingAttribute(player, stack);

            if (player instanceof ServerPlayer serverPlayer) {
                syncDurabilityIfChanged(serverPlayer, stack);
            }

            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(player.level() instanceof ServerLevel serverLevel)
                    || !player.isFallFlying()
                    || player.tickCount % 20 != 0
                    || hasVanillaEquipmentGlider(player)
                    || stack.nextDamageWillBreak()) {
                return;
            }

            stack.hurtAndBreak(1, serverLevel, serverPlayer, broken -> {});

            syncDurabilityIfChanged(serverPlayer, stack);
            updateGlidingAttribute(player, stack);
        }

        @Override
        public void onEquip(@NonNull Player player, @NonNull ItemStack stack) {
            updateGlidingAttribute(player, stack);

            if (player instanceof ServerPlayer serverPlayer) {
                LAST_SYNCED_DAMAGE.put(serverPlayer, stack.getDamageValue());
            }
        }

        @Override
        public void onUnequip(@NonNull Player player, @NonNull ItemStack stack) {
            removeGlidingAttribute(player);

            if (player instanceof ServerPlayer serverPlayer) {
                LAST_SYNCED_DAMAGE.remove(serverPlayer);
            }
        }

        @Override
        public net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_ELYTRA;
        }
    };

    public ElytraAccessoryCompat(IEventBus modBus) {
        AccessoryHelper.bindAccessory(Items.ELYTRA, ELYTRA_ACCESSORY);
        modBus.addListener(ElytraAccessoryCompat::overrideAccessoryType);
    }

    private static void overrideAccessoryType(AccessoryOverrideTypesEvent event) {
        event.overrideRemaps.put(Items.ELYTRA, AccessoryType.SPECIAL.get());
    }

    private static void syncDurabilityIfChanged(ServerPlayer player, ItemStack stack) {
        int currentDamage = stack.getDamageValue();
        Integer previousDamage = LAST_SYNCED_DAMAGE.put(player, currentDamage);

        if (previousDamage == null || previousDamage == currentDamage) {
            return;
        }

        int slot = AccessoryHelper.getSlot(stack);
        if (slot < 0) {
            return;
        }

        // Mark the accessory slot dirty for Ohmega's normal bookkeeping...
        AccessoryHelper.getContainer(player).onContentsChanged(slot);

        // ...and send the changed stack immediately to the owner so the HUD
        // durability bar reacts to Mending/repairs without opening Ohmega.
        AccessoryHelper.syncSlots(
                player,
                new int[]{slot},
                List.of(stack.copy()),
                List.of(player)
        );
    }

    private static void updateGlidingAttribute(Player player, ItemStack stack) {
        var attribute = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attribute == null) {
            return;
        }

        boolean usable = !stack.isEmpty() && !stack.nextDamageWillBreak();
        boolean hasModifier = attribute.hasModifier(GLIDE_MODIFIER_ID);

        if (usable && !hasModifier) {
            attribute.addTransientModifier(GLIDE_MODIFIER);
        } else if (!usable && hasModifier) {
            attribute.removeModifier(GLIDE_MODIFIER_ID);
        }
    }

    private static void removeGlidingAttribute(Player player) {
        var attribute = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attribute != null && attribute.hasModifier(GLIDE_MODIFIER_ID)) {
            attribute.removeModifier(GLIDE_MODIFIER_ID);
        }
    }

    private static boolean hasVanillaEquipmentGlider(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
                return true;
            }
        }

        return false;
    }
}
