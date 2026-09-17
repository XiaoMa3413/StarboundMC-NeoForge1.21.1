package com.starboundmc.warp;

import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.SharedShipProgress;
import com.starboundmc.world.universe.BuiltInUniverse;
import com.starboundmc.world.universe.LegacyUniverseCompatibility;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gates for the §33 behaviour-preservation items.
 *
 * <p>Three of the plan's completion criteria are of the form "this is unchanged":
 * travel behaviour, fuel costs and story gating. A refactor is exactly the kind of
 * change that perturbs those by accident, so they are pinned here rather than
 * trusted.</p>
 */
class WarpBehaviourInvarianceTest
{
    private static final String LUSH = BuiltInUniverse.STARTER_BODY_ID;
    private static final String MOLTEN = "sys1:molten";
    private static final String BARREN = "sys1:barren";
    private static final String FROZEN = "sys2:frozen";

    // ------------------------------------------------------------- §33: fuel

    /**
     * Same-system travel costs 20 and cross-system costs 100, unchanged.
     *
     * <p>The migration replaced a colon-prefix test with a catalog ownership
     * lookup, so this is the assertion that the refactor derived the same answer
     * for the same pairs.</p>
     */
    @Test
    void fuelCostsAreUnchanged()
    {
        assertEquals(20, ShipWarpManager.warpFuelCost(LUSH, MOLTEN),
                "a within-system hop must still cost 20");
        assertEquals(20, ShipWarpManager.warpFuelCost(MOLTEN, BARREN),
                "two bodies of the same system must still cost 20");
        assertEquals(20, ShipWarpManager.warpFuelCost(BARREN, LUSH),
                "the cost must not depend on direction");
        assertEquals(100, ShipWarpManager.warpFuelCost(LUSH, FROZEN),
                "crossing systems must still cost 100");
        assertEquals(100, ShipWarpManager.warpFuelCost(FROZEN, LUSH),
                "the cross-system cost must not depend on direction");
    }

    /** The constants themselves are part of the contract. */
    @Test
    void fuelConstantsAreUnchanged()
    {
        assertEquals(1000, ShipFuelService.MAX_FUEL);
        assertEquals(20, ShipFuelService.WARP_FUEL_COST);
        assertEquals(100, ShipFuelService.CROSS_SYSTEM_FUEL_COST);
        assertEquals(ShipFuelService.MAX_FUEL, ShipStateData.MAX_FUEL);
    }

    /** An unknown body must not be silently treated as same-system. */
    @Test
    void anUnknownDestinationFallsBackToTheCrossSystemCost()
    {
        assertEquals(ShipFuelService.CROSS_SYSTEM_FUEL_COST,
                ShipWarpManager.warpFuelCost(LUSH, "othermod:planet_x"),
                "an unresolvable destination must not be priced as a local hop");
    }

    /** Fuel storage still clamps to the tank. */
    @Test
    void theFuelTankStillClampsItsContents()
    {
        ShipStateData data = new ShipStateData();
        data.setFuel(ShipFuelService.MAX_FUEL + 500);
        assertEquals(ShipFuelService.MAX_FUEL, data.getFuel());
        data.setFuel(-10);
        assertEquals(0, data.getFuel());

        assertEquals(0, ShipFuelService.acceptedAmount(ShipFuelService.MAX_FUEL, 100),
                "a full tank accepts nothing");
        assertEquals(100, ShipFuelService.acceptedAmount(0, 100));
        assertEquals(50, ShipFuelService.acceptedAmount(ShipFuelService.MAX_FUEL - 50, 100));
    }

    // ---------------------------------------------------- §33: story gating

    /**
     * A fresh world starts locked down exactly as before: the core is offline and
     * neither drive works.
     */
    @Test
    void aFreshWorldStartsWithTheSameStoryGate()
    {
        SharedShipProgress fresh = SharedShipProgress.newWorld();
        assertEquals(CoreState.OFFLINE, fresh.core());
        assertEquals(EngineState.DAMAGED, fresh.sublightEngine());
        assertEquals(EngineState.DAMAGED, fresh.hyperdrive());
        assertFalse(fresh.canTravelWithinSystem(),
                "sublight travel must be gated until the engine is repaired");
        assertFalse(fresh.canTravelBetweenSystems(),
                "cross-system travel must be gated until the hyperdrive is repaired");
    }

    /**
     * The progression spine still unlocks the same gates in the same order.
     *
     * <p>Each step is a precondition of the next: the sublight repair is refused
     * until the surface mission is complete, and the hyperdrive needs sublight
     * online. A refactor that reordered or dropped a gate would let a player travel
     * without doing the work, so the refusal cases are asserted too — a no-op call
     * must leave the gate shut.</p>
     */
    @Test
    void theStoryGateStillUnlocksInTheSameOrder()
    {
        SharedShipProgress fresh = SharedShipProgress.newWorld();
        assertFalse(fresh.canTravelWithinSystem(), "closed at the start");
        assertFalse(fresh.canTravelBetweenSystems(), "closed at the start");

        // Core online, but nothing else yet.
        SharedShipProgress online = fresh.beginCoreReboot(0L, 20L).finishCoreRebootIfDue(100L);
        assertEquals(CoreState.ONLINE, online.core());
        assertFalse(online.canTravelWithinSystem(),
                "an online core alone must not unlock travel");

        // The sublight repair requires the surface mission to be complete first.
        assertSame(online, online.restoreSublightEngine(),
                "the sublight repair must be refused before the surface mission");

        SharedShipProgress missionComplete = online.activateSurfaceMission().completeSurfaceMission();
        SharedShipProgress sublight = missionComplete.restoreSublightEngine();
        assertTrue(sublight.canTravelWithinSystem(),
                "repairing sublight must unlock within-system travel");
        assertFalse(sublight.canTravelBetweenSystems(),
                "within-system travel must not unlock cross-system travel");

        SharedShipProgress hyperdrive = sublight.restoreHyperdrive();
        assertTrue(hyperdrive.canTravelBetweenSystems(),
                "repairing the hyperdrive must unlock cross-system travel");
    }

