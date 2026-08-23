package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server request to upgrade one matter-manipulator track. */
public record UpgradeMatterManipulatorPacket(byte track) implements CustomPacketPayload {
    public static final Type<UpgradeMatterManipulatorPacket> TYPE =
            PayloadSupport.type("upgrade_matter_manipulator");
    public static final StreamCodec<FriendlyByteBuf, UpgradeMatterManipulatorPacket> STREAM_CODEC =
            CustomPacketPayload.codec(UpgradeMatterManipulatorPacket::write,
                    UpgradeMatterManipulatorPacket::new);

    public UpgradeMatterManipulatorPacket(int track) {
        this((byte) track);
    }

    private UpgradeMatterManipulatorPacket(FriendlyByteBuf buffer) {
        this(buffer.readByte());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeByte(track);
    }

    @Override
    public Type<UpgradeMatterManipulatorPacket> type() {
        return TYPE;
    }
}
