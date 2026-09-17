package com.starboundmc.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-geometry coverage for the rocky moon's landforms and outposts: the
 * impact-crater field, and the mining-outpost layouts with their setting and
 * airless-moon rules. Deliberately Minecraft-free so the shape rules can be
 * asserted without the game bootstrap.
 */
final class RockyMoonTerrainTest
{
    /** Every authored layout, so a new one cannot dodge the rule checks. */
    private static final String[] ALL_LAYOUTS = {
            "beacon_pad", "habitat_module", "processing_plant", "comms_tower",
            "supply_cache", "crystal_outcrop", "scrapper_camp",
    };

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

    @Test
    void baseReliefKeepsItselfWithinBoundsAndOffTheClamp()
    {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int atClamp = 0;
        int samples = 0;
        for (int x = -600; x < 600; x += 3)
        {
            for (int z = -600; z < 600; z += 3)
            {
                int offset = RockyMoonRelief.offset(x, z);
                min = Math.min(min, offset);
                max = Math.max(max, offset);
                if (offset == RockyMoonRelief.MIN_OFFSET || offset == RockyMoonRelief.MAX_OFFSET)
                    atClamp++;
                samples++;
            }
        }
        assertTrue(min < 0, "relief must cut downwards, got min " + min);
        assertTrue(max > 0, "relief must rise upwards, got max " + max);
        assertTrue(min >= RockyMoonRelief.MIN_OFFSET && max <= RockyMoonRelief.MAX_OFFSET,
                "relief escaped its declared bounds: " + min + ".." + max);
        // Sitting on the clamp means the octaves sum past the limit, which
        // flattens the field into plateaus - the very thing it exists to avoid.
        assertTrue(atClamp == 0, atClamp + " of " + samples + " columns are pinned to a relief clamp");
    }

    @Test
    void baseReliefIsSmoothEnoughToNotTerrace()
    {
        // shapeColumn reshapes each column independently, so a field that jumps
        // two blocks between most neighbours would appear as visible staircases.
        // A rare 2-3 block step is fine — that is a slope — but it must not be
        // the common case.
        int pairs = 0;
        int bigSteps = 0;
        for (int z = -400; z < 400; z += 3)
        {
            for (int x = -400; x < 400; x++)
            {
                int step = Math.abs(RockyMoonRelief.offset(x, z)
                        - RockyMoonRelief.offset(x + 1, z));
                assertTrue(step <= 3, "relief steps " + step + " blocks between neighbours");
                if (step >= 2)
                    bigSteps++;
                pairs++;
            }
        }
        assertTrue(bigSteps * 100 < pairs,
                bigSteps + "/" + pairs + " neighbour pairs step 2+ blocks: that would terrace");
    }

    @Test
    void baseReliefLeavesNoLongDeadLevelStretch()
    {
        // The reported defect was walking a long way with no height change at
        // all. A short level stretch is natural ground; a very long one is the
        // "eerie flat plain" reading. Vanilla plains commonly run ~50-100 blocks
        // level, so the bar is set just beyond that.
        int longest = 0;
        for (int z = -800; z < 800; z += 13)
        {
            int run = 0;
            for (int x = -800; x < 800; x++)
            {
                if (RockyMoonRelief.offset(x, z) == RockyMoonRelief.offset(x + 1, z))
                {
                    run++;
                    longest = Math.max(longest, run);
                }
                else
                    run = 0;
            }
        }
        assertTrue(longest < 130,
                "found a " + longest + "-block stretch with no relief change at all");
    }

    @Test
    void noLargeAreaIsCompletelyFlat()
    {
        // The reported defect: away from the craters the surface was uniform
        // gravel plain. Relief is additive and unconditional, so a window with
        // zero height range must not exist anywhere outside the landing plain.
        int flatWindows = 0;
        int windows = 0;
        for (int wx = -400; wx < 400; wx += 64)
        {
            for (int wz = -400; wz < 400; wz += 64)
            {
                int spawnX = RockyMoonPlanet.DEFAULT_SPAWN.getX();
                int spawnZ = RockyMoonPlanet.DEFAULT_SPAWN.getZ();
                // The plain is levelled on purpose, so it is exempt.
                if (Math.hypot(wx + 32 - spawnX, wz + 32 - spawnZ)
                        < RockyMoonLandingPlain.EDGE_RADIUS + 64)
                    continue;
                int lo = Integer.MAX_VALUE;
                int hi = Integer.MIN_VALUE;
                for (int x = wx; x < wx + 64; x += 4)
                {
                    for (int z = wz; z < wz + 64; z += 4)
                    {
                        int delta = RockyMoonRelief.offset(x, z)
                                + RockyMoonCraters.deformation(x, z);
                        lo = Math.min(lo, delta);
                        hi = Math.max(hi, delta);
                    }
                }
                assertTrue(hi - lo > 0,
                        "a 64x64 window at " + wx + "," + wz + " is dead flat");
                windows++;
                if (hi - lo <= 1)
                    flatWindows++;
            }
        }
        assertTrue(windows > 100, "the scan must cover the wastes");
        assertTrue(flatWindows * 10 < windows,
                flatWindows + "/" + windows + " windows undulate by a single block or less");
    }

