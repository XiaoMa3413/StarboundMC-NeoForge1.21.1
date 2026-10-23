package com.starboundmc.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Only button intent crosses the wire; all timings and hits belong to the server. */
public record GunTriggerPacket(int action) implements CustomPacketPayload {
    public static final int PRESS = 0, RELEASE = 1, CANCEL = 2;
    public static final Type<GunTriggerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("starboundmc", "gun_trigger"));
    public static final StreamCodec<FriendlyByteBuf, GunTriggerPacket> STREAM_CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> buffer.writeByte(packet.action), buffer -> new GunTriggerPacket(buffer.readUnsignedByte()));
    @Override public Type<GunTriggerPacket> type() { return TYPE; }
    public static void handle(GunTriggerPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) GunTrigger.input(player, packet.action);
    }
}
