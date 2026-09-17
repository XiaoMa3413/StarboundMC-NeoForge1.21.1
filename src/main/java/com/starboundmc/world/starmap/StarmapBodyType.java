package com.starboundmc.world.starmap;

import com.mojang.serialization.Codec;

/** Base procedural template used to draw a non-stellar body on the star map. */
public enum StarmapBodyType
{
    GENERIC,
    ROCKY,
    TERRESTRIAL,
    VOLCANIC,
    GAS_GIANT,
    ICY;

    /**
     * Unknown names are reported as a parse error rather than an exception, so a
     * typo in a datapack names the bad value instead of failing the registry load.
     */
    public static final Codec<StarmapBodyType> CODEC =
            Codec.STRING.comapFlatMap(StarmapBodyType::parse, StarmapBodyType::name);

    static com.mojang.serialization.DataResult<StarmapBodyType> parse(String name)
    {
        for (StarmapBodyType type : values())
        {
            if (type.name().equals(name))
                return com.mojang.serialization.DataResult.success(type);
        }
        return com.mojang.serialization.DataResult.error(
                () -> "Unknown starmap body type: " + name);
    }
}
