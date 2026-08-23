package com.starboundmc;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipStateData;
import com.starboundmc.world.ShipTemplatePlacer;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage5PersistenceTest {
    @Test
    void flightSnapshotRoundTripsAtomically() {
        UniversePosition position = UniversePosition.of(
                new SectorCoordinate(7, -4, 2), 123.5, -456.25, 789.75);
        UniverseDelta velocity = new UniverseDelta(12.25, -0.5, 88.0);
        ShipStateData original = new ShipStateData();
        original.setFlight(true, "system:target", 17, 90, FlightPhase.HYPERSPACE,
                position, velocity, 22.0, -3.0, 1.5);

        CompoundTag tag = original.save(new CompoundTag(), RegistryAccess.EMPTY);
        ShipStateData restored = ShipStateData.load(tag, RegistryAccess.EMPTY);

        assertTrue(restored.isFlightActive());
        assertEquals("system:target", restored.getFlightTargetEntryId());
        assertEquals(17, restored.getFlightElapsedTicks());
        assertEquals(90, restored.getFlightTotalTicks());
        assertEquals(FlightPhase.HYPERSPACE, restored.getFlightPhase());
        assertEquals(position, restored.getShipUniversePosition());
        assertEquals(velocity, restored.getShipVelocity());
        assertEquals(22.0, restored.getShipYaw());
        assertEquals(-3.0, restored.getShipPitch());
        assertEquals(1.5, restored.getShipRoll());
    }

    @Test
    void invalidShipFieldsUseBoundedSafeDefaults() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Fuel", Integer.MAX_VALUE);
        tag.putBoolean("FlightActive", true);
        tag.putInt("FlightElapsed", -20);
        tag.putInt("FlightTotal", -1);
        tag.putString("FlightTarget", "");
        tag.putString("FlightPhaseName", "NOT_A_PHASE");
        tag.putDouble("ShipLocalX", Double.NaN);
        tag.putDouble("ShipYaw", Double.POSITIVE_INFINITY);

        ShipStateData restored = ShipStateData.load(tag, RegistryAccess.EMPTY);

        assertEquals(ShipStateData.MAX_FUEL, restored.getFuel());
        assertFalse(restored.isFlightActive());
        assertNull(restored.getFlightTargetEntryId());
        assertEquals(0, restored.getFlightElapsedTicks());
        assertEquals(0, restored.getFlightTotalTicks());
        assertEquals(FlightPhase.DOCKED, restored.getFlightPhase());
        assertEquals(UniversePosition.of(0.0, 102.0, 0.0), restored.getShipUniversePosition());
        assertEquals(0.0, restored.getShipYaw());
    }

    @Test
    void teleporterNamesAreTrimmedBoundedAndPersisted() {
        CompoundTag entries = new CompoundTag();
        entries.putString("minecraft:overworld|1,64,2", "  Bridge  ");
        entries.putString("bad|3,70,4", "x".repeat(100));
        entries.putString("blank|0,0,0", "   ");
        CompoundTag input = new CompoundTag();
        input.put("Entries", entries);

        TeleporterManager manager = TeleporterManager.load(input, RegistryAccess.EMPTY);
        CompoundTag saved = manager.save(new CompoundTag(), RegistryAccess.EMPTY)
                .getCompound("Entries");

        assertEquals("Bridge", saved.getString("minecraft:overworld|1,64,2"));
        assertEquals(TeleporterManager.MAX_NAME_LENGTH, saved.getString("bad|3,70,4").length());
        assertFalse(saved.contains("blank|0,0,0"));
    }

    @Test
    void shipTemplatePlacementFlagRoundTrips() {
        CompoundTag input = new CompoundTag();
        input.putBoolean("Placed", true);
        ShipTemplatePlacer.ShipTemplateData data =
                ShipTemplatePlacer.ShipTemplateData.load(input, RegistryAccess.EMPTY);

        assertTrue(data.save(new CompoundTag(), RegistryAccess.EMPTY).getBoolean("Placed"));
    }
}
