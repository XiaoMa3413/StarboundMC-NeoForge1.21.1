package com.starboundmc.world.universe;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A3, as it stands after A10 deleted the legacy model.
 *
 * <p>This began as a legacy-versus-catalog comparison: it read the shipped
 * {@code Planet} / {@code StarSystems} / {@code ShipSpace} statics and asserted
 * the new definitions reproduced them exactly. That was the right check while both
 * models existed, and it is what let the migration proceed safely.</p>
 *
 * <p>The legacy side is gone, so the comparison has no subject. What remains is
 * the contract everything else relies on: the shipped data, the codecs, and the
 * invariants that make lookups unambiguous. The values checked here are literals,
 * which is stronger than reading them back out of the thing under test.</p>
 */
class UniverseCatalogEquivalenceTest
{
    private static final double EPS = 1.0E-9;

    private static final UniverseCatalog CATALOG =
            UniverseCatalog.of(BuiltInUniverse.systems());

    // -------------------------------------------------------------- structure

    @Test
    void theCatalogContainsTheShippedUniverse()
    {
        assertEquals(2, CATALOG.systemCount());
        assertEquals(6, CATALOG.bodyCount());

        Set<String> systemIds = new LinkedHashSet<>();
        for (StarSystemDefinition system : CATALOG.allSystems())
            systemIds.add(system.systemId());
        assertEquals(Set.of("sys1", "sys2"), systemIds,
                "system ids are stored in saves and packets and must not change");

        Set<String> bodyIds = new LinkedHashSet<>();
        for (CelestialBodyDefinition body : CATALOG.allBodies())
            bodyIds.add(body.entryId());
        assertEquals(Set.of("sys1:barren", "sys1:lush", "sys1:molten", "sys1:gasgiant",
                        "sys1:rockymoon", "sys2:frozen"), bodyIds,
                "entry ids are stored in saves and packets and must not change");
    }

    @Test
    void catalogOwnershipResolvesEveryBodyToItsSystem()
    {
        for (StarSystemDefinition system : CATALOG.allSystems())
        {
            for (CelestialBodyDefinition body : system.bodies())
            {
                assertEquals(system.systemId(),
                        CATALOG.systemOfBody(body.entryId()).orElseThrow().systemId(),
                        "wrong owner for " + body.entryId());
            }
        }
    }

    @Test
    void moonCountsMatchTheDeclaredOrbits()
    {
        StarSystemDefinition main = CATALOG.system("sys1").orElseThrow();
        assertEquals(1, main.moonCount("sys1:lush"));
        assertEquals(1, main.moonCount("sys1:gasgiant"));
        assertEquals(0, main.moonCount("sys1:barren"));
        assertEquals(0, main.moonCount("sys1:molten"));
        assertEquals(0, CATALOG.system("sys2").orElseThrow().moonCount("sys2:frozen"));
        assertEquals(0, main.moonCount(null));
    }

    @Test
    void everyMoonNamesAParentThatExistsInItsOwnSystem()
    {
        for (StarSystemDefinition system : CATALOG.allSystems())
        {
            for (CelestialBodyDefinition body : system.bodies())
            {
                String parent = body.parentEntryId().orElse(null);
                if (parent == null)
                    continue;
                assertTrue(CATALOG.body(parent).isPresent(),
                        body.entryId() + " orbits missing parent " + parent);
                assertEquals(system.systemId(),
                        CATALOG.systemOfBody(parent).orElseThrow().systemId(),
                        body.entryId() + " orbits a body in a different system");
            }
        }
    }

    // ------------------------------------------------------------ navigation

    @Test
    void navigationGeometryMatchesTheAuthoredValues()
    {
        for (String entryId : NavigableBodyExpectations.entryIds())
        {
            NavigableBodyExpectations.Dock expected = NavigableBodyExpectations.dock(entryId);
            BodyNavigationProfile navigation = CATALOG.body(entryId).orElseThrow()
                    .navigation().orElseThrow();

            assertEquals(expected.radius(), navigation.bodyRadius(), EPS, entryId + " radius");
            assertEquals(expected.yawDock(), navigation.dockYaw(), EPS, entryId + " dock yaw");
            assertPosition(expected.dockPosition(), navigation.dockPosition(), entryId + " dock");
            assertPosition(expected.bodyPosition(), navigation.bodyPosition(), entryId + " body");
        }
    }

