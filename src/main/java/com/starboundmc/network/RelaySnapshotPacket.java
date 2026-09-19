// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record RelaySnapshotPacket(int phase, BlockPos origin, int ticks, String homeBody, boolean recovered, boolean completed, int outsideCrew) implements CustomPacketPayload {
    public static final Type<RelaySnapshotPacket> TYPE = PayloadSupport.type("relay_snapshot");
    public static final StreamCodec<FriendlyByteBuf, RelaySnapshotPacket> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.phase); b.writeBlockPos(p.origin); b.writeVarInt(p.ticks); b.writeUtf(p.homeBody, 128); b.writeBoolean(p.recovered); b.writeBoolean(p.completed); b.writeVarInt(p.outsideCrew); },
            b -> new RelaySnapshotPacket(b.readVarInt(), b.readBlockPos(), b.readVarInt(), b.readUtf(128), b.readBoolean(), b.readBoolean(), b.readVarInt()));
    @Override public Type<RelaySnapshotPacket> type() { return TYPE; }
}
