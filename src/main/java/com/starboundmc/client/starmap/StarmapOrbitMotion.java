package com.starboundmc.client.starmap;

/** Shared client-side orbit timing model used by drawing and hit testing. */
public final class StarmapOrbitMotion
{
    private static final double FULL_TURN = Math.PI * 2.0D;
    /** Radians advanced per client tick by an orbit at the reference radius. */
    public static final float BASE_SPEED = 0.0015F;
    /** Authoring-space radius whose speed is exactly {@link #BASE_SPEED}. */
    public static final float REFERENCE_RADIUS = 52.0F;
    /** Moon authoring radii use a separate scale and should remain readable. */
    public static final float MOON_BASE_SPEED = 0.0030F;
    public static final float MOON_REFERENCE_RADIUS = 20.0F;

    private StarmapOrbitMotion()
    {
    }

    /**
     * Returns radians per tick for the supplied authoring-space orbit radius.
     * Kepler-style angular velocity: a larger orbit has a substantially longer
     * period, proportional to radius to the power of 3/2.
     */
    public static float speedForRadius(float orbitRadius)
    {
        float radius = Math.max(1.0F, orbitRadius);
        return BASE_SPEED * inverseThreeHalves(radius, REFERENCE_RADIUS);
    }

    /**
     * Calculates a phase from a continuous clock. The clock is deliberately a
     * double so a long-running screen does not lose sub-tick precision.
     */
    public static float phase(double orbitClock, float orbitRadius)
    {
        return normalizedPhase(orbitClock, speedForRadius(orbitRadius));
    }

    public static float moonPhase(double orbitClock, float orbitRadius)
    {
        return normalizedPhase(orbitClock, moonSpeedForRadius(orbitRadius));
    }

    public static float moonSpeedForRadius(float orbitRadius)
    {
        float radius = Math.max(1.0F, orbitRadius);
        return MOON_BASE_SPEED * inverseThreeHalves(radius, MOON_REFERENCE_RADIUS);
    }

    private static float inverseThreeHalves(float radius, float referenceRadius)
    {
        double ratio = referenceRadius / radius;
        return (float) (ratio * Math.sqrt(ratio));
    }

    private static float normalizedPhase(double orbitClock, double speed)
    {
        double phase = orbitClock * speed % FULL_TURN;
        if (phase < 0.0D)
            phase += FULL_TURN;
        return (float) phase;
    }
}