    private static void assertPosition(UniversePosition expected, UniversePosition actual, String label)
    {
        assertEquals(expected.sector(), actual.sector(), label + " sector");
        assertEquals(expected.localX(), actual.localX(), EPS, label + " x");
        assertEquals(expected.localY(), actual.localY(), EPS, label + " y");
        assertEquals(expected.localZ(), actual.localZ(), EPS, label + " z");
    }

    @Test
    void navigationDistancesMatchTheAuthoredFlightDistances()
    {
        List<String> bodies = NavigableBodyExpectations.entryIds();
        for (String from : bodies)
        {
            for (String to : bodies)
            {
                if (from.equals(to))
                    continue;
                Double expected = expectedDistanceOrNull(from, to);
                if (expected == null)
                    continue;
                UniversePosition fromDock = CATALOG.body(from).orElseThrow()
                        .navigation().orElseThrow().dockPosition();
                UniversePosition toDock = CATALOG.body(to).orElseThrow()
                        .navigation().orElseThrow().dockPosition();

                assertEquals(expected, Math.sqrt(fromDock.distanceToSqr(toDock)), 1.0E-6,
                        from + " -> " + to);
            }
        }
    }

    /** Exact distances were captured for the starter routes; others skip. */
    private static Double expectedDistanceOrNull(String from, String to)
    {
        try
        {
            return NavigableBodyExpectations.flightDistance(from, to);
        }
        catch (NullPointerException missing)
        {
            return null;
        }
    }

    @Test
    void sunPositionsAndDirectionsMatch()
    {
        for (StarSystemDefinition system : CATALOG.allSystems())
        {
            assertEquals(NavigableBodyExpectations.starPosition(system.systemId()),
                    system.stellarVisual().getVirtualPosition(),
                    system.systemId() + " star position");
            assertEquals(NavigableBodyExpectations.lightingDirection(system.systemId()),
                    system.lightingDirection(), system.systemId() + " lighting direction");
        }
    }

    /** Ownership is a data property; the entry id's shape must not decide it. */
    @Test
    void ownershipComesFromTheDataNotFromTheIdPrefix()
    {
        assertEquals("sys1", CATALOG.systemOfBody("sys1:lush").orElseThrow().systemId());
        assertEquals("sys2", CATALOG.systemOfBody("sys2:frozen").orElseThrow().systemId());
        assertTrue(CATALOG.systemOfBody("unowned").isEmpty());
        assertTrue(CATALOG.body("sys1:missing").isEmpty());
        assertTrue(CATALOG.body(null).isEmpty());
        assertTrue(CATALOG.system("sys9").isEmpty());
        assertTrue(UniverseCatalog.empty().isEmpty());
    }

    // ---------------------------------------------------------------- catalog

    @Test
    void catalogLookupsReturnTheExpectedSlices()
    {
        assertEquals(4, CATALOG.navigableBodies().size());
        assertEquals(4, CATALOG.surfaceBodies().size());
        // The renderer draws exactly these four; see PlanetRendererTableEquivalenceTest.
        assertEquals(4, CATALOG.spaceRenderedBodies().size());

        Set<String> navigable = new LinkedHashSet<>();
        for (CelestialBodyDefinition body : CATALOG.navigableBodies())
            navigable.add(body.entryId());
        assertEquals(Set.of("sys1:barren", "sys1:lush", "sys1:molten", "sys2:frozen"), navigable);

        assertTrue(CATALOG.bodyByDimension(ResourceLocation.parse("minecraft:overworld")).isPresent());
        assertTrue(CATALOG.bodyByDimension(ResourceLocation.parse("starboundmc:barren")).isPresent());
        assertTrue(CATALOG.bodyByDimension(ResourceLocation.parse("starboundmc:molten")).isPresent());
        assertTrue(CATALOG.bodyByDimension(ResourceLocation.parse("starboundmc:frozen")).isPresent());
        assertTrue(CATALOG.bodyByDimension(ResourceLocation.parse("starboundmc:nope")).isEmpty());
    }

