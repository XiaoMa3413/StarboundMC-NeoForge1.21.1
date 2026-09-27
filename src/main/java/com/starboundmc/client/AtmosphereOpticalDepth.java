package com.starboundmc.client;

/** Geometry shared with the atmosphere shader for shell-space radii and reference path lengths. */
final class AtmosphereOpticalDepth
{
    private AtmosphereOpticalDepth()
    {
    }

    static Radii forShell(float outerMeshRadius, float shellScale)
    {
        if (!Float.isFinite(outerMeshRadius) || outerMeshRadius <= 0.0F
                || !Float.isFinite(shellScale) || shellScale <= 1.0F)
            throw new IllegalArgumentException("Atmosphere shell radius and scale must be finite and positive");

        float innerRadius = outerMeshRadius / shellScale;
        float tangentPath = (float) Math.sqrt(Math.max(
                outerMeshRadius * outerMeshRadius - innerRadius * innerRadius, 0.0F));
        return new Radii(innerRadius, outerMeshRadius, 2.0F * tangentPath);
    }

    /** Pure reference for the two visible-path cases used in atmosphere.fsh. */
    static float pathLength(float impact, Radii radii)
    {
        float outerPath = (float) Math.sqrt(Math.max(
                radii.outerRadius() * radii.outerRadius() - impact * impact, 0.0F));
        if (impact < radii.innerRadius())
        {
            float innerPath = (float) Math.sqrt(Math.max(
                    radii.innerRadius() * radii.innerRadius() - impact * impact, 0.0F));
            return Math.max(outerPath - innerPath, 0.0F);
        }
        if (impact < radii.outerRadius())
            return 2.0F * outerPath;
        return 0.0F;
    }

    record Radii(float innerRadius, float outerRadius, float maxOpticalDepth)
    {
    }
}
