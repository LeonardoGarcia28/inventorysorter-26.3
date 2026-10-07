package com.coolerpromc.archerythings.network.packet;

import com.coolerpromc.archerythings.Constants;
import com.coolerpromc.archerythings.component.ModDataComponents;
import com.coolerpromc.archerythings.component.data.QuiverData;
import com.coolerpromc.archerythings.platform.Services;
import com.coolerpromc.archerythings.platform.util.PayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record ClientBoundQuiverSyncPacket(QuiverData data, int selected)
        implements CustomPacketPayload {

    public static final Type<ClientBoundQuiverSyncPacket> TYPE =
            new Type<>(Constants.id("sync_quiver_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundQuiverSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    QuiverData.STREAM_CODEC,
                    ClientBoundQuiverSyncPacket::data,
                    ByteBufCodecs.INT,
                    ClientBoundQuiverSyncPacket::selected,
                    ClientBoundQuiverSyncPacket::new
            );

    public void handle(PayloadContext context) {
        context.execute(() -> {
            if (!Services.QUIVER.isQuiverEquipped(context.player())) {
                return;
            }

            ItemStack quiver = Services.QUIVER.getQuiver(context.player());
            if (quiver.isEmpty()) {
                return;
            }

            quiver.set(ModDataComponents.QUIVER_DATA.get(), this.data);
            quiver.set(ModDataComponents.SELECTED.get(), this.selected);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
