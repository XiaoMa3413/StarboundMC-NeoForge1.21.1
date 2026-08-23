package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniverseRouteFrameTest
{
    @Test
    void routeCoordinatesRemainContinuousAcrossPositiveSectorBoundary()
    {
        UniversePosition origin = UniversePosition.of(
                new SectorCoordinate(12L, -3L, 8L), 49_990.0, 100.0, -25.0);
        UniversePosition destination = UniversePosition.of(
                new SectorCoordinate(13L, -3L, 8L), -49_970.0, 104.0, 15.0);
        UniverseRouteFrame frame = new UniverseRouteFrame(origin);

        Vec3 relativeDestination = frame.toRelative(destination);
        UniversePosition boundary = frame.toUniverse(new Vec3(10.0, 1.0, 10.0));

        assertEquals(new Vec3(40.0, 4.0, 40.0), relativeDestination);
        assertEquals(new SectorCoordinate(13L, -3L, 8L), boundary.sector());
        assertEquals(-50_000.0, boundary.localX());
        assertEquals(destination, frame.toUniverse(relativeDestination));
    }

    @Test
    void routeCoordinatesRemainContinuousAcrossNegativeSectorBoundary()
    {
        UniversePosition origin = UniversePosition.of(
                new SectorCoordinate(-20L, 2L, 1L), -49_990.0, 0.0, 0.0);
        UniversePosition destination = UniversePosition.of(
                new SectorCoordinate(-21L, 2L, 1L), 49_980.0, 0.0, 0.0);
        UniverseRouteFrame frame = new UniverseRouteFrame(origin);

        assertEquals(new Vec3(-30.0, 0.0, 0.0), frame.toRelative(destination));
        assertEquals(destination, frame.toUniverse(new Vec3(-30.0, 0.0, 0.0)));
    }
}
