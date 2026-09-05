package com.starboundmc.client.space;

/**
 * Distance-to-quality policy for planets, moons and future non-stellar bodies.
 *
 * <p>Angular size is used instead of a raw world distance. A large planet and
 * a small moon therefore receive the same visual quality when they occupy the
 * same amount of the player's view.</p>
 */
public final class CelestialLodPolicy
{
    /** Entry thresholds. Exit thresholds below provide a small hysteresis band. */
    public static final double FULL_ENTER_DEGREES = 2.50;
    public static final double REDUCED_ENTER_DEGREES = 0.50;
    public static final double POINT_ENTER_DEGREES = 0.08;

    public static final double FULL_EXIT_DEGREES = 2.10;
    public static final double REDUCED_EXIT_DEGREES = 0.42;
    public static final double POINT_EXIT_DEGREES = 0.065;

    private CelestialLodPolicy()
    {
    }

    /** Returns the apparent diameter in degrees for a body at the given distance. */
    public static double angularDiameterDegrees(double radius, double distance)
    {
        if (!Double.isFinite(radius) || radius <= 0.0)
            throw new IllegalArgumentException("radius must be positive and finite");
        if (!Double.isFinite(distance) || distance < 0.0)
            throw new IllegalArgumentException("distance must be finite and non-negative");
        double angle = 2.0 * Math.atan2(radius, Math.max(0.0, distance));
        return Math.toDegrees(angle);
    }

    /** Classifies a body without a forced visibility override. */
    public static CelestialLod classify(double radius, double distance)
    {
        return classifyAngularSize(angularDiameterDegrees(radius, distance));
    }

    /** Classifies an already calculated angular diameter. */
    public static CelestialLod classifyAngularSize(double angularDiameterDegrees)
    {
        if (!Double.isFinite(angularDiameterDegrees) || angularDiameterDegrees < 0.0)
            throw new IllegalArgumentException("angular diameter must be finite and non-negative");
        if (angularDiameterDegrees >= FULL_ENTER_DEGREES)
            return CelestialLod.FULL;
        if (angularDiameterDegrees >= REDUCED_ENTER_DEGREES)
            return CelestialLod.REDUCED;
        if (angularDiameterDegrees >= POINT_ENTER_DEGREES)
            return CelestialLod.POINT;
        return CelestialLod.CULLED;
    }

    /**
     * Applies entry/exit thresholds so a body does not flap between tiers when
     * the ship is close to a boundary. The forced minimum is used by the
     * departure/arrival legs to keep the source and target system coherent.
     */
    public static CelestialLod hysteretic(double angularDiameterDegrees,
                                          CelestialLod previous,
                                          CelestialLod minimum)
    {
        if (!Double.isFinite(angularDiameterDegrees) || angularDiameterDegrees < 0.0)
            throw new IllegalArgumentException("angular diameter must be finite and non-negative");
        CelestialLod prior = previous == null ? CelestialLod.CULLED : previous;
        CelestialLod result;
        switch (prior)
        {
            case FULL -> result = angularDiameterDegrees >= FULL_EXIT_DEGREES
                    ? CelestialLod.FULL : classifyAngularSize(angularDiameterDegrees);
            case REDUCED -> result = angularDiameterDegrees >= REDUCED_EXIT_DEGREES
                    && angularDiameterDegrees < FULL_ENTER_DEGREES
                    ? CelestialLod.REDUCED : classifyAngularSize(angularDiameterDegrees);
            case POINT -> result = angularDiameterDegrees >= POINT_EXIT_DEGREES
                    && angularDiameterDegrees < REDUCED_ENTER_DEGREES
                    ? CelestialLod.POINT : classifyAngularSize(angularDiameterDegrees);
            case CULLED -> result = classifyAngularSize(angularDiameterDegrees);
            default -> result = CelestialLod.CULLED;
        }
        CelestialLod required = minimum == null ? CelestialLod.CULLED : minimum;
        return result.atLeast(required) ? result : required;
    }

    /** Promotes a result to a minimum quality without ever demoting it. */
    public static CelestialLod atLeast(CelestialLod level, CelestialLod minimum)
    {
        CelestialLod value = level == null ? CelestialLod.CULLED : level;
        CelestialLod required = minimum == null ? CelestialLod.CULLED : minimum;
        return value.atLeast(required) ? value : required;
    }
}
