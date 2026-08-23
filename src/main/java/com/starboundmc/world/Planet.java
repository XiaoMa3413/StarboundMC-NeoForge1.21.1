package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import net.minecraft.resources.ResourceLocation;

/**
 * A destination planet the ship can orbit. The ship itself never moves; warping
 * simply swaps which planet texture is rendered below the ship.
 */
public enum Planet
{
    LUSH("lush"),
    MOLTEN("molten"),
    FROZEN("frozen"),
    BARREN("barren");

    private final String id;

    Planet(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public ResourceLocation texture()
    {
        return ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "textures/planet/" + id + ".png");
    }

    public String translationKey()
    {
        return "planet." + StarboundMC.MODID + "." + id;
    }

    public static Planet fromId(String id)
    {
        for (Planet planet : values())
        {
            if (planet.id.equals(id))
                return planet;
        }
        return LUSH;
    }

    /** Threat level shown on the star map (1 = safest, 10 = deadliest). */
    public int threatLevel()
    {
        switch (this)
        {
            case LUSH:   return 1;
            case BARREN: return 2;
            case FROZEN: return 4;
            case MOLTEN: return 6;
        }
        return 0;
    }
}
