package com.starboundmc.client.space;

/** Rendering and resource tier for a planet or other non-stellar body. */
public enum CelestialLod
{
    /** Textured sphere, atmosphere and all body-specific details. */
    FULL,
    /** Textured sphere without the expensive secondary effects. */
    REDUCED,
    /** Small camera-facing marker; no body mesh or texture upload. */
    POINT,
    /** Outside the useful visual range. Metadata remains available. */
    CULLED;

    /** Returns true when this level is at least as visually detailed as other. */
    public boolean atLeast(CelestialLod other)
    {
        return rank(this) >= rank(other);
    }

    private static int rank(CelestialLod level)
    {
        return switch (level)
        {
            case CULLED -> 0;
            case POINT -> 1;
            case REDUCED -> 2;
            case FULL -> 3;
        };
    }
}
