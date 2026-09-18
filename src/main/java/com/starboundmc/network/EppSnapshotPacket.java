// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EppSnapshotPacket(int oxygen, int capacity, int exposure, boolean equipped,
                                boolean airless, boolean refilling) implements CustomPacketPayload {
    public static final Type<EppSnapshotPacket> TYPE = PayloadSupport.type("epp_snapshot");
    public static final StreamCodec<FriendlyByteBuf, EppSnapshotPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EppSnapshotPacket::write, EppSnapshotPacket::new);
    private EppSnapshotPacket(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean()); }
    private void write(FriendlyByteBuf b) { b.writeVarInt(oxygen); b.writeVarInt(capacity); b.writeVarInt(exposure); b.writeBoolean(equipped); b.writeBoolean(airless); b.writeBoolean(refilling); }
    @Override public Type<EppSnapshotPacket> type() { return TYPE; }
}
