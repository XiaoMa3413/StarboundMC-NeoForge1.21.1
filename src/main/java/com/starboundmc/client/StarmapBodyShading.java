package com.starboundmc.client;

import com.starboundmc.world.starmap.StarmapBodyVisual;

/** Pure colour calculation for the shared procedural sphere renderer. */
public final class StarmapBodyShading
{
    private static final double LIGHT_SCREEN_COMPONENT = 0.58D;
    private static final double LIGHT_DEPTH_COMPONENT = Math.sqrt(
            1.0D - LIGHT_SCREEN_COMPONENT * LIGHT_SCREEN_COMPONENT);

    private StarmapBodyShading()
    {
    }

    /**
     * Shades one normalized point on a body. Local x/y and light x/y use
     * screen-space directions where {@code -1..1} spans the visible disc.
     */
    public static int shade(StarmapBodyVisual visual, double localX, double localY,
                            double lightDirectionX, double lightDirectionY)
    {
        double radialSquared = localX * localX + localY * localY;
        if (radialSquared > 1.0D)
        {
            double inverseRadius = 1.0D / Math.sqrt(radialSquared);
            localX *= inverseRadius;
            localY *= inverseRadius;
            radialSquared = 1.0D;
        }

        double localZ = Math.sqrt(Math.max(0.0D, 1.0D - radialSquared));
        double lightLength = Math.hypot(lightDirectionX, lightDirectionY);
        double lightX = lightLength < 0.0001D
                ? -LIGHT_SCREEN_COMPONENT * 0.70710678D
                : lightDirectionX / lightLength * LIGHT_SCREEN_COMPONENT;
        double lightY = lightLength < 0.0001D
                ? -LIGHT_SCREEN_COMPONENT * 0.70710678D
                : lightDirectionY / lightLength * LIGHT_SCREEN_COMPONENT;
        double diffuse = Math.max(0.0D,
                localX * lightX + localY * lightY + localZ * LIGHT_DEPTH_COMPONENT);

        double detail = detailPattern(localX, localY, visual.getTextureSeed())
                * visual.getSurfaceDetail() * 0.22D;
        int primary = visual.getPrimaryColor();
        int secondary = visual.getSecondaryColor();
        double brightness = 0.48D + diffuse * 0.55D + Math.pow(diffuse, 10.0D) * 0.10D;
        double rim = Math.pow(1.0D - localZ, 2.0D) * 0.14D;

        int red = shadedChannel(channel(primary, 16), channel(secondary, 16),
                detail, brightness, rim);
        int green = shadedChannel(channel(primary, 8), channel(secondary, 8),
                detail, brightness, rim);
        int blue = shadedChannel(channel(primary, 0), channel(secondary, 0),
                detail, brightness, rim);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    /** Applies the same directional sphere lighting to a full-colour sprite sample. */
    public static int shadeTexture(int color, double localX, double localY,
                                   double lightDirectionX, double lightDirectionY)
    {
        double radialSquared = localX * localX + localY * localY;
        if (radialSquared > 1.0D)
        {
            double inverseRadius = 1.0D / Math.sqrt(radialSquared);
            localX *= inverseRadius;
            localY *= inverseRadius;
            radialSquared = 1.0D;
        }
        double localZ = Math.sqrt(Math.max(0.0D, 1.0D - radialSquared));
        double lightLength = Math.hypot(lightDirectionX, lightDirectionY);
        double lightX = lightLength < 0.0001D
                ? -LIGHT_SCREEN_COMPONENT * 0.70710678D
                : lightDirectionX / lightLength * LIGHT_SCREEN_COMPONENT;
        double lightY = lightLength < 0.0001D
                ? -LIGHT_SCREEN_COMPONENT * 0.70710678D
                : lightDirectionY / lightLength * LIGHT_SCREEN_COMPONENT;
        double diffuse = Math.max(0.0D,
                localX * lightX + localY * lightY + localZ * LIGHT_DEPTH_COMPONENT);
        double brightness = 0.42D + diffuse * 0.64D + Math.pow(diffuse, 10.0D) * 0.08D;
        double rim = Math.pow(1.0D - localZ, 2.0D) * 0.08D;
        int alpha = color >>> 24 & 0xFF;
        int red = texturedChannel(channel(color, 16), brightness, rim);
        int green = texturedChannel(channel(color, 8), brightness, rim);
        int blue = texturedChannel(channel(color, 0), brightness, rim);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    static double detailPattern(double x, double y, long seed)
    {
        double phaseA = ((seed >>> 8) & 0xFFFFL) / 65535.0D * Math.PI * 2.0D;
        double phaseB = ((seed >>> 24) & 0xFFFFL) / 65535.0D * Math.PI * 2.0D;
        double broad = Math.sin(x * 4.1D + y * 2.7D + phaseA);
        double cross = Math.sin(x * -2.3D + y * 5.2D + phaseB);
        return clamp01(0.5D + broad * 0.30D + cross * 0.20D);
    }

    private static int shadedChannel(int primary, int secondary, double detail,
                                     double brightness, double rim)
    {
        double base = primary + (secondary - primary) * detail;
        return clampChannel((int) Math.round(base * brightness + (255.0D - base) * rim));
    }

    private static int texturedChannel(int base, double brightness, double rim)
    {
        return clampChannel((int) Math.round(base * brightness + (255.0D - base) * rim));
    }

    private static int channel(int color, int shift)
    {
        return color >>> shift & 0xFF;
    }

    private static int clampChannel(int value)
    {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp01(double value)
    {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
