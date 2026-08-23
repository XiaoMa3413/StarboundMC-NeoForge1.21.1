package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request for a star-map destination. */
public record StartWarpPacket(String entryId) implements CustomPacketPayload {
    public static final Type<StartWarpPacket> TYPE = PayloadSupport.type("start_warp");
    public static final StreamCodec<FriendlyByteBuf, StartWarpPacket> STREAM_CODEC =
            CustomPacketPayload.codec(StartWarpPacket::write, StartWarpPacket::new);

    public StartWarpPacket {
        entryId = PayloadSupport.requireString(entryId, PayloadSupport.MAX_ID_LENGTH, "entryId");
    }

    private StartWarpPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(entryId, PayloadSupport.MAX_ID_LENGTH);
    }

    @Override
    public Type<StartWarpPacket> type() {
        return TYPE;
    }
}
