package com.starboundmc.warp;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipStateDataTest
{
    @Test
    void oldPositionFieldsLoadInSectorZero()
    {
        CompoundTag oldSave = baseTag();
        oldSave.putDouble("ShipX", 1200.5);
        oldSave.putDouble("ShipY", 102.0);
        oldSave.putDouble("ShipZ", -875.25);

        UniversePosition position = ShipStateData.load(oldSave).getShipUniversePosition();

        assertEquals(SectorCoordinate.ZERO, position.sector());
        assertEquals(1200.5, position.localX());
        assertEquals(102.0, position.localY());
        assertEquals(-875.25, position.localZ());
    }

    @Test
    void sectorPositionAndVelocityRoundTripExactly()
    {
        UniversePosition expectedPosition = UniversePosition.of(
                new SectorCoordinate(7L, -4L, 2L), 123.5, -456.25, 789.75);
        UniverseDelta expectedVelocity = new UniverseDelta(12.25, -0.5, 88.0);
        ShipStateData original = new ShipStateData();
        original.setFlight(true, "test:target", 17, 90, FlightPhase.HYPERSPACE,
                expectedPosition, expectedVelocity, 22.0, -3.0, 1.5);

        ShipStateData restored = ShipStateData.load(original.save(new CompoundTag(), RegistryAccess.EMPTY));

        assertEquals(expectedPosition, restored.getShipUniversePosition());
        assertEquals(expectedVelocity, restored.getShipVelocity());
    }

    @Test
    void nonCanonicalLocalCoordinatesNormalizeWhenLoaded()
    {
        CompoundTag save = baseTag();
        save.putLong("ShipSectorX", 4L);
        save.putLong("ShipSectorY", -2L);
        save.putLong("ShipSectorZ", 0L);
        save.putDouble("ShipLocalX", 50_001.0);
        save.putDouble("ShipLocalY", -50_001.0);
        save.putDouble("ShipLocalZ", 150_000.0);

        UniversePosition position = ShipStateData.load(save).getShipUniversePosition();

        assertEquals(new SectorCoordinate(5L, -3L, 2L), position.sector());
        assertEquals(-49_999.0, position.localX());
        assertEquals(49_999.0, position.localY());
        assertEquals(-50_000.0, position.localZ());
    }

    @Test
    void savingRetainsLegacyAbsolutePositionFields()
    {
        ShipStateData data = new ShipStateData();
        data.setFlight(false, null, 0, 0, FlightPhase.DOCKED,
                UniversePosition.of(new SectorCoordinate(1L, -1L, 0L), 25.0, -40.0, 102.0),
                new UniverseDelta(0.0, 0.0, 0.0), 0.0, 0.0, 0.0);

        CompoundTag saved = data.save(new CompoundTag(), RegistryAccess.EMPTY);

        assertTrue(saved.contains("ShipX"));
        assertTrue(saved.contains("ShipY"));
        assertTrue(saved.contains("ShipZ"));
        assertEquals(100_025.0, saved.getDouble("ShipX"));
        assertEquals(-100_040.0, saved.getDouble("ShipY"));
        assertEquals(102.0, saved.getDouble("ShipZ"));
    }

    private static CompoundTag baseTag()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Planet", Planet.LUSH.getId());
        return tag;
    }
}
