package com.starboundmc.network;

import com.starboundmc.world.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client notification that an authoritative warp has begun. */
public record WarpStartPacket(String planetId, int durationTicks, String entryId)
        implements CustomPacketPayload {
    public static final Type<WarpStartPacket> TYPE = PayloadSupport.type("warp_start");
    public static final StreamCodec<FriendlyByteBuf, WarpStartPacket> STREAM_CODEC =
            CustomPacketPayload.codec(WarpStartPacket::write, WarpStartPacket::new);

    public WarpStartPacket {
        planetId = PayloadSupport.requireString(planetId, PayloadSupport.MAX_ID_LENGTH, "planetId");
        entryId = PayloadSupport.requireString(
                entryId == null ? "" : entryId, PayloadSupport.MAX_ID_LENGTH, "entryId");
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
    }

    public WarpStartPacket(Planet planet, int durationTicks, String entryId) {
        this(planet.getId(), durationTicks, entryId);
    }

    private WarpStartPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH), buffer.readVarInt(),
                buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(planetId, PayloadSupport.MAX_ID_LENGTH);
        buffer.writeVarInt(durationTicks);
        buffer.writeUtf(entryId, PayloadSupport.MAX_ID_LENGTH);
    }

    @Override
    public Type<WarpStartPacket> type() {
        return TYPE;
    }
}
