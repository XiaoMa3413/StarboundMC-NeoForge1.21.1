// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;
import net.minecraft.core.BlockPos;
import com.starboundmc.encounter.RelayData.Phase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record RelaySnapshotPacket(Phase phase, BlockPos origin, int ticks, String homeBody, boolean recovered, boolean completed, int outsideCrew) implements CustomPacketPayload {
    public RelaySnapshotPacket {
        java.util.Objects.requireNonNull(phase, "phase");
        origin = java.util.Objects.requireNonNull(origin, "origin").immutable();
        homeBody = PayloadSupport.requireString(homeBody, 128, "homeBody");
        if (ticks < 0 || ticks > com.starboundmc.encounter.RelayEncounter.APPROACH_TICKS || outsideCrew < 0
                || completed && !recovered) throw new IllegalArgumentException("Invalid relay snapshot");
    }
    public static final Type<RelaySnapshotPacket> TYPE = PayloadSupport.type("relay_snapshot");
    public static final StreamCodec<FriendlyByteBuf, RelaySnapshotPacket> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.phase.networkId()); b.writeBlockPos(p.origin); b.writeVarInt(p.ticks); b.writeUtf(p.homeBody, 128); b.writeBoolean(p.recovered); b.writeBoolean(p.completed); b.writeVarInt(p.outsideCrew); },
            b -> new RelaySnapshotPacket(Phase.byNetworkId(b.readVarInt()), b.readBlockPos(), b.readVarInt(), b.readUtf(128), b.readBoolean(), b.readBoolean(), b.readVarInt()));
    @Override public Type<RelaySnapshotPacket> type() { return TYPE; }
}
