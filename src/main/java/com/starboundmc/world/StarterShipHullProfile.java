package com.starboundmc.world;

/**
 * Pure geometry for the compact starter scout hull.
 *
 * <p>The ship points toward +Z. Each longitudinal slice defines the solid
 * volume of the central hull; {@link ShipStructure} decides which palette
 * block to use for the exposed shell. Keeping the profile free of Minecraft
 * classes makes the silhouette cheap to test and tune.</p>
 */
final class StarterShipHullProfile
{
    static final int MIN_X = -6;
    static final int MAX_X = 6;
    static final int MIN_Y = 99;
    static final int MAX_Y = 106;
    static final int MIN_Z = -9;
    static final int MAX_Z = 11;

    static final int FLOOR_Y = 100;

    private static final Slice[] SLICES = {
            // z = -9 .. 11: compact engine block, shared cabin, tapered bow.
            new Slice(2, 101, 105),
            new Slice(3, 100, 106),
            new Slice(3, 100, 106),
            new Slice(3, 100, 106),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 105),
            new Slice(4, 100, 106),
            new Slice(4, 100, 106),
            new Slice(3, 100, 106),
            new Slice(3, 100, 106),
            new Slice(3, 100, 105),
            new Slice(2, 101, 104),
            new Slice(1, 102, 103),
            new Slice(0, 102, 102)
    };

    private StarterShipHullProfile()
    {
    }

    static int length()
    {
        return MAX_Z - MIN_Z + 1;
    }

    static int maximumWidth()
    {
        return MAX_X - MIN_X + 1;
    }

    static int maximumHeight()
    {
        return MAX_Y - MIN_Y + 1;
    }

    static Slice sliceAt(int z)
    {
        if (z < MIN_Z || z > MAX_Z)
            return null;
        return SLICES[z - MIN_Z];
    }

    static boolean containsMainVolume(int x, int y, int z)
    {
        Slice slice = sliceAt(z);
        return slice != null
                && Math.abs(x) <= slice.halfWidth()
                && y >= slice.floorY()
                && y <= slice.roofY();
    }

    static boolean isMainShell(int x, int y, int z)
    {
        if (!containsMainVolume(x, y, z))
            return false;
        return !containsMainVolume(x - 1, y, z)
                || !containsMainVolume(x + 1, y, z)
                || !containsMainVolume(x, y - 1, z)
                || !containsMainVolume(x, y + 1, z)
                || !containsMainVolume(x, y, z - 1)
                || !containsMainVolume(x, y, z + 1);
    }

    static boolean containsEnginePod(int x, int y, int z)
    {
        int absX = Math.abs(x);
        if (absX < 5 || absX > 6 || z < -8 || z > -4)
            return false;
        int floorY = z == -8 ? 101 : FLOOR_Y;
        int roofY = switch (z)
        {
            case -8 -> 105;
            case -4 -> 104;
            default -> 106;
        };
        return y >= floorY && y <= roofY;
    }

    static boolean isKeel(int x, int y, int z)
    {
        return x == 0 && y == MIN_Y && z >= -4 && z <= 3;
    }

    static boolean isCoreCabinColumn(int x, int z)
    {
        return Math.abs(x) <= 2 && z >= -6 && z <= 3;
    }

    record Slice(int halfWidth, int floorY, int roofY)
    {
    }
}