    /**
     * A save from before the prologue keeps every ability, which is what stops the
     * migration from stranding existing players behind a gate they already passed.
     */
    @Test
    void aLegacySaveRetainsAllTravelAbilities()
    {
        SharedShipProgress legacy = SharedShipProgress.legacyUnlocked();
        assertTrue(legacy.canTravelWithinSystem());
        assertTrue(legacy.canTravelBetweenSystems());

        // And it survives the save/load round trip the migration touched.
        CompoundTag tag = new CompoundTag();
        tag.put("Story", legacy.save());
        ShipStateData restored = ShipStateData.load(tag);
        assertTrue(restored.getStoryProgress().canTravelWithinSystem());
        assertTrue(restored.getStoryProgress().canTravelBetweenSystems());
    }

    /**
     * Story progress must survive a save produced by the migrated schema, since
     * the save layout changed in A7.
     *
     * <p>Driven through {@link ShipStateData}'s own progression methods rather than
     * by building a detached {@code SharedShipProgress}: the state object holds the
     * progress, and only its mutators mark the data dirty and write it out. Doing
     * it the other way round produced a save that looked correct while containing
     * nothing, which is exactly the kind of mistake this test exists to prevent.</p>
     */
    @Test
    void storyProgressSurvivesTheMigratedSaveLayout()
    {
        ShipStateData data = new ShipStateData();
        data.setCurrentEntryId(FROZEN);
        data.beginCoreReboot(0L, 20L);
        data.finishCoreRebootIfDue(100L);
        data.activateSurfaceMission();
        data.completeSurfaceMission();
        data.restoreSublightEngine();
        assertTrue(data.getStoryProgress().canTravelWithinSystem(),
                "precondition: the drive must be repaired before saving");

        CompoundTag saved = data.save(new CompoundTag(), RegistryAccess.EMPTY);
        ShipStateData restored = ShipStateData.load(saved);

        assertEquals(ShipStateData.SCHEMA_VERSION, saved.getInt("SchemaVersion"));
        assertEquals(data.getCurrentEntryId(), restored.getCurrentEntryId(),
                "the location must survive alongside the story state");
        assertEquals(data.getStoryProgress().core(), restored.getStoryProgress().core());
        assertEquals(data.getStoryProgress().sublightEngine(),
                restored.getStoryProgress().sublightEngine());
        assertTrue(restored.getStoryProgress().canTravelWithinSystem(),
                "an unlocked drive must still be unlocked after the layout change");
        assertFalse(restored.getStoryProgress().canTravelBetweenSystems(),
                "an unrepaired hyperdrive must stay gated after the layout change");
    }

    /** A legacy save's unlocked drives must survive the migrated layout too. */
    @Test
    void aLegacyUnlockedSaveSurvivesTheMigratedLayout()
    {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Planet", "frozen");
        legacy.put("Story", SharedShipProgress.legacyUnlocked().save());

        ShipStateData restored = ShipStateData.load(legacy);

        assertEquals(FROZEN, restored.getCurrentEntryId(),
                "the legacy planet name must migrate to its entry id");
        assertTrue(restored.getStoryProgress().canTravelWithinSystem());
        assertTrue(restored.getStoryProgress().canTravelBetweenSystems());
    }

    // ------------------------------------------------- §33: travel behaviour

    /**
     * Every navigable pair is priced and can be routed, so no body became
     * unreachable through the refactor.
     */
    @Test
    void everyNavigablePairIsStillReachableAndPriced()
    {
        List<String> bodies = List.of(LUSH, MOLTEN, BARREN, FROZEN);
        for (String from : bodies)
        {
            for (String to : bodies)
            {
                if (from.equals(to))
                    continue;
                int cost = ShipWarpManager.warpFuelCost(from, to);
                assertTrue(cost == 20 || cost == 100,
                        from + " -> " + to + " has an unexpected cost " + cost);
                assertTrue(ShipFlightController.sampleUniversePosition(from, to, 560, 0) != null,
                        from + " -> " + to + " must produce a route");
            }
        }
    }

    /**
     * The legacy save mapping must still resolve every released planet name, since
     * that is what keeps existing saves loadable after the enum was deleted.
     */
    @Test
    void everyLegacyPlanetNameStillResolves()
    {
        for (String name : new String[] {"lush", "molten", "barren", "frozen"})
        {
            assertTrue(LegacyUniverseCompatibility.parsePlanetId(name).isPresent(),
                    "released save name '" + name + "' must still migrate");
        }
        assertEquals("sys1:lush", LegacyUniverseCompatibility.parsePlanetId("lush").orElseThrow());
        assertEquals("sys2:frozen", LegacyUniverseCompatibility.parsePlanetId("frozen").orElseThrow());
    }
}
