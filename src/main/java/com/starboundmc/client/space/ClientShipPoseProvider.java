package com.starboundmc.client.space;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.world.phys.Vec3;

/** Adapter from the current automatic-flight state to the generic pose API. */
final class ClientShipPoseProvider implements ShipPoseProvider
{
    static final ClientShipPoseProvider INSTANCE = new ClientShipPoseProvider();

    private ClientShipPoseProvider()
    {
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
    public Planet currentBody()
    {
        return ClientPlanetState.getCurrent();
    }

    @Override
    public Planet targetBody()
    {
        return ClientPlanetState.getWarpTarget();
    }

    @Override
    public String currentSystemHint()
    {
        String systemId = StarSystems.systemIdOfEntry(ClientPlanetState.getCurrentEntryId());
        return systemId != null ? systemId : systemIdOf(currentBody());
    }

    @Override
    public String targetSystemHint()
    {
        String systemId = StarSystems.systemIdOfEntry(ClientPlanetState.getWarpEntryId());
        return systemId != null ? systemId : systemIdOf(targetBody());
    }

    private static String systemIdOf(Planet planet)
    {
        StarSystem system = StarSystems.systemOfPlanet(planet);
        return system == null ? null : system.getSystemId();
    }
}
