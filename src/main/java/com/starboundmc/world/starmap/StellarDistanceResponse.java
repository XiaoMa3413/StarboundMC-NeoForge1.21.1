package com.starboundmc.world.starmap;

/**
 * Artistic distance curve for a star. It intentionally amplifies visible size
 * changes compared with a physical inverse-distance projection.
 */
public record StellarDistanceResponse(double referenceDistance, float baseSkyRadius,
                                      float minimumLocalScale, float maximumLocalScale,
                                      float distanceResponseExponent, float remotePointRadius,
                                      float coronaActivation, float effectActivation)
{
    public StellarDistanceResponse
    {
        if (!Double.isFinite(referenceDistance) || referenceDistance <= 0.0)
            throw new IllegalArgumentException("referenceDistance must be positive and finite");
        if (!Float.isFinite(baseSkyRadius) || baseSkyRadius <= 0.0F)
            throw new IllegalArgumentException("baseSkyRadius must be positive and finite");
        if (!Float.isFinite(minimumLocalScale) || minimumLocalScale <= 0.0F
                || !Float.isFinite(maximumLocalScale) || maximumLocalScale < minimumLocalScale)
            throw new IllegalArgumentException("invalid local scale bounds");
        if (!Float.isFinite(distanceResponseExponent) || distanceResponseExponent <= 0.0F)
            throw new IllegalArgumentException("distanceResponseExponent must be positive and finite");
        if (!Float.isFinite(remotePointRadius) || remotePointRadius <= 0.0F)
            throw new IllegalArgumentException("remotePointRadius must be positive and finite");
        if (!Float.isFinite(coronaActivation) || coronaActivation < 0.0F || coronaActivation >= 1.0F
                || !Float.isFinite(effectActivation)
                || effectActivation < coronaActivation || effectActivation >= 1.0F)
            throw new IllegalArgumentException("invalid stellar effect activation thresholds");
    }

    public float distanceScale(double distance)
    {
        double safeDistance = Math.max(1.0, distance);
        double raw = Math.pow(referenceDistance / safeDistance, distanceResponseExponent);
        return clamp((float) raw, minimumLocalScale, maximumLocalScale);
    }

    public float localSkyRadius(double distance)
    {
        return baseSkyRadius * distanceScale(distance);
    }

    public float skyRadius(double distance, float systemInfluence)
    {
        float influence = clamp(systemInfluence, 0.0F, 1.0F);
        return lerp(remotePointRadius, localSkyRadius(distance), influence);
    }

    public float coronaWeight(float systemInfluence)
    {
        return activationWeight(systemInfluence, coronaActivation);
    }

    public float effectWeight(float systemInfluence)
    {
        return activationWeight(systemInfluence, effectActivation);
    }

    private static float activationWeight(float influence, float threshold)
    {
        float t = clamp((influence - threshold) / (1.0F - threshold), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float from, float to, float amount)
    {
        return from + (to - from) * amount;
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
