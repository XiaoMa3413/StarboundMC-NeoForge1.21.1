package com.starboundmc.network;

import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Objects;

/** Server-authoritative ship environment state for one open navigation menu. */
public record ShipEnvironmentSnapshotPacket(
        int containerId,
        int schemaVersion,
        long revision,
        CoreState core,
        EngineState sublightEngine,
        EngineState hyperdrive,
        int rebootTicksRemaining) implements CustomPacketPayload
{
    private static final int MAX_STATE_ID_LENGTH = 24;

    public static final Type<ShipEnvironmentSnapshotPacket> TYPE =
            PayloadSupport.type("ship_environment_snapshot");
    public static final StreamCodec<FriendlyByteBuf, ShipEnvironmentSnapshotPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ShipEnvironmentSnapshotPacket::write,
                    ShipEnvironmentSnapshotPacket::new);

    public ShipEnvironmentSnapshotPacket
    {
        if (containerId < 0 || schemaVersion <= 0 || revision < 0L
                || rebootTicksRemaining < 0)
            throw new IllegalArgumentException("Environment snapshot contains an invalid version field");
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(sublightEngine, "sublightEngine");
        Objects.requireNonNull(hyperdrive, "hyperdrive");
    }

    private ShipEnvironmentSnapshotPacket(FriendlyByteBuf buffer)
    {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(),
                requireCore(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                requireEngine(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                requireEngine(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(containerId);
        buffer.writeVarInt(schemaVersion);
        buffer.writeVarLong(revision);
        buffer.writeUtf(core.id(), MAX_STATE_ID_LENGTH);
        buffer.writeUtf(sublightEngine.id(), MAX_STATE_ID_LENGTH);
        buffer.writeUtf(hyperdrive.id(), MAX_STATE_ID_LENGTH);
        buffer.writeVarInt(rebootTicksRemaining);
    }

    @Override
    public Type<ShipEnvironmentSnapshotPacket> type()
    {
        return TYPE;
    }

    private static CoreState requireCore(String id)
    {
        CoreState state = CoreState.fromId(id, null);
        if (state == null)
            throw new IllegalArgumentException("Unknown core state " + id);
        return state;
    }

    private static EngineState requireEngine(String id)
    {
        EngineState state = EngineState.fromId(id, null);
        if (state == null)
            throw new IllegalArgumentException("Unknown engine state " + id);
        return state;
    }
}
