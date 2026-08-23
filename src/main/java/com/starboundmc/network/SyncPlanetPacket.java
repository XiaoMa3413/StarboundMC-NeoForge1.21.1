package com.starboundmc.network;

import com.starboundmc.world.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client current-orbit snapshot. */
public record SyncPlanetPacket(String planetId) implements CustomPacketPayload {
    public static final Type<SyncPlanetPacket> TYPE = PayloadSupport.type("sync_planet");
    public static final StreamCodec<FriendlyByteBuf, SyncPlanetPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncPlanetPacket::write, SyncPlanetPacket::new);

    public SyncPlanetPacket {
        planetId = PayloadSupport.requireString(planetId, PayloadSupport.MAX_ID_LENGTH, "planetId");
    }

    public SyncPlanetPacket(Planet planet) {
        this(planet.getId());
    }

    private SyncPlanetPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(planetId, PayloadSupport.MAX_ID_LENGTH);
    }

    @Override
    public Type<SyncPlanetPacket> type() {
        return TYPE;
    }
}
