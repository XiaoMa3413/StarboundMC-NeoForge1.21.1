package com.starboundmc.network;

import com.starboundmc.mobility.MobilityEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JumpBoostPacket(int forward, int sideways) implements CustomPacketPayload {
    public static final Type<JumpBoostPacket> TYPE = PayloadSupport.type("jump_boost");
    public static final StreamCodec<FriendlyByteBuf, JumpBoostPacket> STREAM_CODEC =
            CustomPacketPayload.codec((p, b) -> { b.writeByte(p.forward); b.writeByte(p.sideways); },
                    b -> new JumpBoostPacket(b.readByte(), b.readByte()));
    @Override public Type<JumpBoostPacket> type() { return TYPE; }
    public static void handle(JumpBoostPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) MobilityEvents.boost(player, packet.forward, packet.sideways);
    }
}
