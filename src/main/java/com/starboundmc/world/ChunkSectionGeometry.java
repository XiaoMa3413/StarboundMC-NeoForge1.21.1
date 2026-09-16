package com.starboundmc.world;

/**
 * Conversions between a chunk's section <em>array index</em> and world Y.
 *
 * <p>These two are not the same number and confusing them is silent. A chunk's
 * section array is always indexed from zero, while the world Y of its bottom
 * section is {@code minBuildHeight} — {@code -64} for the overworld-shaped
 * dimensions and {@code 0} for the nether-shaped ones. So on the Rocky Moon,
 * section array index 0 sits at world Y -64, and treating the index as a
 * section-Y coordinate reports every block 64 blocks too high.</p>
 *
 * <p>Deliberately free of Minecraft types so bootstrap-free unit tests can pin
 * the arithmetic; the callers pass {@code ChunkAccess#getMinSection()} and
 * {@code #getMinBuildHeight()} straight in.</p>
 */
public final class ChunkSectionGeometry
{
    /** Blocks per chunk section on the vertical axis. */
    public static final int SECTION_HEIGHT = 16;

    private ChunkSectionGeometry()
    {
    }

    /**
     * World Y of the bottom of the section at {@code sectionIndex} in a chunk
     * whose lowest section is {@code minSection}.
     *
     * @param minSection   {@code ChunkAccess#getMinSection()}, i.e.
     *                     {@code floorDiv(minBuildHeight, 16)}
     * @param sectionIndex index into {@code ChunkAccess#getSections()}, from 0
     */
    public static int sectionBottomY(int minSection, int sectionIndex)
    {
        return (sectionIndex + minSection) * SECTION_HEIGHT;
    }

    /**
     * World Y of a block addressed by section array index and in-section offset,
     * measured from that section's bottom.
     *
     * @param inSectionY in-section offset, 0..15
     */
    public static int blockY(int minSection, int sectionIndex, int inSectionY)
    {
        return sectionBottomY(minSection, sectionIndex) + inSectionY;
    }

    /** Inverse of {@link #sectionBottomY}: the array index holding {@code y}. */
    public static int sectionIndex(int minSection, int y)
    {
        return Math.floorDiv(y, SECTION_HEIGHT) - minSection;
    }
}
