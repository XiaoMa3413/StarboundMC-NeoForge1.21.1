package com.starboundmc.space;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversePositionTest
{
    @Test
    void legacyCoordinatesRemainInSectorZero()
    {
        UniversePosition position = UniversePosition.fromLegacy(new Vec3(10.0, 20.0, 30.0));
        assertEquals(SectorCoordinate.ZERO, position.sector());
        assertEquals(10.0, position.localX());
        assertEquals(20.0, position.localY());
        assertEquals(30.0, position.localZ());
    }

    @Test
    void positiveAndNegativeBoundariesNormalize()
    {
        UniversePosition positive = UniversePosition.of(50_001.0, 0.0, 0.0);
        assertEquals(new SectorCoordinate(1, 0, 0), positive.sector());
        assertEquals(-49_999.0, positive.localX());

        UniversePosition negative = UniversePosition.of(-50_001.0, 0.0, 0.0);
        assertEquals(new SectorCoordinate(-1, 0, 0), negative.sector());
        assertEquals(49_999.0, negative.localX());
    }

    @Test
    void distanceRemainsContinuousAcrossSectorBoundary()
    {
        UniversePosition left = UniversePosition.of(49_999.5, 0.0, 0.0);
        UniversePosition right = UniversePosition.of(50_000.5, 0.0, 0.0);
        assertEquals(1.0, left.deltaTo(right).x(), 1.0E-9);
        assertEquals(1.0, left.distanceToSqr(right), 1.0E-9);
    }

    @Test
    void legacyRelativeDistanceIsUnchanged()
    {
        Vec3 fromLegacy = new Vec3(-5_000.0, 102.0, -2_000.0);
        Vec3 toLegacy = new Vec3(6_000.0, 102.0, 4_000.0);
        UniversePosition from = UniversePosition.fromLegacy(fromLegacy);
        UniversePosition to = UniversePosition.fromLegacy(toLegacy);
        assertEquals(fromLegacy.distanceToSqr(toLegacy), from.distanceToSqr(to), 1.0E-9);
    }

    @Test
    void hugeSectorDistanceUsesDoubleDelta()
    {
        UniversePosition origin = UniversePosition.of(SectorCoordinate.ZERO, 0.0, 0.0, 0.0);
        UniversePosition far = UniversePosition.of(new SectorCoordinate(1_000_000L, -2L, 3L), 12.5, 0.0, -8.0);
        UniverseDelta delta = origin.deltaTo(far);
        assertTrue(delta.x() > 99_999_999_999.0);
        assertEquals(-200_000.0, delta.y(), 1.0E-9);
        assertEquals(300_000.0 - 8.0, delta.z(), 1.0E-9);
    }
}
