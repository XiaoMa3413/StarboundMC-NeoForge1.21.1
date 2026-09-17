package com.starboundmc.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Integer address of one fixed-size sector in the continuous universe. */
public record SectorCoordinate(long x, long y, long z)
{
    public static final SectorCoordinate ZERO = new SectorCoordinate(0L, 0L, 0L);

    /**
     * Datapack form. Sector values stay well inside int range for any authored
     * universe, but the wire type is long, so the codec uses {@code long} to
     * avoid a silent narrowing when a definition is hand-written.
     */
    public static final Codec<SectorCoordinate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("x").forGetter(SectorCoordinate::x),
            Codec.LONG.fieldOf("y").forGetter(SectorCoordinate::y),
            Codec.LONG.fieldOf("z").forGetter(SectorCoordinate::z)
    ).apply(instance, SectorCoordinate::new));

    public SectorCoordinate offset(long dx, long dy, long dz)
    {
        if (dx == 0L && dy == 0L && dz == 0L)
            return this;
        return new SectorCoordinate(Math.addExact(x, dx), Math.addExact(y, dy), Math.addExact(z, dz));
    }
}