    @Test
    void eachDimensionIsClaimedByExactlyOneBody()
    {
        Set<ResourceLocation> dimensions = new LinkedHashSet<>();
        for (CelestialBodyDefinition body : CATALOG.surfaceBodies())
        {
            ResourceLocation dimension = body.surface().orElseThrow().dimension();
            assertTrue(dimensions.add(dimension), "duplicate dimension claim: " + dimension);
        }
        assertEquals(4, dimensions.size());
        assertEquals("sys1:barren",
                CATALOG.bodyByDimension(ResourceLocation.parse("starboundmc:barren"))
                        .orElseThrow().entryId());
        assertEquals("sys1:lush",
                CATALOG.bodyByDimension(ResourceLocation.parse("minecraft:overworld"))
                        .orElseThrow().entryId());
    }

    /**
     * Both systems sit inside sector zero (their centres are tens of thousands of
     * units apart, under the 100,000 sector size), which is why the index reports
     * a single occupied sector.
     */
    @Test
    void theSpatialIndexBucketsSystemsByTheirNavigationSector()
    {
        StarSystemDefinition[] output = new StarSystemDefinition[8];
        int found = CATALOG.spatialIndex().queryNearby(
                CATALOG.system("sys1").orElseThrow().navigationCenter().sector(), 0, output);

        assertEquals(2, found, "both systems occupy sector zero");
        assertEquals(2, CATALOG.spatialIndex().systemCount());
        assertEquals(1, CATALOG.spatialIndex().occupiedSectorCount());
    }

    // ------------------------------------------------ legacy save compatibility

    /**
     * A10 deleted the {@code Planet} enum, but the names it wrote into saves are
     * permanent. This pins the mapping so the save migration cannot silently lose
     * an entry — the failure mode being a player quietly relocated.
     */
    @Test
    void theLegacySaveMappingIsCompleteAndLossless()
    {
        var mapping = LegacyUniverseCompatibility.legacyNameMapping();
        assertEquals(4, mapping.size());
        assertEquals("sys1:lush", mapping.get("lush"));
        assertEquals("sys1:molten", mapping.get("molten"));
        assertEquals("sys1:barren", mapping.get("barren"));
        assertEquals("sys2:frozen", mapping.get("frozen"));

        for (var entry : mapping.entrySet())
        {
            assertEquals(entry.getValue(),
                    LegacyUniverseCompatibility.parsePlanetId(entry.getKey()).orElseThrow());
            assertEquals(entry.getKey(),
                    LegacyUniverseCompatibility.legacyPlanetName(entry.getValue()).orElseThrow());
            assertTrue(LegacyUniverseCompatibility.hasLegacyName(entry.getValue()));
        }

        // Bodies with no legacy name answer empty rather than borrowing one.
        assertTrue(LegacyUniverseCompatibility.legacyPlanetName("sys1:gasgiant").isEmpty());
        assertTrue(LegacyUniverseCompatibility.legacyPlanetName("sys1:rockymoon").isEmpty());
        assertTrue(LegacyUniverseCompatibility.parsePlanetId("othermod:planet_x").isEmpty());
        assertTrue(LegacyUniverseCompatibility.parsePlanetId(null).isEmpty());
        assertTrue(LegacyUniverseCompatibility.legacyPlanetName(null).isEmpty());
    }

    // ------------------------------------------------------------------ codec

    @Test
    void definitionsRoundTripThroughTheirCodecs()
    {
        for (StarSystemDefinition system : BuiltInUniverse.systems())
        {
            var encoded = StarSystemDefinition.CODEC
                    .encodeStart(JSON, system).getOrThrow();
            StarSystemDefinition decoded = StarSystemDefinition.CODEC
                    .parse(JSON, encoded).getOrThrow();
            assertEquals(system, decoded, "codec round trip changed " + system.systemId());
        }
    }

