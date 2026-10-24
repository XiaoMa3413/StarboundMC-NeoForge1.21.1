package com.starboundmc.weapon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cosmetic acknowledgement of a server-authorized shot. */
public record GunShotPacket(ResourceLocation item, int slot) implements CustomPacketPayload {
    public static final Type<GunShotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("starboundmc", "gun_shot"));
    public static final StreamCodec<FriendlyByteBuf, GunShotPacket> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeResourceLocation(p.item); b.writeVarInt(p.slot); },
            b -> new GunShotPacket(b.readResourceLocation(), b.readVarInt()));
    @Override public Type<GunShotPacket> type() { return TYPE; }
}
