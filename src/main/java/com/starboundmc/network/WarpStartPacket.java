package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server notification of a new warp, carrying the canonical destination ID and route duration. */
public record WarpStartPacket(String entryId, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<WarpStartPacket> TYPE = PayloadSupport.type("warp_start");
    public static final StreamCodec<FriendlyByteBuf, WarpStartPacket> STREAM_CODEC =
            CustomPacketPayload.codec(WarpStartPacket::write, WarpStartPacket::new);

    public WarpStartPacket {
        entryId = PayloadSupport.requireString(entryId, PayloadSupport.MAX_ID_LENGTH, "entryId");
        if (entryId.isBlank() || durationTicks <= 0) {
            throw new IllegalArgumentException("Warp target must be nonempty and duration positive");
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