    @Test
    void codecsRejectMalformedDefinitions()
    {
        assertTrue(BodyOrbitDefinition.CODEC.parse(JSON, json("{}")).isError());
        assertTrue(BodySurfaceDefinition.CODEC.parse(JSON, json("""
                {"dimension":"minecraft:overworld","landing_policy":"TELEPORT"}""")).isError());
        assertTrue(StarmapBodyVisual.CODEC.parse(JSON, json("""
                {"body_type":"PLASMA","marker_size":8,"primary_color":1}""")).isError());
        assertTrue(CelestialBodyDefinition.CODEC.parse(JSON, json("""
                {"entry_id":"sys1:x","name_key":"n","type_key":"t","description_key":"d",
                 "threat_level":11,"orbit":{"orbit_radius":1,"orbit_angle":0.0},
                 "starmap_visual":{"body_type":"ROCKY","marker_size":8,"primary_color":1}}""")).isError());

        // A bare dimension name is normalized to the vanilla namespace rather than
        // rejected, matching how vanilla reads its own datapacks.
        assertEquals(ResourceLocation.parse("minecraft:overworld"),
                BodySurfaceDefinition.CODEC.parse(JSON, json("""
                                {"dimension":"overworld"}"""))
                        .getOrThrow().dimension());
    }

    @Test
    void aMalformedEntryIdIsRejectedRatherThanTreatedAsAPrefix()
    {
        assertThrows(IllegalArgumentException.class, () -> new CelestialBodyDefinition(
                "lush", "n", "t", "d", 0,
                BodyOrbitDefinition.aroundStar(1, 0.0F),
                StarmapBodyVisual.basic(0xFF000000, 8),
                Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CelestialBodyDefinition(
                null, "n", "t", "d", 0,
                BodyOrbitDefinition.aroundStar(1, 0.0F),
                StarmapBodyVisual.basic(0xFF000000, 8),
                Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void catalogRejectsDuplicateIdsAndDimensionClaims()
    {
        StarSystemDefinition system = BuiltInUniverse.systems().get(0);
        assertThrows(IllegalArgumentException.class,
                () -> UniverseCatalog.of(List.of(system, system)),
                "duplicate system id must be rejected");

        // Two bodies claiming the same dimension is ambiguous, so it fails loudly
        // instead of letting whichever loaded last win.
        CelestialBodyDefinition body = system.bodies().get(0);
        CelestialBodyDefinition second = new CelestialBodyDefinition(
                "sys1:clone", body.nameKey(), body.typeKey(), body.descriptionKey(), 1,
                BodyOrbitDefinition.aroundStar(9, 9.0F), body.starmapVisual(),
                Optional.empty(), Optional.empty(), body.surface());
        StarSystemDefinition withClone = new StarSystemDefinition("sys1clone",
                system.nameKey(), system.descriptionKey(), system.starTypeKey(),
                system.stellarVisual(), system.galaxyMapPosition(),
                system.navigationCenter(), system.influenceRadius(), List.of(body, second));
        assertThrows(IllegalArgumentException.class,
                () -> UniverseCatalog.of(List.of(withClone)),
                "duplicate dimension claim must be rejected");
    }

    @Test
    void environmentProfilesSurviveRoundTrips()
    {
        for (StarSystemDefinition system : BuiltInUniverse.systems())
        {
            for (CelestialBodyDefinition body : system.bodies())
            {
                if (body.surface().isEmpty())
                    continue;
                PlanetEnvironmentProfile environment = body.surface().orElseThrow().environment();
                var encoded = PlanetEnvironmentProfile.CODEC
                        .encodeStart(JSON, environment).getOrThrow();
                PlanetEnvironmentProfile decoded = PlanetEnvironmentProfile.CODEC
                        .parse(JSON, encoded).getOrThrow();
                assertEquals(environment, decoded, body.entryId());
            }
        }
    }

    private static final com.mojang.serialization.JsonOps JSON =
            com.mojang.serialization.JsonOps.INSTANCE;

    private static com.google.gson.JsonElement json(String raw)
    {
        return com.google.gson.JsonParser.parseString(raw);
    }
}
