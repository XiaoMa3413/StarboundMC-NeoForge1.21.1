package com.starboundmc.client.space;

import com.starboundmc.client.StarmapUniverse;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.world.phys.Vec3;

/** Adapter from the current automatic-flight state to the generic pose API. */
final class ClientShipPoseProvider implements ShipPoseProvider
{
    static final ClientShipPoseProvider INSTANCE = new ClientShipPoseProvider();

    private ClientShipPoseProvider()
    {
    }

    /**
     * Capture the client flight state once for a render frame. Calling the
     * individual accessors while a network snapshot arrives can otherwise
     * combine the old position with the new heading for one frame.
     */
    SpaceRenderContext capture(float animationTicks)
    {
        ClientPlanetState.VisualSnapshot snapshot = ClientPlanetState.captureVisualSnapshot();
        // The entry id is authoritative; the snapshot's body id is the fallback for
        // a frame where the star-state packet has not landed yet.
        String currentHint = systemIdOf(snapshot.currentEntryId());
        if (currentHint == null)
            currentHint = systemIdOf(snapshot.currentBody());
        String targetHint = systemIdOf(snapshot.targetEntryId());
        if (targetHint == null)
            targetHint = systemIdOf(snapshot.targetBody());
        return new SpaceRenderContext(snapshot.position(), snapshot.universePosition(),
                snapshot.velocity(), snapshot.yaw(), snapshot.pitch(), snapshot.roll(),
                snapshot.flightPhase(), snapshot.warping(), snapshot.warpProgress(),
                snapshot.warpDurationTicks(), snapshot.currentBody(), snapshot.targetBody(),
                currentHint, targetHint, animationTicks);
    }

    @Override
    public Vec3 position()
    {
        return ClientPlanetState.getShipPosition();
    }

    @Override
    public UniversePosition universePosition()
    {
        return ClientPlanetState.getShipUniversePosition();
    }

    @Override
    public Vec3 velocity()
    {
        return ClientPlanetState.getShipVelocity();
    }

    @Override
    public double yaw()
    {
        return ClientPlanetState.getShipYaw();
    }

    @Override
    public double pitch()
    {
        return ClientPlanetState.getShipPitch();
    }

    @Override
    public double roll()
    {
        return ClientPlanetState.getShipRoll();
    }

    @Override
    public FlightPhase flightPhase()
    {
        return ClientPlanetState.getFlightPhase();
    }

    @Override
    public boolean isWarping()
    {
        return ClientPlanetState.isWarping();
    }

    @Override
    public float warpProgress()
    {
        return ClientPlanetState.warpProgress();
    }

    @Override
    public int warpDurationTicks()
    {
        return ClientPlanetState.getWarpDurationTicks();
    }

    @Override
    public String currentBodyId()
    {
        return ClientPlanetState.getCurrent();
    }

    @Override
    public String targetBodyId()
    {
        return ClientPlanetState.getWarpTarget();
    }

    @Override
    public String currentSystemHint()
    {
        String systemId = systemIdOf(ClientPlanetState.getCurrentEntryId());
        return systemId != null ? systemId : systemIdOf(currentBodyId());
    }

    @Override
    public String targetSystemHint()
    {
        String systemId = systemIdOf(ClientPlanetState.getWarpEntryId());
        return systemId != null ? systemId : systemIdOf(targetBodyId());
    }

    /** System owning a body id, or null when the body is unknown to the catalog. */
    private static String systemIdOf(String entryId)
    {
        return StarmapUniverse.systemIdOfEntry(entryId);
    }
}
