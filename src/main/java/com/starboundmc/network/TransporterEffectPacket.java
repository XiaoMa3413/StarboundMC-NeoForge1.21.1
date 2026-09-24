package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** A successful travel cue, scoped to one dimension; never a request to teleport. */
public record TransporterEffectPacket(ResourceLocation dimension, Vec3 position, boolean device, boolean arrival)
        implements CustomPacketPayload {
    public static final Type<TransporterEffectPacket> TYPE = PayloadSupport.type("transporter_effect");
    public static final StreamCodec<FriendlyByteBuf, TransporterEffectPacket> STREAM_CODEC =
            CustomPacketPayload.codec(TransporterEffectPacket::write, TransporterEffectPacket::new);
    private TransporterEffectPacket(FriendlyByteBuf b) {
        this(b.readResourceLocation(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readBoolean(), b.readBoolean());
    }
    private void write(FriendlyByteBuf b) {
        b.writeResourceLocation(dimension);
        b.writeDouble(position.x); b.writeDouble(position.y); b.writeDouble(position.z);
        b.writeBoolean(device); b.writeBoolean(arrival);
    }
    @Override public Type<TransporterEffectPacket> type() { return TYPE; }
}
