// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EppSnapshotPacket(int oxygen, int capacity, int exposure, boolean equipped,
                                boolean airless, boolean refilling, int generation,
                                int coldExposure, int coldTier, int coldProtection,
                                int heatExposure, int heatTier, int heatProtection) implements CustomPacketPayload {
    public static final Type<EppSnapshotPacket> TYPE = PayloadSupport.type("epp_snapshot");
    public static final StreamCodec<FriendlyByteBuf, EppSnapshotPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EppSnapshotPacket::write, EppSnapshotPacket::new);
    private EppSnapshotPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }
    private void write(FriendlyByteBuf b) {
        b.writeVarInt(oxygen); b.writeVarInt(capacity); b.writeVarInt(exposure);
        b.writeBoolean(equipped); b.writeBoolean(airless); b.writeBoolean(refilling);
        b.writeVarInt(generation); b.writeVarInt(coldExposure); b.writeVarInt(coldTier); b.writeVarInt(coldProtection);
        b.writeVarInt(heatExposure); b.writeVarInt(heatTier); b.writeVarInt(heatProtection);
    }
    @Override public Type<EppSnapshotPacket> type() { return TYPE; }
}
