package com.starboundmc.client.compat.stellarview;

import java.util.Objects;

import com.starboundmc.space.UniversePosition;

/** Pure coordinate conversion; optional-mod types are created only inside the runtime bridge. */
public final class StellarViewPositionAdapter
{
    public static final double KM_PER_LY = 9_460_730_472_581.2;
    private UniversePosition position = UniversePosition.of(0, 0, 0);

    public record Coordinates(long xLy, long yLy, long zLy, double xKm, double yKm, double zKm) {}

    public void setPosition(UniversePosition position)
    {
        this.position = Objects.requireNonNull(position, "position");
    }

    public Coordinates sample()
    {
        return toSpaceCoords(position);
    }

    /** One virtual unit maps to one external light year; keep fractions before scaling. */
    public static Coordinates toSpaceCoords(UniversePosition position)
    {
        Objects.requireNonNull(position, "position");
        long x = (long) Math.floor(position.localX());
        long y = (long) Math.floor(position.localY());
        long z = (long) Math.floor(position.localZ());
        return new Coordinates(
                axis(position.sector().x(), x),
                axis(position.sector().y(), y),
                axis(position.sector().z(), z),
                (position.localX() - x) * KM_PER_LY,
                (position.localY() - y) * KM_PER_LY,
                (position.localZ() - z) * KM_PER_LY);
    }

    private static long axis(long sector, long local)
    {
        return Math.addExact(Math.multiplyExact(sector, (long) UniversePosition.SECTOR_SIZE), local);
    }
}
