// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record RelayActionPacket(int containerId, boolean leave) implements CustomPacketPayload {
    public static final Type<RelayActionPacket> TYPE = PayloadSupport.type("relay_action");
    public static final StreamCodec<FriendlyByteBuf, RelayActionPacket> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeVarInt(p.containerId); b.writeBoolean(p.leave); }, b -> new RelayActionPacket(b.readVarInt(), b.readBoolean()));
    @Override public Type<RelayActionPacket> type() { return TYPE; }
    public static void handle(RelayActionPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) com.starboundmc.encounter.RelayEncounter.request(player, packet.containerId, packet.leave);
    }
}
