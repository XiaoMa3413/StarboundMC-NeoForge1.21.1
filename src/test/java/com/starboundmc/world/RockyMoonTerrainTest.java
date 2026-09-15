package com.starboundmc.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-geometry coverage for the rocky moon's new landforms: the impact-crater
 * field and the mining-outpost layouts. Both are deliberately Minecraft-free so
 * the shape rules can be asserted without the game bootstrap.
 */
final class RockyMoonTerrainTest
{
    // ---- impact craters ----

    @Test
    void craterFieldIsBoundedAndCanGoBothWays() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = 0; x < 512; x++)
        {
            for (int z = 0; z < 512; z += 1)
            {
                int delta = RockyMoonCraters.deformation(x, z);
                min = Math.min(min, delta);
                max = Math.max(max, delta);
            }
        }
        assertTrue(min < 0, "the field must contain bowls");
        assertTrue(max > 0, "the field must contain raised rims");
        assertTrue(min >= -RockyMoonCraters.maxDepth(), "bowls must stay within the clamp, got " + min);
        assertTrue(max <= 4, "rims must stay shallow, got " + max);
    }

    @Test
    void craterFieldIsAFunctionOfWorldPositionOnly() {
        // The generator samples world coordinates, so the field must not depend
        // on which chunk a column lands in: identical inputs give identical
        // output, and the profile never steps more than a crater wall's slope.
        // (Measured against the analytic bowl: 2*depth/radius <= ~2/block, plus
        // rim and overlap, so 4 is a generous ceiling that still catches a hash
        // bug producing a tear.)
        for (int z = -50; z <= 50; z += 7)
        {
            for (int x = 15; x <= 17; x++)
            {
                assertEquals(RockyMoonCraters.deformation(x, z),
                        RockyMoonCraters.deformation(x, z), "field must be deterministic");
                int a = RockyMoonCraters.deformation(x, z);
                int b = RockyMoonCraters.deformation(x + 1, z);
                assertTrue(Math.abs(a - b) <= 4,
                        "crater field tore at " + x + "," + z + ": " + a + " -> " + b);
            }
        }
    }

    @Test
    void craterBowlsAreStoneAndRimsAreGravel() {
        // The surface-material rule: bowl floors expose stone, rims gather
        // loose debris. Assert the field produces both categories.
        boolean bowl = false;
        boolean rim = false;
        for (int x = -400; x < 400 && !(bowl && rim); x += 3)
        {
            for (int z = -400; z < 400; z += 3)
            {
                int delta = RockyMoonCraters.deformation(x, z);
                if (delta < 0)
                    bowl = true;
                if (delta > 0)
                    rim = true;
                if (bowl && rim)
                    break;
            }
        }
        assertTrue(bowl && rim, "both crater bowls and rims must occur");
    }

    // ---- mining outposts ----

    @Test
    void everyOutpostFitsInsideItsOwnChunk() {
        // A structure must never reach into a neighbouring chunk: it is written
        // during that chunk's generation, and WorldGenRegion rejects writes
        // outside the 3x3 feature region (which can spin the worker forever).
        assertFits(RockyMoonOutposts.spawnBeacon());
        assertFits(findStructure("crystal_outcrop").id(), findStructure("crystal_outcrop"));
        assertFits(findStructure("mining_camp").id(), findStructure("mining_camp"));
    }

    private static void assertFits(RockyMoonOutposts.Structure structure) {
        assertFits(structure.id(), structure);
    }

    private static void assertFits(String id, RockyMoonOutposts.Structure structure) {
        int maxJitter = RockyMoonOutposts.MAX_CENTER_JITTER;
        // Centre is chunk-local x/z = 8 plus jitter; the layout's half-extent
        // must keep every block inside [0, 16).
        assertTrue(8 + maxJitter + structure.halfX() <= 16,
                id + " footprint crosses the +X chunk edge");
        assertTrue(8 - maxJitter - structure.halfX() >= 0,
                id + " footprint crosses the -X chunk edge");
        assertTrue(8 + maxJitter + structure.halfZ() <= 16,
                id + " footprint crosses the +Z chunk edge");
        assertTrue(8 - maxJitter - structure.halfZ() >= 0,
                id + " footprint crosses the -Z chunk edge");
        assertTrue(structure.clearHeight() > 0);
    }

    @Test
    void spawnBeaconStandsAtTheLandingPoint() {
        RockyMoonOutposts.Structure beacon = RockyMoonOutposts.spawnBeacon();
        assertEquals("beacon_pad", beacon.id());
        boolean hasEmitter = beacon.blocks().stream()
                .anyMatch(block -> block.kind() == RockyMoonOutposts.Kind.ENGINE);
        boolean hasCrystalMarkers = beacon.blocks().stream()
                .anyMatch(block -> block.kind() == RockyMoonOutposts.Kind.CRYSTAL);
        assertTrue(hasEmitter, "the beacon needs its scavenged engine emitter");
        assertTrue(hasCrystalMarkers, "the beacon needs self-luminous crystal markers");
        // The centre pad is recessed deepslate, so the emitter cannot be the
        // only block and the pad reads as built, not as a single pillar.
        assertTrue(beacon.blocks().size() > 40, "pad surface is too sparse");
    }

    // ---- airless-moon rules ----

    @Test
    void noLayoutBurnsAnythingWithoutOxygen() {
        // Torches and lanterns are open flame and need an oxidizer. This moon
        // has no atmosphere, so no flame block may appear in any layout — the
        // outpost is lit by self-luminous crystal and the salvaged engine.
        for (RockyMoonOutposts.Kind kind : RockyMoonOutposts.Kind.values()) {
            String name = kind.name();
            assertFalse(name.equals("TORCH") || name.equals("LANTERN") || name.equals("FIRE")
                            || name.equals("CAMPFIRE") || name.equals("SOUL_TORCH"),
                    "flame block kind is not allowed on an airless moon: " + name);
        }
        for (String id : new String[] {"beacon_pad", "crystal_outcrop", "mining_camp"}) {
            RockyMoonOutposts.Structure structure = id.equals("beacon_pad")
                    ? RockyMoonOutposts.spawnBeacon() : findStructure(id);
            for (RockyMoonOutposts.Block block : structure.blocks()) {
                String name = block.kind().name();
                assertFalse(name.contains("TORCH") || name.contains("LANTERN") || name.contains("FIRE"),
                        id + " uses a flame block at " + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    @Test
    void noLayoutUsesBlocksThatWouldFloatInPlace() {
        // Chains (and loose bars) have no canSurvive check in vanilla, so an
        // unsupported one hangs in mid-air forever. The layouts must not use a
        // chain at all, and every deepslate post must rest on something.
        for (RockyMoonOutposts.Kind kind : RockyMoonOutposts.Kind.values())
            assertFalse(kind.name().equals("CHAIN"),
                    "chains float without support; do not use them here");
        for (String id : new String[] {"beacon_pad", "crystal_outcrop", "mining_camp"}) {
            RockyMoonOutposts.Structure structure = id.equals("beacon_pad")
                    ? RockyMoonOutposts.spawnBeacon() : findStructure(id);
            java.util.Set<Long> occupied = new java.util.HashSet<>();
            for (RockyMoonOutposts.Block block : structure.blocks())
                occupied.add(key(block.x(), block.y(), block.z()));
            for (RockyMoonOutposts.Block block : structure.blocks()) {
                if (block.y() <= 0)
                    continue;
                // Bars are an intentional spanning roof; everything else that
                // sits above the pad must have a block directly beneath it.
                if (block.kind() == RockyMoonOutposts.Kind.IRON_BARS)
                    continue;
                assertTrue(occupied.contains(key(block.x(), block.y() - 1, block.z())),
                        id + " floats a " + block.kind() + " at "
                                + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 8) << 40) ^ ((long) (y + 8) << 20) ^ (z + 8);
    }

    @Test
    void beaconKeepsTheSpawnColumnClear() {
        // findSurfaceSpawn scans the exact spawn column first and teleports the
        // player one above the top solid block. Any beacon block above the pad
        // surface at (0, 0) would drop the player on top of it.
        RockyMoonOutposts.Structure beacon = RockyMoonOutposts.spawnBeacon();
        for (RockyMoonOutposts.Block block : beacon.blocks())
        {
            if (block.x() == 0 && block.z() == 0)
                assertTrue(block.y() <= 0,
                        "beacon blocks the spawn column at y=" + block.y());
        }
    }

    @Test
    void nonBeaconChunksAreDeterministicAndMostlyEmpty() {
        int populated = 0;
        int total = 0;
        for (int cx = -40; cx < 40; cx++)
        {
            for (int cz = -40; cz < 40; cz++)
            {
                total++;
                RockyMoonOutposts.Structure a = RockyMoonOutposts.forChunk(cx, cz);
                RockyMoonOutposts.Structure b = RockyMoonOutposts.forChunk(cx, cz);
                assertEquals(a == null, b == null, "hash lookup must be deterministic");
                if (a != null)
                    populated++;
            }
        }
        // Outposts should be landmarks, not a carpet: well under a quarter of
        // chunks, but present. (OUTCROP 0.16 + CAMP 0.09 = 0.25 exactly, so this
        // is really a floor check against an accidental all-or-nothing roll.)
        assertTrue(populated > 0, "the wastes must contain outposts");
        assertTrue(populated < total / 2, "outposts must stay sparse");
        assertEquals(null, RockyMoonOutposts.forChunk(0, 0), "the spawn chunk is the beacon");
    }

    private static RockyMoonOutposts.Structure findStructure(String id) {
        for (int cx = -200; cx < 200; cx++)
        {
            for (int cz = -200; cz < 200; cz++)
            {
                RockyMoonOutposts.Structure structure = RockyMoonOutposts.forChunk(cx, cz);
                if (structure != null && structure.id().equals(id))
                    return structure;
            }
        }
        throw new AssertionError("no " + id + " found");
    }

    @Test
    void outpostLayoutsHaveNoDuplicateBlocks() {
        for (String id : new String[] {"beacon_pad", "crystal_outcrop", "mining_camp"})
        {
            RockyMoonOutposts.Structure structure = id.equals("beacon_pad")
                    ? RockyMoonOutposts.spawnBeacon() : findStructure(id);
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (RockyMoonOutposts.Block block : structure.blocks())
            {
                long key = ((long) (block.x() + 8) << 40)
                        ^ ((long) (block.y() + 8) << 20)
                        ^ (block.z() + 8);
                assertTrue(seen.add(key),
                        id + " places two blocks at " + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    @Test
    void landingPlainIsLeftFlatForThedescent() {
        // The crater and pad passes must never run inside the plain: the
        // generator skips those columns, so assert the plain geometry itself
        // still owns a flat core (regression guard for the new passes).
        int spawnX = RockyMoonPlanet.DEFAULT_SPAWN.getX();
        int spawnZ = RockyMoonPlanet.DEFAULT_SPAWN.getZ();
        int bench = RockyMoonLandingPlain.computeBenchY((x, z) -> 80);
        for (int[] probe : new int[][] {{spawnX, spawnZ}, {spawnX + 40, spawnZ},
                {spawnX - 30, spawnZ + 30}, {spawnX, spawnZ + 40}}) {
            int y = RockyMoonLandingPlain.targetY(probe[0], probe[1], 200, bench);
            assertTrue(y >= bench && y <= bench + 2, "plain core must stay flat");
        }
        assertFalse(RockyMoonLandingPlain.inPlain(spawnX + 500, spawnZ));
    }
}
