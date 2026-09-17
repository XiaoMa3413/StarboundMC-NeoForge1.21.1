package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client notification that an authoritative warp has begun.
 *
 * <p>Carries the destination as a universe entry id. It used to carry a legacy
 * planet name as well, which the client decoded through the planet enum; that
 * field is gone, so the destination cannot be misread as the starter planet when
 * a body has no legacy name (migration §21).</p>
 */
public record WarpStartPacket(String entryId, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<WarpStartPacket> TYPE = PayloadSupport.type("warp_start");
    public static final StreamCodec<FriendlyByteBuf, WarpStartPacket> STREAM_CODEC =
            CustomPacketPayload.codec(WarpStartPacket::write, WarpStartPacket::new);

    public WarpStartPacket {
        entryId = PayloadSupport.requireString(entryId, PayloadSupport.MAX_ID_LENGTH, "entryId");
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
    }

    private WarpStartPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(entryId, PayloadSupport.MAX_ID_LENGTH);
        buffer.writeVarInt(durationTicks);
    }

    @Override
    public Type<WarpStartPacket> type() {
        return TYPE;
    }
}
