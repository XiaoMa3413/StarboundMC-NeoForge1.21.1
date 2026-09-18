// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EppVisualPacket(int entityId, boolean equipped) implements CustomPacketPayload {
    public static final Type<EppVisualPacket> TYPE = PayloadSupport.type("epp_visual");
    public static final StreamCodec<FriendlyByteBuf, EppVisualPacket> STREAM_CODEC =
            CustomPacketPayload.codec(EppVisualPacket::write, EppVisualPacket::new);
    private EppVisualPacket(FriendlyByteBuf b) { this(b.readVarInt(), b.readBoolean()); }
    private void write(FriendlyByteBuf b) { b.writeVarInt(entityId); b.writeBoolean(equipped); }
    @Override public Type<EppVisualPacket> type() { return TYPE; }
}
