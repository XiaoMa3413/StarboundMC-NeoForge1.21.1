package com.starboundmc.space;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Continuous-universe position stored as an integer sector plus a small local
 * double coordinate. Absolute values are never converted to GPU floats.
 */
public final class UniversePosition
{
    public static final double SECTOR_SIZE = 100_000.0;
    public static final double HALF_SECTOR_SIZE = SECTOR_SIZE * 0.5;

    private final SectorCoordinate sector;
    private final double localX;
    private final double localY;
    private final double localZ;

    private UniversePosition(SectorCoordinate sector, double localX, double localY, double localZ)
    {
        this.sector = sector;
        this.localX = localX;
        this.localY = localY;
        this.localZ = localZ;
    }

    public static UniversePosition of(SectorCoordinate sector, double localX, double localY, double localZ)
    {
        Objects.requireNonNull(sector, "sector");
        requireFinite(localX, localY, localZ);

        long shiftX = sectorShift(localX);
        long shiftY = sectorShift(localY);
        long shiftZ = sectorShift(localZ);
        SectorCoordinate normalizedSector = sector.offset(shiftX, shiftY, shiftZ);
        return new UniversePosition(normalizedSector,
                normalizeLocal(localX, shiftX),
                normalizeLocal(localY, shiftY),
                normalizeLocal(localZ, shiftZ));
    }

    public static UniversePosition of(double x, double y, double z)
    {
        return of(SectorCoordinate.ZERO, x, y, z);
    }

    /** Compatibility adapter: all existing virtual coordinates start in sector zero. */
    public static UniversePosition fromLegacy(Vec3 position)
    {
        Objects.requireNonNull(position, "position");
        return of(position.x, position.y, position.z);
    }

    public UniversePosition add(UniverseDelta delta)
    {
        Objects.requireNonNull(delta, "delta");
        return of(sector, localX + delta.x(), localY + delta.y(), localZ + delta.z());
    }

    public UniversePosition subtract(UniverseDelta delta)
    {
        Objects.requireNonNull(delta, "delta");
        return of(sector, localX - delta.x(), localY - delta.y(), localZ - delta.z());
    }

    /** Returns {@code target - this} without constructing a huge absolute coordinate first. */
    public UniverseDelta deltaTo(UniversePosition target)
    {
        Objects.requireNonNull(target, "target");
        return new UniverseDelta(deltaXTo(target), deltaYTo(target), deltaZTo(target));
    }

    public double deltaXTo(UniversePosition target)
    {
        Objects.requireNonNull(target, "target");
        return ((double) target.sector.x() - (double) sector.x()) * SECTOR_SIZE
                + target.localX - localX;
    }

    public double deltaYTo(UniversePosition target)
    {
        Objects.requireNonNull(target, "target");
        return ((double) target.sector.y() - (double) sector.y()) * SECTOR_SIZE
                + target.localY - localY;
    }

    public double deltaZTo(UniversePosition target)
    {
        Objects.requireNonNull(target, "target");
        return ((double) target.sector.z() - (double) sector.z()) * SECTOR_SIZE
                + target.localZ - localZ;
    }

    public double distanceToSqr(UniversePosition target)
    {
        double dx = deltaXTo(target);
        double dy = deltaYTo(target);
        double dz = deltaZTo(target);
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean sameSector(UniversePosition other)
    {
        return other != null && sector.equals(other.sector);
    }

    public SectorCoordinate sector()
    {
        return sector;
    }

    public double localX()
    {
        return localX;
    }

    public double localY()
    {
        return localY;
    }

    public double localZ()
    {
        return localZ;
    }

    /** Compatibility view of this position inside its current sector. */
    public Vec3 toLocalVec3()
    {
        return new Vec3(localX, localY, localZ);
    }

    private static long sectorShift(double local)
    {
        double shift = Math.floor((local + HALF_SECTOR_SIZE) / SECTOR_SIZE);
        if (shift < Long.MIN_VALUE || shift > Long.MAX_VALUE)
            throw new IllegalArgumentException("Local universe coordinate exceeds sector range: " + local);
        return (long) shift;
    }

    private static double normalizeLocal(double local, long sectorShift)
    {
        double normalized = local - sectorShift * SECTOR_SIZE;
        // Absorb the rare rounding case exactly on the positive boundary.
        return normalized >= HALF_SECTOR_SIZE ? normalized - SECTOR_SIZE : normalized;
    }

    private static void requireFinite(double x, double y, double z)
    {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Universe position must be finite");
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (!(object instanceof UniversePosition other))
            return false;
        return sector.equals(other.sector)
                && Double.compare(localX, other.localX) == 0
                && Double.compare(localY, other.localY) == 0
                && Double.compare(localZ, other.localZ) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sector, localX, localY, localZ);
    }

    @Override
    public String toString()
    {
        return "UniversePosition[sector=" + sector + ", local=(" + localX + ", " + localY + ", " + localZ + ")]";
    }
}
