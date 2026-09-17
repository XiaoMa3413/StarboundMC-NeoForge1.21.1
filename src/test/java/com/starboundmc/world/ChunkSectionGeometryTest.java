package com.starboundmc.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the section-array-index to world-Y arithmetic.
 *
 * <p>This conversion had a silent off-by-{@code minBuildHeight} bug: the moon's
 * generator derived every terrain height as {@code sectionIndex * 16 + y},
 * omitting the array's offset, and so reported every column 64 blocks too high.
 * The craters were never excavated, the outposts were stamped in the sky, and
 * nothing caught it because the mistake is invisible in any dimension whose
 * {@code min_y} happens to be 0. These tests assert the non-zero case.</p>
 */
final class ChunkSectionGeometryTest
{
    /** Rocky Moon / Barren: overworld-shaped, so the array starts at section -4. */
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_MIN_SECTION = -4;
    /** Nether-shaped dimensions start the array at section 0. */
    private static final int NETHER_MIN_Y = 0;
    private static final int NETHER_MIN_SECTION = 0;

    @Test
    void sectionBottomIsMeasuredFromTheChunksOwnMinimum()
    {
        // Section array index 0 is where the chunk starts, not world Y 0. This
        // is the exact assertion the floating-terrain bug violated.
        assertEquals(-64, ChunkSectionGeometry.sectionBottomY(OVERWORLD_MIN_SECTION, 0));
        assertEquals(0, ChunkSectionGeometry.sectionBottomY(NETHER_MIN_SECTION, 0));
        // Reading a section index as a section-Y coordinate is what broke:
        // index 0 is NOT world Y 0 in an overworld-shaped dimension.
        assertNotEquals(0, ChunkSectionGeometry.sectionBottomY(OVERWORLD_MIN_SECTION, 0));
    }

    @Test
    void blockYIsTheSectionBottomPlusTheInSectionOffset()
    {
        assertEquals(-64, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 0, 0));
        assertEquals(-49, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 0, 15));
        assertEquals(-48, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 1, 0));
        assertEquals(0, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 4, 0));
        assertEquals(80, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 9, 0));
        assertEquals(95, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, 9, 15));
    }

    @Test
    void sectionIndexIsTheInverseOfSectionBottom()
    {
        for (int sectionY = OVERWORLD_MIN_SECTION; sectionY < OVERWORLD_MIN_SECTION + 24; sectionY++)
        {
            int bottom = ChunkSectionGeometry.sectionBottomY(OVERWORLD_MIN_SECTION, sectionY);
            assertEquals(sectionY, ChunkSectionGeometry.sectionIndex(OVERWORLD_MIN_SECTION, bottom),
                    "round trip failed for section " + sectionY);
            // Every block inside the section must map back to it.
            assertEquals(sectionY,
                    ChunkSectionGeometry.sectionIndex(OVERWORLD_MIN_SECTION, bottom + 15));
        }
    }

    @Test
    void aSurfaceInsideTheMoonAlwaysMapsBackToItsOwnSection()
    {
        // The moon's playable surface is nowhere near Y 0, so the round trip has
        // to hold across the whole built range, and the index must never be
        // mistaken for the coordinate.
        for (int y = OVERWORLD_MIN_Y; y < OVERWORLD_MIN_Y + 384; y += 7)
        {
            int index = ChunkSectionGeometry.sectionIndex(OVERWORLD_MIN_SECTION, y);
            assertTrue(index >= 0, "array index must be non-negative, got " + index + " for y " + y);
            assertTrue(index < 24, "array index must fit the moon's 24 sections, got " + index);
            int inSection = y - ChunkSectionGeometry.sectionBottomY(OVERWORLD_MIN_SECTION, index);
            assertTrue(inSection >= 0 && inSection < 16, "in-section offset out of range: " + inSection);
            assertEquals(y, ChunkSectionGeometry.blockY(OVERWORLD_MIN_SECTION, index, inSection));
        }
    }

    @Test
    void theMoonDeclaresAnOverworldShape()
    {
        // Guards the premise the conversion depends on: if the rocky moon's
        // min_y were ever changed to 0, the tests above would stop covering the
        // bug they exist for.
        assertEquals(OVERWORLD_MIN_Y, OVERWORLD_MIN_SECTION * ChunkSectionGeometry.SECTION_HEIGHT);
    }
}
