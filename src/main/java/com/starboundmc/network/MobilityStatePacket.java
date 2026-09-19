package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MobilityStatePacket(boolean equipped, boolean charged) implements CustomPacketPayload {
    public static final Type<MobilityStatePacket> TYPE = PayloadSupport.type("mobility_state");
    public static final StreamCodec<FriendlyByteBuf, MobilityStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec((p, b) -> { b.writeBoolean(p.equipped); b.writeBoolean(p.charged); },
                    b -> new MobilityStatePacket(b.readBoolean(), b.readBoolean()));
    @Override public Type<MobilityStatePacket> type() { return TYPE; }
}