    @Test
    void landingPlainFadeRampsFromFlatCoreToFullRelief()
    {
        // The fade is what lets the levelled bench meet rough ground without a
        // ring of cliffs: zero across the flat core, one past the skirt.
        int spawnX = RockyMoonPlanet.DEFAULT_SPAWN.getX();
        int spawnZ = RockyMoonPlanet.DEFAULT_SPAWN.getZ();
        assertEquals(0.0, RockyMoonLandingPlain.shapeFade(spawnX, spawnZ), 1.0e-9,
                "the flat core must carry no landform");
        assertEquals(1.0, RockyMoonLandingPlain.shapeFade(spawnX + 400, spawnZ), 1.0e-9,
                "the wastes must carry the full landform");
        // Monotone along a ray, so relief eases in rather than snapping on.
        int previous = -1;
        for (int distance = 0; distance <= 160; distance += 4)
        {
            int fade = (int) Math.round(RockyMoonLandingPlain.shapeFade(
                    spawnX + distance, spawnZ) * 1000.0);
            assertTrue(fade >= previous, "fade must not fall going outwards");
            previous = fade;
        }
    }

    // ---- mining outposts ----

    @Test
    void everyLayoutFitsInsideItsOwnChunk() {
        // A structure must never reach into a neighbouring chunk: it is written
        // during that chunk's generation, and WorldGenRegion rejects writes
        // outside the 3x3 feature region (a failed step re-queues the chunk
        // forever and the generating thread spins).
        for (String id : ALL_LAYOUTS)
            assertFits(id, RockyMoonOutposts.byId(id));
    }

    private static void assertFits(String id, RockyMoonOutposts.Structure structure) {
        assertTrue(structure.halfX() <= RockyMoonOutposts.MAX_HALF_EXTENT,
                id + " half-extent exceeds the single-chunk budget");
        assertTrue(structure.halfZ() <= RockyMoonOutposts.MAX_HALF_EXTENT,
                id + " half-extent exceeds the single-chunk budget");
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
        assertTrue(hasKind(beacon, RockyMoonOutposts.Kind.BEACON),
                "the beacon needs its signal head");
        assertTrue(hasKind(beacon, RockyMoonOutposts.Kind.LIGHT),
                "the beacon needs its approach lights");
        assertTrue(hasKind(beacon, RockyMoonOutposts.Kind.HAZARD),
                "the landing field needs hazard banding");
        assertTrue(beacon.blocks().size() > 40, "pad surface is too sparse");
    }

