package com.starboundmc.network;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** Server -> client versioned, authoritative virtual-flight snapshot. */
public final class SyncFlightPacket implements CustomPacketPayload {
    public static final Type<SyncFlightPacket> TYPE = PayloadSupport.type("sync_flight");
    public static final StreamCodec<FriendlyByteBuf, SyncFlightPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncFlightPacket::write, SyncFlightPacket::new);

    private final long revision;
    private final long serverTick;
    private final int phaseId;
    private final UniversePosition position;
    private final UniverseDelta velocity;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final int elapsedTicks;
    private final int totalTicks;
    private final String targetEntryId;

    public SyncFlightPacket(long revision, long serverTick, FlightPhase phase,
                            double x, double y, double z,
                            double vx, double vy, double vz,
                            double yaw, double pitch, double roll,
                            int elapsedTicks, int totalTicks, String targetEntryId) {
        this(revision, serverTick, phase,
                UniversePosition.fromLegacy(new Vec3(x, y, z)),
                new UniverseDelta(vx, vy, vz), yaw, pitch, roll,
                elapsedTicks, totalTicks, targetEntryId);
    }

    public SyncFlightPacket(long revision, long serverTick, FlightPhase phase,
                            UniversePosition position, UniverseDelta velocity,
                            double yaw, double pitch, double roll,
                            int elapsedTicks, int totalTicks, String targetEntryId) {
        this(revision, serverTick, phase.ordinal(), position, velocity,
                (float) yaw, (float) pitch, (float) roll,
                elapsedTicks, totalTicks, targetEntryId);
    }

    private SyncFlightPacket(long revision, long serverTick, int phaseId,
                             UniversePosition position, UniverseDelta velocity,
                             float yaw, float pitch, float roll,
                             int elapsedTicks, int totalTicks, String targetEntryId) {
        if (revision < 0 || serverTick < 0) {
            throw new IllegalArgumentException("Flight revision and server tick must be non-negative");
        }
        if (phaseId < 0 || phaseId >= FlightPhase.values().length) {
            throw new IllegalArgumentException("Unknown flight phase id " + phaseId);
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(roll)) {
            throw new IllegalArgumentException("Flight rotation must be finite");
        }
        if (elapsedTicks < 0 || totalTicks < 0 || elapsedTicks > totalTicks) {
            throw new IllegalArgumentException(
                    "Invalid flight progress " + elapsedTicks + "/" + totalTicks);
        }
        this.revision = revision;
        this.serverTick = serverTick;
        this.phaseId = phaseId;
        this.position = java.util.Objects.requireNonNull(position, "position");
        this.velocity = java.util.Objects.requireNonNull(velocity, "velocity");
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.elapsedTicks = elapsedTicks;
        this.totalTicks = totalTicks;
        this.targetEntryId = PayloadSupport.requireString(
                targetEntryId == null ? "" : targetEntryId,
                PayloadSupport.MAX_ID_LENGTH, "targetEntryId");
    }

    private SyncFlightPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarLong(), buffer.readVarLong(), buffer.readVarInt(),
                UniversePosition.of(
                        new SectorCoordinate(buffer.readLong(), buffer.readLong(), buffer.readLong()),
                        buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new UniverseDelta(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(PayloadSupport.MAX_ID_LENGTH));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeVarLong(serverTick);
        buffer.writeVarInt(phaseId);
        buffer.writeLong(position.sector().x());
        buffer.writeLong(position.sector().y());
        buffer.writeLong(position.sector().z());
        buffer.writeDouble(position.localX());
        buffer.writeDouble(position.localY());
        buffer.writeDouble(position.localZ());
        buffer.writeDouble(velocity.x());
        buffer.writeDouble(velocity.y());
        buffer.writeDouble(velocity.z());
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(roll);
        buffer.writeVarInt(elapsedTicks);
        buffer.writeVarInt(totalTicks);
        buffer.writeUtf(targetEntryId, PayloadSupport.MAX_ID_LENGTH);
    }

    @Override
    public Type<SyncFlightPacket> type() {
        return TYPE;
    }

    public long revision() {
        return revision;
    }

    public long serverTick() {
        return serverTick;
    }

    public FlightPhase phase() {
        return FlightPhase.byNetworkId(phaseId);
    }

    public int phaseId() {
        return phaseId;
    }

    public UniversePosition position() {
        return position;
    }

    public UniverseDelta velocity() {
        return velocity;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public String targetEntryId() {
        return targetEntryId;
    }
}
