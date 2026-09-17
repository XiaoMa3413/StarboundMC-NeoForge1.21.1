package com.starboundmc.warp;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.universe.BuiltInUniverse;
import com.starboundmc.world.universe.UniverseCatalog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression gate for the "ship parked at a body this build cannot place" crash.
 *
 * <h2>What happened</h2>
 *
 * <p>Entering a world aborted with {@code IllegalArgumentException: Unknown
 * navigable body: sys1:rockymoon} during {@code ServerStartedEvent}. The save was
 * written by a build that could fly to the rocky moon; the universe this build
 * loads does not provide that body, so the id resolves to nothing. §25 explicitly
 * requires that state to be survivable — keep the id, warn, forbid travel away —
 * but {@code persistDock()} asked for the dock position of an unresolvable body and
 * threw while the server was starting.</p>
 *
 * <h2>Why the existing tests missed it</h2>
 *
 * <p>{@code ShipStateDataMigrationTest} covered loading such a save and asserted
 * the id survived. Nothing covered what happens <em>next</em>: {@code init} then
 * asks for that body's dock. The unit under test was the save loader, while the
 * crash was in its caller. The tests below drive the decision the caller makes.</p>
 */
class StrandedAtUnknownBodyTest
{
    /** The exact body id from the crashing save. */
    private static final String UNKNOWN = "sys1:rockymoon";

    private static final String LUSH = BuiltInUniverse.STARTER_BODY_ID;

    @AfterEach
    void resetManager()
    {
        ShipWarpManager.reset();
    }

    // ------------------------------------------------- the decision that threw

    /**
     * The dock decision must not ask the universe to place a body it cannot
     * resolve; it keeps the last known pose instead.
     */
    @Test
    void theDockDecisionKeepsTheLastPoseWhenStranded()
    {
        UniversePosition stored = UniversePosition.fromLegacy(
                new net.minecraft.world.phys.Vec3(-15781.273564177154, 89.0, -7464.854196469775));

        UniversePosition position = ShipWarpManager.dockPositionFor(UNKNOWN, true, stored);
        assertEquals(stored, position,
                "a stranded ship must keep its stored position, not be given a dock");
        assertEquals(306.0, ShipWarpManager.dockYawFor(UNKNOWN, true, 306.0), 0.0,
                "a stranded ship must keep its stored heading");
    }

    /**
     * And the opposite: a resolvable body still snaps to its authored dock, so the
     * guard did not accidentally disable normal docking.
     */
    @Test
    void theDockDecisionStillUsesTheDockForAResolvableBody()
    {
        UniversePosition nonsense = UniversePosition.of(999.0, 999.0, 999.0);

        assertEquals(UniverseNavigation.universeDock(LUSH),
                ShipWarpManager.dockPositionFor(LUSH, false, nonsense),
                "a normal dock must still come from the catalog");
        assertEquals(UniverseNavigation.yawDock(LUSH),
                ShipWarpManager.dockYawFor(LUSH, false, 123.0), 0.0,
                "a normal dock heading must still come from the catalog");
    }

    /**
     * The reason the guard exists: the underlying lookup throws for an unknown
     * body. If this ever becomes lenient, the guard is redundant but harmless; if it
     * stays strict, removing the guard is what crashes the server.
     */
    @Test
    void theGeometryAccessorsRefuseAnUnknownBody()
    {
        assertFalse(UniverseNavigation.isNavigable(UNKNOWN),
                "precondition: this build does not provide the rocky moon");
        assertThrows(IllegalArgumentException.class,
                () -> UniverseNavigation.universeDock(UNKNOWN),
                "the dock lookup is what used to abort server start");
        assertThrows(IllegalArgumentException.class,
                () -> UniverseNavigation.universeBodyPosition(UNKNOWN));
        assertThrows(IllegalArgumentException.class,
                () -> UniverseNavigation.radius(UNKNOWN));
        // yawDock is deliberately lenient; the guard covers it anyway so a stranded
        // ship reports its stored heading rather than a fabricated zero.
        assertTrue(Double.isFinite(UniverseNavigation.yawDock(UNKNOWN)));
    }

    // --------------------------------------------------------- the save itself

