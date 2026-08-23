package com.starboundmc.space;

/** Integer address of one fixed-size sector in the continuous universe. */
public record SectorCoordinate(long x, long y, long z)
{
    public static final SectorCoordinate ZERO = new SectorCoordinate(0L, 0L, 0L);

    public SectorCoordinate offset(long dx, long dy, long dz)
    {
        if (dx == 0L && dy == 0L && dz == 0L)
            return this;
        return new SectorCoordinate(Math.addExact(x, dx), Math.addExact(y, dy), Math.addExact(z, dz));
    }
}
