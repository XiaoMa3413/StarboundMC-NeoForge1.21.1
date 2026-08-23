package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to return to the ship teleporter. */
public record TeleportToShipPacket() implements CustomPacketPayload {
    public static final Type<TeleportToShipPacket> TYPE = PayloadSupport.type("teleport_to_ship");
    public static final StreamCodec<FriendlyByteBuf, TeleportToShipPacket> STREAM_CODEC =
            StreamCodec.unit(new TeleportToShipPacket());

    @Override
    public Type<TeleportToShipPacket> type() {
        return TYPE;
    }
}
