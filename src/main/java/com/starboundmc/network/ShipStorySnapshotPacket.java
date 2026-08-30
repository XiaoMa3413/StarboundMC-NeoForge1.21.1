package com.starboundmc.network;

import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.SituationTopic;
import com.starboundmc.story.SurfaceMissionState;
import com.starboundmc.story.TutorialTopic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Objects;

/** Server-authoritative shared and owner-personal story snapshot for one open terminal. */
public record ShipStorySnapshotPacket(
        int containerId,
        long acknowledgedRequestId,
        int sharedSchemaVersion,
        long sharedRevision,
        CoreState core,
        SurfaceMissionState surfaceMission,
        EngineState sublightEngine,
        EngineState hyperdrive,
        int rebootTicksRemaining,
        int playerSchemaVersion,
        long playerRevision,
        boolean identityConfirmed,
        int readSituationMask,
        int tutorialMask,
        int dismissedHintMask) implements CustomPacketPayload
{
    private static final int MAX_STATE_ID_LENGTH = 24;

    public static final Type<ShipStorySnapshotPacket> TYPE =
            PayloadSupport.type("ship_story_snapshot");
    public static final StreamCodec<FriendlyByteBuf, ShipStorySnapshotPacket> STREAM_CODEC =
            CustomPacketPayload.codec(ShipStorySnapshotPacket::write, ShipStorySnapshotPacket::new);

    public ShipStorySnapshotPacket
    {
        if (containerId < 0 || acknowledgedRequestId < 0L
                || sharedSchemaVersion <= 0 || playerSchemaVersion <= 0
                || sharedRevision < 0L || playerRevision < 0L || rebootTicksRemaining < 0)
            throw new IllegalArgumentException("Story snapshot contains a negative or missing version field");
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(surfaceMission, "surfaceMission");
        Objects.requireNonNull(sublightEngine, "sublightEngine");
        Objects.requireNonNull(hyperdrive, "hyperdrive");
        if (readSituationMask < 0
                || (readSituationMask & ~SituationTopic.REQUIRED_MASK) != 0)
            throw new IllegalArgumentException("Unknown situation read bits " + readSituationMask);
        int knownTutorialMask = TutorialTopic.knownMask();
        if (tutorialMask < 0 || (tutorialMask & ~knownTutorialMask) != 0)
            throw new IllegalArgumentException("Unknown tutorial bits " + tutorialMask);
        if (dismissedHintMask < 0)
            throw new IllegalArgumentException("dismissedHintMask must be non-negative");
    }

    private ShipStorySnapshotPacket(FriendlyByteBuf buffer)
    {
        this(buffer.readVarInt(), buffer.readVarLong(),
                buffer.readVarInt(), buffer.readVarLong(),
                requireCore(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                requireMission(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                requireEngine(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                requireEngine(buffer.readUtf(MAX_STATE_ID_LENGTH)),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(),
                buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(acknowledgedRequestId);
        buffer.writeVarInt(sharedSchemaVersion);
        buffer.writeVarLong(sharedRevision);
        buffer.writeUtf(core.id(), MAX_STATE_ID_LENGTH);
        buffer.writeUtf(surfaceMission.id(), MAX_STATE_ID_LENGTH);
        buffer.writeUtf(sublightEngine.id(), MAX_STATE_ID_LENGTH);
        buffer.writeUtf(hyperdrive.id(), MAX_STATE_ID_LENGTH);
        buffer.writeVarInt(rebootTicksRemaining);
        buffer.writeVarInt(playerSchemaVersion);
        buffer.writeVarLong(playerRevision);
        buffer.writeBoolean(identityConfirmed);
        buffer.writeVarInt(readSituationMask);
        buffer.writeVarInt(tutorialMask);
        buffer.writeVarInt(dismissedHintMask);
    }

    @Override
    public Type<ShipStorySnapshotPacket> type()
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

    private static SurfaceMissionState requireMission(String id)
    {
        SurfaceMissionState state = SurfaceMissionState.fromId(id, null);
        if (state == null)
            throw new IllegalArgumentException("Unknown surface mission state " + id);
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
