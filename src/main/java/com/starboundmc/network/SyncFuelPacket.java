package com.starboundmc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client authoritative ship fuel snapshot. */
public record SyncFuelPacket(int fuel, int maxFuel) implements CustomPacketPayload {
    public static final Type<SyncFuelPacket> TYPE = PayloadSupport.type("sync_fuel");
    public static final StreamCodec<FriendlyByteBuf, SyncFuelPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncFuelPacket::write, SyncFuelPacket::new);

    public SyncFuelPacket {
        if (maxFuel <= 0 || fuel < 0 || fuel > maxFuel) {
            throw new IllegalArgumentException("Invalid fuel snapshot " + fuel + "/" + maxFuel);
        }
    }

    private SyncFuelPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(fuel);
        buffer.writeVarInt(maxFuel);
    }

    @Override
    public Type<SyncFuelPacket> type() {
        return TYPE;
    }
}
