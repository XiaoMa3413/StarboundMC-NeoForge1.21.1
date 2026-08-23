package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Versioned server-authoritative virtual-flight snapshot. */
public final class SyncFlightPacket
{
    private final long revision;
    private final long serverTick;
    private final int phaseId;
    private final UniversePosition position;
    private final UniverseDelta velocity;
    private final float yaw, pitch, roll;
    private final int elapsedTicks, totalTicks;
    private final String targetEntryId;

    public SyncFlightPacket(long revision, long serverTick, FlightPhase phase,
                            double x, double y, double z, double vx, double vy, double vz,
                            double yaw, double pitch, double roll, int elapsedTicks, int totalTicks, String targetEntryId)
    {
        this(revision, serverTick, phase,
                UniversePosition.fromLegacy(new Vec3(x, y, z)), new UniverseDelta(vx, vy, vz),
                yaw, pitch, roll, elapsedTicks, totalTicks, targetEntryId);
    }

    public SyncFlightPacket(long revision, long serverTick, FlightPhase phase,
                            UniversePosition position, UniverseDelta velocity,
                            double yaw, double pitch, double roll, int elapsedTicks, int totalTicks, String targetEntryId)
    {
        this(revision, serverTick, phase.ordinal(), position, velocity, (float) yaw, (float) pitch, (float) roll,
                elapsedTicks, totalTicks, targetEntryId);
    }

    private SyncFlightPacket(long revision, long serverTick, int phaseId,
                             UniversePosition position, UniverseDelta velocity,
                             float yaw, float pitch, float roll, int elapsedTicks, int totalTicks, String targetEntryId)
    {
        this.revision = revision; this.serverTick = serverTick; this.phaseId = phaseId;
        this.position = java.util.Objects.requireNonNull(position, "position");
        this.velocity = java.util.Objects.requireNonNull(velocity, "velocity");
        this.yaw = yaw; this.pitch = pitch; this.roll = roll;
        this.elapsedTicks = elapsedTicks; this.totalTicks = totalTicks;
        this.targetEntryId = targetEntryId == null ? "" : targetEntryId;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarLong(revision); buf.writeVarLong(serverTick); buf.writeVarInt(phaseId);
        buf.writeLong(position.sector().x()); buf.writeLong(position.sector().y()); buf.writeLong(position.sector().z());
        buf.writeDouble(position.localX()); buf.writeDouble(position.localY()); buf.writeDouble(position.localZ());
        buf.writeDouble(velocity.x()); buf.writeDouble(velocity.y()); buf.writeDouble(velocity.z());
        buf.writeFloat(yaw); buf.writeFloat(pitch); buf.writeFloat(roll);
        buf.writeVarInt(elapsedTicks); buf.writeVarInt(totalTicks); buf.writeUtf(targetEntryId, 128);
    }

    public static SyncFlightPacket decode(FriendlyByteBuf buf)
    {
        long revision = buf.readVarLong();
        long serverTick = buf.readVarLong();
        int phaseId = buf.readVarInt();
        UniversePosition position = UniversePosition.of(
                new SectorCoordinate(buf.readLong(), buf.readLong(), buf.readLong()),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
        UniverseDelta velocity = new UniverseDelta(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new SyncFlightPacket(revision, serverTick, phaseId, position, velocity,
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128));
    }

    public static void handle(SyncFlightPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> ClientPlanetState.applyFlightSnapshot(msg.revision, msg.serverTick,
                FlightPhase.byNetworkId(msg.phaseId), msg.position, msg.velocity,
                msg.yaw, msg.pitch, msg.roll, msg.elapsedTicks, msg.totalTicks,
                msg.targetEntryId.isEmpty() ? null : msg.targetEntryId));
        ctx.get().setPacketHandled(true);
    }

    public UniversePosition position()
    {
        return position;
    }

    public UniverseDelta velocity()
    {
        return velocity;
    }
}
