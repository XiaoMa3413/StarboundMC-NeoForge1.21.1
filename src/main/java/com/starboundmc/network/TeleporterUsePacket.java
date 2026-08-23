package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request for a destination in the currently open teleporter. */
public record TeleporterUsePacket(String key) implements CustomPacketPayload {
    public static final Type<TeleporterUsePacket> TYPE = PayloadSupport.type("teleporter_use");
    public static final StreamCodec<FriendlyByteBuf, TeleporterUsePacket> STREAM_CODEC =
            CustomPacketPayload.codec(TeleporterUsePacket::write, TeleporterUsePacket::new);

    public TeleporterUsePacket {
        key = PayloadSupport.requireString(key, PayloadSupport.MAX_ID_LENGTH, "key");
    }

    private TeleporterUsePacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(key, PayloadSupport.MAX_ID_LENGTH);
    }

    @Override
    public Type<TeleporterUsePacket> type() {
        return TYPE;
    }
}
