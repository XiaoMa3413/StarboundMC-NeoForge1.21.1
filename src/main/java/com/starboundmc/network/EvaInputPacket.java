// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import com.starboundmc.epp.EvaMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EvaInputPacket(int input) implements CustomPacketPayload {
    public static final Type<EvaInputPacket> TYPE = PayloadSupport.type("eva_input");
    public static final StreamCodec<FriendlyByteBuf, EvaInputPacket> STREAM_CODEC =
            CustomPacketPayload.codec((p, b) -> b.writeByte(p.input), b -> new EvaInputPacket(b.readUnsignedByte()));
    @Override public Type<EvaInputPacket> type() { return TYPE; }
    public static void handle(EvaInputPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) EvaMovement.acceptInput(player, packet.input);
    }
}
