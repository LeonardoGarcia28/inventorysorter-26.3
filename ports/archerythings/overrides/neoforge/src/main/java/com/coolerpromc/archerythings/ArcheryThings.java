package com.coolerpromc.archerythings;

import com.coolerpromc.archerythings.compat.OhmegaHelper;
import com.coolerpromc.archerythings.network.packet.ServerBoundQuiverMenuPacket;
import com.coolerpromc.archerythings.network.packet.ServerBoundSelectQuiverSlotPacket;
import com.coolerpromc.archerythings.platform.NeoForgeRegistryHelper;
import com.coolerpromc.archerythings.platform.Services;
import com.coolerpromc.archerythings.platform.util.NeoForgePayloadContext;
import com.coolerpromc.archerythings.util.AnvilHandler;
import com.coolerpromc.archerythings.util.ArrowHandler;
import com.coolerpromc.archerythings.util.UseItemHandler;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MODID)
public class ArcheryThings {
    public ArcheryThings(IEventBus modEventBus) {
        CommonClass.init();
        NeoForgeRegistryHelper.register(modEventBus);

        if (ModList.get().isLoaded("ohmega")) {
            OhmegaHelper.init(modEventBus);
        }

        modEventBus.addListener(this::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.register(this);
    }

    public void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ServerBoundQuiverMenuPacket.TYPE,
                ServerBoundQuiverMenuPacket.STREAM_CODEC,
                (payload, context) -> payload.handle(new NeoForgePayloadContext(context)));
        registrar.playToServer(
                ServerBoundSelectQuiverSlotPacket.TYPE,
                ServerBoundSelectQuiverSlotPacket.STREAM_CODEC,
                (payload, context) -> payload.handle(new NeoForgePayloadContext(context)));
        registrar.playToServer(
                com.coolerpromc.archerythings.network.packet.ServerBoundToggleQuiverPickupPacket.TYPE,
                com.coolerpromc.archerythings.network.packet.ServerBoundToggleQuiverPickupPacket.STREAM_CODEC,
                (payload, context) -> payload.handle(new NeoForgePayloadContext(context)));
    }

    @SubscribeEvent
    public void onLivingGetProjectile(LivingGetProjectileEvent event) {
        event.setProjectileItemStack(
                ArrowHandler.getProjectileFromQuiver(event.getEntity(), event.getProjectileItemStack()));
    }

    @SubscribeEvent
    public void onLivingEntityUseItem(PlayerInteractEvent.RightClickItem event) {
        InteractionResult result = UseItemHandler.onUseItem(event.getItemStack(), event.getEntity(), event.getHand());
        if (result == InteractionResult.SUCCESS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        ArrowHandler.onArrowLoose(event.getEntity(), event.getBow(), event.getLevel(), event.hasAmmo());
    }

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        AnvilHandler.AnvilUpdateResult result = AnvilHandler.onUpdate(left, right, event.getName());

        if (!result.output().isEmpty()) {
            event.setOutput(result.output());
            event.setMaterialCost(1);
            event.setXpCost(result.xpCost());
        }
    }

    @SubscribeEvent
    public void onGroundArrowPickup(ItemEntityPickupEvent.Pre event) {
        if (!Services.QUIVER.forcePickupToQuiver(event.getPlayer())) {
            return;
        }

        ItemStack groundStack = event.getItemEntity().getItem();
        if (!(groundStack.getItem() instanceof ArrowItem)) {
            return;
        }

        ItemStack quiver = Services.QUIVER.getQuiver(event.getPlayer());
        if (quiver.isEmpty()) {
            return;
        }

        // While the Ohmega Quiver is equipped, arrows never fall through into
        // the vanilla inventory. They fill the Quiver first; if full, they stay
        // on the ground.
        boolean inserted = ArrowHandler.insertIntoQuiver(quiver, groundStack);
        if (inserted) {
            Services.QUIVER.syncQuiver(event.getPlayer(), quiver);
        }
        event.setCanPickup(TriState.FALSE);

        if (groundStack.isEmpty()) {
            event.getItemEntity().discard();
        }
    }
}