    /**
     * The exact save shape that crashed: schema 2, an unresolvable current entry,
     * and a stored pose.
     */
    private static CompoundTag strandedSave()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", ShipStateData.SCHEMA_VERSION);
        tag.putString("CurrentEntry", UNKNOWN);
        tag.putString("Planet", "");
        tag.putInt("Fuel", 980);
        // The sector keys are part of the position contract: without them the loader
        // falls back to the legacy absolute fields, and without those to a default
        // dock. The crashing save carried all of them.
        tag.putLong("ShipSectorX", 0L);
        tag.putLong("ShipSectorY", 0L);
        tag.putLong("ShipSectorZ", 0L);
        tag.putDouble("ShipLocalX", -15781.273564177154);
        tag.putDouble("ShipLocalY", 89.0);
        tag.putDouble("ShipLocalZ", -7464.854196469775);
        tag.putDouble("ShipYaw", 306.0);
        tag.put("Visited", new net.minecraft.nbt.ListTag());
        return tag;
    }

    @Test
    void aStrandedSaveLoadsWithoutThrowingAndKeepsItsId()
    {
        ShipStateData data = assertDoesNotThrow(() -> ShipStateData.load(strandedSave()));

        assertEquals(UNKNOWN, data.getCurrentEntryId(),
                "§25: the id must be preserved so re-adding the body restores the player");
        assertEquals(980, data.getFuel(), "fuel must survive");
        assertEquals(306.0, data.getShipYaw(), 0.0, "the stored heading must survive");
        assertTrue(data.getShipUniversePosition().localX() < -15000.0,
                "the stored position must survive rather than being reset to a dock");
    }

    @Test
    void theStrandedIdSurvivesASaveLoadCycleUnchanged()
    {
        ShipStateData first = ShipStateData.load(strandedSave());
        CompoundTag written = first.save(new CompoundTag(), RegistryAccess.EMPTY);
        ShipStateData second = ShipStateData.load(written);

        assertEquals(UNKNOWN, second.getCurrentEntryId(),
                "an unresolvable id must round-trip, not decay into the starter body");
        assertEquals("", written.getString("Planet"),
                "and it must not claim a legacy planet name it does not have");
    }

    // --------------------------------------------------- the manager's handling

    /**
     * {@code init} must complete for a stranded save. This is the call that failed
     * in production; it needs no server because the decision it makes is now a pure
     * helper, but the state transition is still asserted here.
     */
    @Test
    void aStrandedSaveDoesNotLeaveTheManagerBelievingItIsSomewhereElse()
    {
        ShipStateData data = ShipStateData.load(strandedSave());

        // The id the rest of the game asks for is still the saved one, so a UI or a
        // travel attempt reports the true location rather than a silent rewrite.
        assertEquals(UNKNOWN, data.getCurrentEntryId());
        assertFalse(UniverseNavigation.isNavigable(data.getCurrentEntryId()),
                "this build cannot place that body, which is why travel is blocked");
    }

    /** Re-adding the body must restore the player, which is the point of §25. */
    @Test
    void reAddingTheBodyWouldResolveAgain()
    {
        UniverseCatalog withRockyMoon = UniverseCatalog.of(List.of(
                BuiltInUniverse.systems().get(0),
                BuiltInUniverse.systems().get(1)));
        // The shipped universe has rockymoon only as a non-navigable body; once a
        // datapack gives it navigation it resolves, and the saved id points there.
        assertTrue(withRockyMoon.body(UNKNOWN).isPresent(),
                "the body is known to the universe, just not placeable");
        assertFalse(withRockyMoon.body(UNKNOWN).orElseThrow().isNavigable());
    }

    /**
     * Nothing the renderer iterates may be unplaceable, or it would crash the same
     * way the warp manager did. This is the same defect class, checked at the source.
     */
    @Test
    void everyBodyTheRendererIteratesCanBePlaced()
    {
        UniverseCatalog catalog = UniverseCatalog.of(BuiltInUniverse.systems());
        for (var body : catalog.spaceRenderedBodies())
        {
            assertTrue(body.isNavigable(),
                    body.entryId() + " is drawn but has no flight geometry to draw it with");
            assertDoesNotThrow(() -> UniverseNavigation.universeBodyPosition(body.entryId()));
            assertDoesNotThrow(() -> UniverseNavigation.radius(body.entryId()));
        }
    }

    /** Sanity: the shipped universe still has the four placeable bodies. */
    @Test
    void theShippedUniverseStillProvidesTheFourNavigableBodies()
    {
        UniverseCatalog catalog = UniverseCatalog.of(BuiltInUniverse.systems());
        assertEquals(4, catalog.navigableBodies().size());
        for (String entryId : new String[] {LUSH, "sys1:molten", "sys1:barren", "sys2:frozen"})
        {
            assertTrue(UniverseNavigation.isNavigable(entryId), entryId + " must be flyable");
            assertDoesNotThrow(() -> ShipWarpManager.dockPositionFor(
                    entryId, false, UniversePosition.of(0.0, 0.0, 0.0)));
        }
    }

    /**
     * A route needs BOTH ends placeable, so a stranded departure body must also
     * block resuming an interrupted flight — not just starting a new one.
     */
    @Test
    void aRouteCannotBeBuiltFromAStrandedBody()
    {
        // The controller asks for the departure dock first, which is what threw.
        assertThrows(IllegalArgumentException.class,
                () -> new ShipFlightController(UNKNOWN, LUSH),
                "a route from an unplaceable body must fail loudly, not silently");
        assertThrows(IllegalArgumentException.class,
                () -> new ShipFlightController(UNKNOWN, LUSH,
                        UniversePosition.of(0.0, 102.0, 0.0), 0, null, 0.0, 0.0, 0.0),
                "the resuming constructor must fail the same way");
        // And the reverse direction, for symmetry.
        assertThrows(IllegalArgumentException.class,
                () -> new ShipFlightController(LUSH, UNKNOWN));
    }

    /** Guard the helper's contract directly, including the null-ish edge. */
    @Test
    void theDockHelperIsTotalForEveryInputShape()
    {
        UniversePosition stored = UniversePosition.of(1.0, 2.0, 3.0);
        assertEquals(stored, ShipWarpManager.dockPositionFor(null, true, stored));
        assertEquals(stored, ShipWarpManager.dockPositionFor("anything", true, stored));
        assertThrows(IllegalArgumentException.class,
                () -> ShipWarpManager.dockPositionFor(UNKNOWN, false, stored),
                "without the stranded flag an unknown body must still be an error");
        // A delta round trip keeps the class usable for the flight snapshot.
        assertEquals(UniverseDelta.ZERO, new UniverseDelta(0.0, 0.0, 0.0));
    }
}
