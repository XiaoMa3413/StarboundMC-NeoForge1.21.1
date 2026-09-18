// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EvaStatePacket(ResourceLocation dimension, int mode) implements CustomPacketPayload {
    public static final Type<EvaStatePacket> TYPE = PayloadSupport.type("eva_state");
    public static final StreamCodec<FriendlyByteBuf, EvaStatePacket> STREAM_CODEC = CustomPacketPayload.codec(
            (p, b) -> { b.writeResourceLocation(p.dimension); b.writeByte(p.mode); },
            b -> new EvaStatePacket(b.readResourceLocation(), b.readUnsignedByte()));
    @Override public Type<EvaStatePacket> type() { return TYPE; }
}
