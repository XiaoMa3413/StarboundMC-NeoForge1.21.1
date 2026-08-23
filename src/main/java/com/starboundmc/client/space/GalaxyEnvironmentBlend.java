package com.starboundmc.client.space;

import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StellarVisualProfile;

/**
 * Reusable continuous environment snapshot derived from nearby system
 * influences. Visuals, navigation UI and future ambient audio read the same
 * weights, so a string system-id change cannot hard-switch the environment.
 */
public final class GalaxyEnvironmentBlend
{
    private final StarSystem[] systems;
    private final float[] influences;
    private int count;
    private int skyTintColor = 0xFFFFFFFF;
    private float skyTintAmount;
    private float radiationLevel;
    private float environmentPresence;
    private StarSystem dominantSystem;
    private float dominantInfluence;

    GalaxyEnvironmentBlend(int capacity)
    {
        systems = new StarSystem[capacity];
        influences = new float[capacity];
    }

    void update(StarSystemResolver.ResolvedStarField field)
    {
        count = field.count();
        float totalWeight = 0.0F;
        float tintR = 0.0F;
        float tintG = 0.0F;
        float tintB = 0.0F;
        float tintStrength = 0.0F;
        float weightedRadiation = 0.0F;
        dominantSystem = null;
        dominantInfluence = 0.0F;

        for (int i = 0; i < count; i++)
        {
            StarSystemResolver.VisibleStar star = field.star(i);
            StarSystem system = star.system();
            float influence = clamp(star.systemInfluence());
            systems[i] = system;
            influences[i] = influence;
            if (influence <= 0.001F)
                continue;

            StellarVisualProfile profile = system.getStellarVisual();
            int color = profile.getCoronaColor();
            tintR += ((color >> 16) & 0xFF) * influence;
            tintG += ((color >> 8) & 0xFF) * influence;
            tintB += (color & 0xFF) * influence;
            float radiation = clamp(profile.getRadiationStrength());
            tintStrength += (0.015F + radiation * 0.075F) * influence;
            weightedRadiation += radiation * influence;
            totalWeight += influence;
            if (influence > dominantInfluence)
            {
                dominantSystem = system;
                dominantInfluence = influence;
            }
        }

        environmentPresence = clamp(totalWeight);
        if (totalWeight <= 0.001F)
        {
            skyTintColor = 0xFFFFFFFF;
            skyTintAmount = 0.0F;
            radiationLevel = 0.0F;
            dominantSystem = null;
            dominantInfluence = 0.0F;
            return;
        }

        int r = Math.round(tintR / totalWeight);
        int g = Math.round(tintG / totalWeight);
        int b = Math.round(tintB / totalWeight);
        skyTintColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        skyTintAmount = tintStrength / totalWeight * environmentPresence;
        radiationLevel = weightedRadiation / totalWeight * environmentPresence;
    }

    void reset()
    {
        count = 0;
        skyTintColor = 0xFFFFFFFF;
        skyTintAmount = 0.0F;
        radiationLevel = 0.0F;
        environmentPresence = 0.0F;
        dominantSystem = null;
        dominantInfluence = 0.0F;
    }

    public float influence(StarSystem system)
    {
        if (system == null)
            return 0.0F;
        for (int i = 0; i < count; i++)
            if (systems[i] == system)
                return influences[i];
        return 0.0F;
    }

    /** Weight available to a future per-system ambient music mixer. */
    public float soundscapeWeight(StarSystem system)
    {
        return influence(system);
    }

    public float deepSpaceWeight()
    {
        return 1.0F - environmentPresence;
    }

    public float environmentPresence()
    {
        return environmentPresence;
    }

    public int skyTintColor()
    {
        return skyTintColor;
    }

    public float skyTintAmount()
    {
        return skyTintAmount;
    }

    public float radiationLevel()
    {
        return radiationLevel;
    }

    public StarSystem dominantSystem()
    {
        return dominantSystem;
    }

    public float dominantInfluence()
    {
        return dominantInfluence;
    }

    private static float clamp(float value)
    {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
