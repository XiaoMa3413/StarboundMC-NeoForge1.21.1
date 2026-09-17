package com.starboundmc.warp;

import com.starboundmc.world.universe.LegacyUniverseCompatibility;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A7: the save's location is the entry id, legacy saves migrate,
 * and an unknown body is never silently rewritten.
 *
 * <p>This is the riskiest part of the migration because it runs against saves
 * that already exist on players' disks. The dangerous failure is quiet: resolving
 * an unrecognised body to the starting planet looks like success and moves the
 * player. The tests below pin the three cases apart.</p>
 */
class ShipStateDataMigrationTest
{
    private static final String LUSH = "sys1:lush";
    private static final String FROZEN = "sys2:frozen";

    private static CompoundTag legacySave(String planetName)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Planet", planetName);
        return tag;
    }

    // ------------------------------------------------------- legacy migration

    @Test
    void aLegacySaveWithoutAnEntryIdIsMigratedFromItsPlanetName()
    {
        // The frozen legacy mapping, exactly as released saves stored it.
        var expected = java.util.Map.of(
                "lush", "sys1:lush",
                "molten", "sys1:molten",
                "barren", "sys1:barren",
                "frozen", "sys2:frozen");
        for (var entry : expected.entrySet())
        {
            ShipStateData restored = ShipStateData.load(legacySave(entry.getKey()));
            assertEquals(entry.getValue(), restored.getCurrentEntryId(),
                    "a schema-1 save naming '" + entry.getKey() + "' must migrate to its entry id");
        }
    }

    /**
     * The whole point of §24: an id the game cannot resolve must not become the
     * starting planet.
     */
    @Test
    void anUnknownLegacyPlanetNameDoesNotBecomeTheStarterPlanet()
    {
        ShipStateData restored = ShipStateData.load(legacySave("othermod:planet_x"));

        assertNull(restored.getCurrentEntryId(),
                "an unknown planet name must not be silently rewritten to a real body");
        assertNull(restored.legacyPlanetName(),
                "an unknown location has no legacy planet either");
    }

    /**
     * {@code Planet.fromId} answers LUSH for anything unknown, so a migration that
     * went through it would relocate the player. This asserts the strict parse the
     * migration actually uses disagrees with that lenient one.
     */
    @Test
    void theMigrationParseRefusesToGuess()
    {
        // The deleted Planet.fromId used to answer LUSH here, which would have
        // relocated the player; the migration parse returns empty instead.
        assertTrue(LegacyUniverseCompatibility.parsePlanetId("nonsense").isEmpty(),
                "the migration parse must refuse to guess");
        assertTrue(LegacyUniverseCompatibility.parsePlanetId(null).isEmpty());
        assertTrue(LegacyUniverseCompatibility.parsePlanetId("  ").isEmpty());
    }

    @Test
    void theMigrationParseIsLenientAboutCaseAndWhitespaceOnly()
    {
        for (var entry : LegacyUniverseCompatibility.legacyNameMapping().entrySet())
        {
            String name = entry.getKey();
            String entryId = entry.getValue();
            assertEquals(entryId, LegacyUniverseCompatibility.parsePlanetId(name).orElseThrow());
            assertEquals(entryId, LegacyUniverseCompatibility.parsePlanetId("  " + name + "  ")
                    .orElseThrow());
            assertEquals(entryId, LegacyUniverseCompatibility
                    .parsePlanetId(name.toUpperCase(java.util.Locale.ROOT)).orElseThrow());
        }
    }

    @Test
    void aSaveWithNeitherFieldLoadsWithNoLocation()
    {
        ShipStateData restored = ShipStateData.load(new CompoundTag());
        assertNull(restored.getCurrentEntryId(),
                "a save with no location must report none, not invent one");
    }

    // ------------------------------------------------------------ round trips

    @Test
    void theEntryIdRoundTripsExactly()
    {
        ShipStateData original = new ShipStateData();
        original.setCurrentEntryId(FROZEN);

        ShipStateData restored = ShipStateData.load(original.save(new CompoundTag(), RegistryAccess.EMPTY));

        assertEquals(FROZEN, restored.getCurrentEntryId());
        assertEquals("frozen", restored.legacyPlanetName());
    }

    @Test
    void theSchemaVersionIsWrittenAndReadBack()
    {
        ShipStateData original = new ShipStateData();
        CompoundTag saved = original.save(new CompoundTag(), RegistryAccess.EMPTY);

        assertEquals(ShipStateData.SCHEMA_VERSION, saved.getInt("SchemaVersion"),
                "the save must declare its layout version");
        assertEquals(ShipStateData.SCHEMA_VERSION,
                ShipStateData.load(saved).getSchemaVersion());
    }

    /**
     * A body with no legacy planet must not write a plausible-looking wrong name.
     */
    @Test
    void aBodyWithNoLegacyPlanetWritesAnEmptyPlanetField()
    {
        ShipStateData data = new ShipStateData();
        data.setCurrentEntryId("sys1:rockymoon");

        CompoundTag saved = data.save(new CompoundTag(), RegistryAccess.EMPTY);

        assertEquals("", saved.getString("Planet"),
                "a non-legacy body must not claim a legacy planet name");
        assertNull(data.legacyPlanetName());
        // And the entry id survives, so the location is not lost.
        assertEquals("sys1:rockymoon", ShipStateData.load(saved).getCurrentEntryId());
    }

    // ---------------------------------------------------------- derived planet

    /**
     * The planet is derived from the entry id, so the two can never disagree.
     */
    @Test
    void theLegacyPlanetIsDerivedFromTheEntryId()
    {
        ShipStateData data = new ShipStateData();
        for (var entry : LegacyUniverseCompatibility.legacyNameMapping().entrySet())
        {
            data.setCurrentEntryId(entry.getValue());
            assertEquals(entry.getKey(), data.legacyPlanetName(),
                    entry.getValue() + " must derive legacy name " + entry.getKey());
        }
    }

    /**
     * Setting an unknown body leaves the ship with no legacy planet but keeps the
     * id, which is what lets a removed datapack be re-added.
     */
    @Test
    void anUnknownBodyKeepsItsIdAndHasNoDerivedPlanet()
    {
        ShipStateData data = new ShipStateData();
        data.setCurrentEntryId("othermod:planet_x");

        assertEquals("othermod:planet_x", data.getCurrentEntryId());
        assertNull(data.legacyPlanetName());

        CompoundTag saved = data.save(new CompoundTag(), RegistryAccess.EMPTY);
        assertEquals("othermod:planet_x", ShipStateData.load(saved).getCurrentEntryId(),
                "an unknown id must survive a save/load cycle unchanged");
    }

    /**
     * A dock position for an unknown body must not crash the load; the ship falls
     * back to a defined position and the location id is still preserved.
     */
    @Test
    void aSaveAtAnUnknownBodyStillLoadsWithADefinedPosition()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", ShipStateData.SCHEMA_VERSION);
        tag.putString("CurrentEntry", "othermod:planet_x");

        ShipStateData restored = ShipStateData.load(tag);

        assertNotNull(restored.getShipUniversePosition(), "the position must be defined");
        assertEquals("othermod:planet_x", restored.getCurrentEntryId());
        assertFalse(restored.isFlightActive());
    }

    @Test
    void theWrittenPlanetFieldAgreesWithTheEntryId()
    {
        ShipStateData data = new ShipStateData();
        data.setCurrentEntryId(FROZEN);
        CompoundTag saved = data.save(new CompoundTag(), RegistryAccess.EMPTY);

        // An older build reading this save gets the right planet back.
        assertEquals("frozen", saved.getString("Planet"));
        assertEquals("frozen", data.legacyPlanetName());
    }

    // ------------------------------------------- the two real saves on this desk

    /**
     * Save A: written by the planet-content build, so it carries both a
     * {@code SchemaVersion} of 2 and a {@code CurrentEntry} alongside a legacy
     * {@code Planet} name.
     *
     * <p>The entry id wins. Reading the legacy name instead would be harmless here
     * because they agree, but the field order must not be load-bearing: the id is
     * the authority and the name is only a view an older build can read.</p>
     */
    @Test
    void aSchema2SaveWithBothFieldsPrefersTheEntryId()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SchemaVersion", 2);
        tag.putString("Planet", "");
        tag.putString("CurrentEntry", "sys1:rockymoon");

        ShipStateData restored = ShipStateData.load(tag);

        assertEquals("sys1:rockymoon", restored.getCurrentEntryId());
        assertEquals(2, restored.getSchemaVersion());
        // No legacy name exists for this body, so the derived field stays empty
        // rather than naming a planet the ship is not at.
        assertNull(restored.legacyPlanetName());
    }

    /**
     * Save B: the same world, from a build that wrote the legacy {@code Planet}
     * name and no {@code SchemaVersion} at all.
     *
     * <p>{@code CurrentEntry} is present here too, which is what makes this shape
     * loadable at all: the name {@code rockymoon} is not in the frozen legacy
     * mapping, because the mapping only covers the four bodies that existed when
     * that field was the only location. Were the id absent, the name would be
     * unresolvable and the save would legitimately load with no location — the
     * opposite of the silent relocation this migration removed.</p>
     */
    @Test
    void aSchema1SaveWithAnUnmappedLegacyNameReliesOnItsEntryId()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Planet", "rockymoon");
        tag.putString("CurrentEntry", "sys1:rockymoon");

        ShipStateData restored = ShipStateData.load(tag);

        assertEquals("sys1:rockymoon", restored.getCurrentEntryId(),
                "the entry id is authoritative even when the legacy name cannot be resolved");
        assertEquals(1, restored.getSchemaVersion(), "a save with no version is the legacy layout");
        assertTrue(LegacyUniverseCompatibility.parsePlanetId("rockymoon").isEmpty(),
                "precondition: 'rockymoon' is not one of the four frozen legacy names");
    }

    /**
     * And the trap that shape hides: with no entry id, that same legacy name must
     * load with <em>no</em> location rather than being guessed at.
     */
    @Test
    void theSameSaveWithoutAnEntryIdRefusesToGuess()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Planet", "rockymoon");

        ShipStateData restored = ShipStateData.load(tag);

        assertNull(restored.getCurrentEntryId(),
                "an unmapped legacy name must not be resolved to a body");
    }
}
