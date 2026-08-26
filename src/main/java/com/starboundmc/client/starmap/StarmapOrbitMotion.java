package com.starboundmc.client.starmap;

/** Shared client-side orbit timing model used by drawing and hit testing. */
public final class StarmapOrbitMotion
{
    private static final double FULL_TURN = Math.PI * 2.0D;
    /** Radians advanced per client tick by an orbit at the reference radius. */
    public static final float BASE_SPEED = 0.0035F;
    /** Authoring-space radius whose speed is exactly {@link #BASE_SPEED}. */
    public static final float REFERENCE_RADIUS = 52.0F;
    /** Moons orbit faster than planets, while still remaining deliberately calm. */
    public static final float MOON_SPEED_MULTIPLIER = 1.45F;

    private StarmapOrbitMotion()
    {
    }

    /**
     * Returns radians per tick for the supplied authoring-space orbit radius.
     * The square-root falloff is a readable Kepler-style approximation.
     */
    public static float speedForRadius(float orbitRadius)
    {
        float radius = Math.max(1.0F, orbitRadius);
        return BASE_SPEED * (float) Math.sqrt(REFERENCE_RADIUS / radius);
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
        return normalizedPhase(orbitClock,
                speedForRadius(orbitRadius) * MOON_SPEED_MULTIPLIER);
    }

    private static float normalizedPhase(double orbitClock, double speed)
    {
        double phase = orbitClock * speed % FULL_TURN;
        if (phase < 0.0D)
            phase += FULL_TURN;
        return (float) phase;
    }
}
