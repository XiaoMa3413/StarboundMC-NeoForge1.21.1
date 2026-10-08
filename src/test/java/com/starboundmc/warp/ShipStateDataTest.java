package com.starboundmc.warp;

import com.starboundmc.space.*;
import com.starboundmc.story.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipStateDataTest {
    private static CompoundTag save(ShipStateData data) { return data.save(new CompoundTag(), RegistryAccess.EMPTY); }
    @Test void sectorPositionVelocityAndFlightRoundTripExactly() {
        var position = UniversePosition.of(new SectorCoordinate(7,-4,2),123.5,-456.25,789.75);
        var velocity = new UniverseDelta(12.25,-.5,88);
        var original = new ShipStateData();
        original.setFlight(true,"test:target",17,90,FlightPhase.HYPERSPACE,position,velocity,22,-3,1.5);
        var restored = ShipStateData.load(save(original));
        assertEquals(position,restored.getShipUniversePosition()); assertEquals(velocity,restored.getShipVelocity());
        assertTrue(restored.isFlightActive()); assertEquals(FlightPhase.HYPERSPACE,restored.getFlightPhase());
        assertEquals("test:target",restored.getFlightTargetEntryId()); assertEquals(17,restored.getFlightElapsedTicks());
        assertEquals(22,restored.getShipYaw());
    }
    @Test void writesOnlyCanonicalSchema() {
        var tag=save(new ShipStateData());
        assertEquals(ShipStateData.SCHEMA_VERSION,tag.getInt("SchemaVersion"));
        assertEquals("DOCKED",tag.getString("FlightPhase"));
        for(var key:new String[]{"Planet","ShipX","ShipY","ShipZ","FlightPhaseName"}) assertFalse(tag.contains(key),key);
        assertEquals(tag,save(ShipStateData.load(tag)));
    }
    @Test void unsupportedInternalLayoutsAndMissingFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(new CompoundTag()));
        for(int version:new int[]{1,2,4}) { var tag=save(new ShipStateData());tag.putInt("SchemaVersion",version);
            assertThrows(IllegalArgumentException.class,()->ShipStateData.load(tag)); }
        for(var key:save(new ShipStateData()).getAllKeys()) {var tag=save(new ShipStateData());tag.remove(key);
            assertThrows(IllegalArgumentException.class,()->ShipStateData.load(tag),key);}
    }
    @Test void corruptionDoesNotBecomeDockedStarterState() {
        var phase=save(new ShipStateData());phase.putString("FlightPhase","BOGUS");
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(phase));
        var location=save(new ShipStateData());location.putString("CurrentEntry","");
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(location));
        var fuel=save(new ShipStateData());fuel.putInt("Fuel",Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(fuel));
        var pose=save(new ShipStateData());pose.putDouble("ShipLocalX",Double.NaN);
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(pose));
        var active=save(new ShipStateData());active.putBoolean("FlightActive",true);
        assertThrows(IllegalArgumentException.class,()->ShipStateData.load(active));
    }
    @Test void unknownDatapackBodyAndPoseSurviveRoundTrip() {
        var data=new ShipStateData();data.setCurrentEntryId("removed:body");data.markVisited("removed:body");
        var position=UniversePosition.of(new SectorCoordinate(80,2,-6),12,103,9);
        data.setFlight(false,null,0,0,FlightPhase.DOCKED,position,UniverseDelta.ZERO,32,0,0);
        var loaded=ShipStateData.load(save(data));
        assertEquals("removed:body",loaded.getCurrentEntryId());assertEquals(position,loaded.getShipUniversePosition());
        assertTrue(loaded.isVisited("removed:body"));
    }
    @Test void newWorldAndCurrentStoryRoundTripWithoutUnlockingProgress() {
        var data=new ShipStateData();assertEquals(CoreState.OFFLINE,data.getStoryProgress().core());
        data.beginCoreReboot(100,40);data.finishCoreRebootIfDue(140);data.activateSurfaceMission();
        var restored=ShipStateData.load(save(data));
        assertEquals(SurfaceMissionState.ACTIVE,restored.getStoryProgress().surfaceMission());
        assertEquals(EngineState.DAMAGED,restored.getStoryProgress().sublightEngine());
    }
}
