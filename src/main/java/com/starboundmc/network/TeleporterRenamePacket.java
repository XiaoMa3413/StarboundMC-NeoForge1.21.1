package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to rename the currently open teleporter. */
public record TeleporterRenamePacket(String name) implements CustomPacketPayload {
    public static final Type<TeleporterRenamePacket> TYPE = PayloadSupport.type("teleporter_rename");
    public static final StreamCodec<FriendlyByteBuf, TeleporterRenamePacket> STREAM_CODEC =
            CustomPacketPayload.codec(TeleporterRenamePacket::write, TeleporterRenamePacket::new);

    public TeleporterRenamePacket {
        name = PayloadSupport.requireString(name, PayloadSupport.MAX_NAME_LENGTH, "name");
    }

    private TeleporterRenamePacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_NAME_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(name, PayloadSupport.MAX_NAME_LENGTH);
    }

    @Override
    public Type<TeleporterRenamePacket> type() {
        return TYPE;
    }
}