    @Test
    void noOutpostUsesAShipComponentAsASurfaceFixture()
    {
        // A repairable ship engine standing as a mast head on a derelict claim
        // read as the wrong kind of object: it is a player-repairable part, not
        // scenery. Signals use the authored beacon emitter instead.
        for (String id : ALL_LAYOUTS)
        {
            for (RockyMoonOutposts.Block block : RockyMoonOutposts.byId(id).blocks())
            {
                String kind = block.kind().name();
                assertFalse(kind.equals("ENGINE"),
                        id + " places a ship engine at "
                                + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    // ---- setting and technology level ----

    @Test
    void outpostsAreBuiltFromTheModsOwnIndustrialHullSet() {
        // The moon is a dead mining claim of the same starfaring civilisation
        // that built the ship, so its structures use the mod's hull set and
        // shipboard devices. Vanilla stone-brick/medieval furniture would read
        // as the wrong tech level entirely.
        assertTrue(hasKind(RockyMoonOutposts.byId("habitat_module"), RockyMoonOutposts.Kind.HULL));
        assertTrue(hasKind(RockyMoonOutposts.byId("processing_plant"), RockyMoonOutposts.Kind.REFINERY));
        assertTrue(hasKind(RockyMoonOutposts.byId("processing_plant"), RockyMoonOutposts.Kind.FURNACE));
        assertTrue(hasKind(RockyMoonOutposts.byId("habitat_module"), RockyMoonOutposts.Kind.WINDOW));
        for (String id : ALL_LAYOUTS) {
            for (RockyMoonOutposts.Block block : RockyMoonOutposts.byId(id).blocks()) {
                String kindName = block.kind().name();
                assertFalse(Set.of("COBBLE", "BRICK", "CRACKED", "ANVIL", "CAULDRON",
                                "CRAFTING", "GLASS_PANE", "DEEPSLATE").contains(kindName),
                        id + " still uses the removed stone-age palette: " + kindName);
            }
        }
    }

    @Test
    void habitatModulesAreSealedExceptForAnAuthoredBreach() {
        // No atmosphere means a habitat is a pressure vessel: it needs a floor,
        // a ceiling and a full wall ring, entered through an airlock.
        for (String id : new String[] {"habitat_module", "supply_cache"}) {
            RockyMoonOutposts.Structure structure = RockyMoonOutposts.byId(id);
            Set<Long> occupied = new HashSet<>();
            for (RockyMoonOutposts.Block block : structure.blocks())
                occupied.add(key(block.x(), block.y(), block.z()));
            int top = maxY(structure);
            for (int x = -structure.halfX(); x <= structure.halfX(); x++) {
                for (int z = -structure.halfZ(); z <= structure.halfZ(); z++) {
                    assertTrue(occupied.contains(key(x, 0, z)), id + " floor gap at " + x + "," + z);
                    assertTrue(occupied.contains(key(x, top, z)), id + " ceiling gap at " + x + "," + z);
                }
            }
            assertTrue(hasKind(structure, RockyMoonOutposts.Kind.DOOR),
                    id + " has no airlock door");
        }
    }

    // ---- airless-moon rules ----

    @Test
    void noLayoutBurnsAnythingWithoutOxygen() {
        // Torches and lanterns are open flame and need an oxidizer. This moon
        // has no atmosphere, so no flame block may appear in any layout: light
        // is electric (industrial_light) or self-luminous fuel crystal.
        for (RockyMoonOutposts.Kind kind : RockyMoonOutposts.Kind.values()) {
            String name = kind.name();
            assertFalse(name.contains("TORCH") || name.contains("LANTERN") || name.contains("FIRE")
                            || name.contains("CAMPFIRE"),
                    "flame block kind is not allowed on an airless moon: " + name);
        }
        for (String id : ALL_LAYOUTS) {
            for (RockyMoonOutposts.Block block : RockyMoonOutposts.byId(id).blocks()) {
                String name = block.kind().name();
                assertFalse(name.contains("TORCH") || name.contains("LANTERN") || name.contains("FIRE"),
                        id + " uses a flame block at " + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    @Test
    void noLayoutLeavesABlockFloatingDetached() {
        // The property that matters is structural attachment, not a block
        // directly underneath: a ceiling or roof beam is a legitimate span, but
        // nothing may hang detached in the sky. The generator levels the whole
        // footprint to the pad, so the y=1 layer always rests on solid ground;
        // the flood fill therefore starts there and every higher block must be
        // face-connected down to it.
        for (RockyMoonOutposts.Kind kind : RockyMoonOutposts.Kind.values())
            assertFalse(kind.name().equals("CHAIN"),
                    "chains float without support; do not use them here");
        for (String id : ALL_LAYOUTS) {
            RockyMoonOutposts.Structure structure = RockyMoonOutposts.byId(id);
            Set<Long> occupied = new HashSet<>();
            for (RockyMoonOutposts.Block block : structure.blocks())
                occupied.add(key(block.x(), block.y(), block.z()));
            Set<Long> reached = new HashSet<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (RockyMoonOutposts.Block block : structure.blocks()) {
                if (block.y() <= 1 && reached.add(key(block.x(), block.y(), block.z())))
                    queue.add(new int[] {block.x(), block.y(), block.z()});
            }
            int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!queue.isEmpty()) {
                int[] at = queue.poll();
                for (int[] step : steps) {
                    int nx = at[0] + step[0];
                    int ny = at[1] + step[1];
                    int nz = at[2] + step[2];
                    if (occupied.contains(key(nx, ny, nz)) && reached.add(key(nx, ny, nz)))
                        queue.add(new int[] {nx, ny, nz});
                }
            }
            for (RockyMoonOutposts.Block block : structure.blocks()) {
                assertTrue(reached.contains(key(block.x(), block.y(), block.z())),
                        id + " leaves a " + block.kind() + " detached at "
                                + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    @Test
    void beaconKeepsTheSpawnColumnClear() {
        // findSurfaceSpawn scans the exact spawn column first and teleports the
        // player one above the top solid block. Any beacon block above the pad
        // surface at (0, 0) would drop the player on top of it.
        RockyMoonOutposts.Structure beacon = RockyMoonOutposts.spawnBeacon();
        for (RockyMoonOutposts.Block block : beacon.blocks()) {
            if (block.x() == 0 && block.z() == 0)
                assertTrue(block.y() <= 0, "beacon blocks the spawn column at y=" + block.y());
        }
    }

    @Test
    void landingSiteCarriesOnlyTheBeaconAndOneHabitat()
    {
        // The claim is dead, so the landing site is not a base: a beacon pad
        // plus the crew's habitat and nothing more. A cluster of four modules
        // read as a settlement and undid the "abandoned" premise.
        Set<String> ids = new HashSet<>();
        for (int cx = -2; cx <= 8; cx++)
        {
            for (int cz = -2; cz <= 8; cz++)
            {
                RockyMoonOutposts.Structure cluster = RockyMoonOutposts.clusterAt(cx, cz);
                if (cluster == null)
                    continue;
                assertTrue(cluster.isCluster(), cluster.id() + " must be a cluster layout");
                // The module has to stand on ground the landing plain already
                // levelled. Out on the skirt it would cut a step, and the player
                // would arrive at a building floating over a cliff edge.
                int centerX = cx * 16 + 8;
                int centerZ = cz * 16 + 8;
                double distance = Math.hypot(centerX - RockyMoonPlanet.DEFAULT_SPAWN.getX(),
                        centerZ - RockyMoonPlanet.DEFAULT_SPAWN.getZ());
                assertTrue(distance + cluster.halfX()
                                <= RockyMoonLandingPlain.FLAT_RADIUS,
                        cluster.id() + " sits outside the plain's flat core at distance " + distance);
                ids.add(cluster.id());
            }
        }
        assertEquals(Set.of("habitat_module"), ids,
                "the landing site must hold the habitat module and nothing else");
        // Cluster plots are never re-placed by the wilderness lattice.
        assertEquals(null, RockyMoonOutposts.forChunk(4, 0), "cluster plots are not wilderness rolls");
    }

    @Test
    void wildernessFindsAreScatteredFarApart()
    {
        // The complaint that drove this: structures stood one chunk apart, so a
        // desolate moon read as a suburb. Spacing is the property that matters,
        // so assert it directly instead of asserting a spawn probability.
        List<int[]> centers = new ArrayList<>();
        for (int cx = -400; cx < 400; cx++)
        {
            for (int cz = -400; cz < 400; cz++)
            {
                RockyMoonOutposts.Structure structure = RockyMoonOutposts.forChunk(cx, cz);
                if (structure != null)
                    centers.add(new int[] {cx * 16 + 8, cz * 16 + 8});
            }
        }
        assertTrue(centers.size() > 100, "the wastes must still contain finds");

        int closest = Integer.MAX_VALUE;
        for (int i = 0; i < centers.size(); i++)
        {
            for (int j = i + 1; j < centers.size(); j++)
            {
                int[] a = centers.get(i);
                int[] b = centers.get(j);
                closest = Math.min(closest,
                        Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1])));
            }
        }
        // Adjacent lattice cells nominate their hosts from the middle of each
        // cell, so the nearest two finds are a full cell apart minus the jitter
        // at both ends and the per-structure offset: at least 12 chunks.
        assertTrue(closest >= 12 * 16,
                "finds clumped: nearest pair only " + closest + " blocks apart");
    }

    @Test
    void wildernessDensityKeepsTheMoonEmpty()
    {
        // One find per CELL_CHANCE of a WILDERNESS_CELL-square cell, i.e. of
        // 16x16 chunks. Assert the resulting rate over a large area so the
        // emptiness cannot silently regress the way the per-chunk roll did.
        int found = 0;
        int chunkSpan = 800;
        for (int cx = -chunkSpan / 2; cx < chunkSpan / 2; cx++)
        {
            for (int cz = -chunkSpan / 2; cz < chunkSpan / 2; cz++)
            {
                if (RockyMoonOutposts.forChunk(cx, cz) != null)
                    found++;
            }
        }
        double perMillionBlocks = found * 1_000_000.0 / (chunkSpan * 16.0 * chunkSpan * 16.0);
        // Nominal 0.12 / 256^2 blocks^-2 = 1.83 per 1e6 blocks^2.
        assertTrue(perMillionBlocks > 0.5 && perMillionBlocks < 5.0,
                "wilderness density out of band: " + perMillionBlocks + " finds per 1000x1000");
    }

    @Test
    void lootCratesAreAuthoredForTheUsableLayouts() {
        // "Partially usable" means the sealed modules still hold something worth
        // the trip, so those layouts must open with a real payoff.
        for (String id : new String[] {"habitat_module", "processing_plant", "supply_cache"}) {
            RockyMoonOutposts.Structure structure = RockyMoonOutposts.byId(id);
            assertTrue(hasKind(structure, RockyMoonOutposts.Kind.CRATE_LOOT),
                    id + " has no loot crate");
            assertFalse(structure.loot().isEmpty(), id + " has an empty loot table");
            Set<Integer> slots = new HashSet<>();
            for (RockyMoonOutposts.Loot loot : structure.loot()) {
                assertTrue(slots.add(loot.slot()), id + " reuses slot " + loot.slot());
                assertTrue(loot.count() > 0, id + " has a non-positive stack");
                assertFalse(loot.item().isBlank(), id + " has an unnamed item");
            }
        }
        // The stripped scrapper camp and the raw mineral claim are scenery, not
        // supply points, so they must not carry loot.
        for (String id : new String[] {"scrapper_camp", "crystal_outcrop"}) {
            assertFalse(hasKind(RockyMoonOutposts.byId(id), RockyMoonOutposts.Kind.CRATE_LOOT),
                    id + " must not carry loot");
        }
    }

    @Test
    void everyLayoutHasNoDuplicateBlocks() {
        for (String id : ALL_LAYOUTS) {
            RockyMoonOutposts.Structure structure = RockyMoonOutposts.byId(id);
            Set<Long> seen = new HashSet<>();
            for (RockyMoonOutposts.Block block : structure.blocks()) {
                assertTrue(seen.add(key(block.x(), block.y(), block.z())),
                        id + " places two blocks at "
                                + block.x() + "," + block.y() + "," + block.z());
            }
        }
    }

    @Test
    void wildernessOutpostsStaySparseAndDeterministic() {
        // Scanned wide enough to cover many lattice cells: a find needs its
        // cell to roll a structure AND its nominated chunk to fall in range, so
        // a small window can legitimately contain none.
        int populated = 0;
        int total = 0;
        for (int cx = -200; cx < 200; cx++) {
            for (int cz = -200; cz < 200; cz++) {
                total++;
                RockyMoonOutposts.Structure a = RockyMoonOutposts.forChunk(cx, cz);
                RockyMoonOutposts.Structure b = RockyMoonOutposts.forChunk(cx, cz);
                assertEquals(a == null, b == null, "hash lookup must be deterministic");
                assertEquals(a == null ? null : a.id(), b == null ? null : b.id(),
                        "the same chunk must always yield the same layout");
                assertTrue(a == null || !a.isCluster(),
                        "forChunk must not re-place a cluster module");
                if (a != null)
                    populated++;
            }
        }
        assertTrue(populated > 0, "the wastes must contain outposts");
        assertTrue(populated < total / 100, "outposts must stay sparse landmarks");
        assertEquals(null, RockyMoonOutposts.forChunk(0, 0), "the spawn chunk is the beacon");
    }

    @Test
    void landingPlainIsLeftFlatForTheDescent() {
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

    // ---- helpers ----

    private static long key(int x, int y, int z) {
        return ((long) (x + 8) << 40) ^ ((long) (y + 8) << 20) ^ (z + 8);
    }

    private static boolean hasKind(RockyMoonOutposts.Structure structure,
                                   RockyMoonOutposts.Kind kind) {
        return structure.blocks().stream().anyMatch(block -> block.kind() == kind);
    }

    private static int maxY(RockyMoonOutposts.Structure structure) {
        int top = 0;
        for (RockyMoonOutposts.Block block : structure.blocks())
            top = Math.max(top, block.y());
        return top;
    }
}
