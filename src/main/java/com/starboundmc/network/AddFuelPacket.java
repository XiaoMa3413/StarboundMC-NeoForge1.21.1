package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to consume valid fuel from the open controller menu. */
public record AddFuelPacket() implements CustomPacketPayload {
    public static final Type<AddFuelPacket> TYPE = PayloadSupport.type("add_fuel");
    public static final StreamCodec<FriendlyByteBuf, AddFuelPacket> STREAM_CODEC =
            StreamCodec.unit(new AddFuelPacket());

    @Override
    public Type<AddFuelPacket> type() {
        return TYPE;
    }
}
