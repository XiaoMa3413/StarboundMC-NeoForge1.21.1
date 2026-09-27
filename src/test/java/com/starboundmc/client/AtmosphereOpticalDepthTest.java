package com.starboundmc.client;

import com.starboundmc.world.universe.BodySpaceVisualProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmosphereOpticalDepthTest
{
    private static final float OUTER_MESH_RADIUS = PlanetRenderer.PLANET_RADIUS;
    private static final float SHELL_SCALE = BodySpaceVisualProfile.DEFAULT_ATMOSPHERE_SHELL_SCALE;
    private static final AtmosphereOpticalDepth.Radii RADII =
            AtmosphereOpticalDepth.forShell(OUTER_MESH_RADIUS, SHELL_SCALE);

    @Test
    void shellRadiiMatchPlanetMeshAndAtmosphereScale()
    {
        assertEquals(OUTER_MESH_RADIUS, RADII.outerRadius(), 0.0F);
        assertEquals(OUTER_MESH_RADIUS / SHELL_SCALE, RADII.innerRadius(), 1.0E-5F);
        assertEquals(2.0F * (float) Math.sqrt(
                RADII.outerRadius() * RADII.outerRadius()
                        - RADII.innerRadius() * RADII.innerRadius()),
                RADII.maxOpticalDepth(), 1.0E-5F);
    }

    @Test
    void inwardOverlapBiasAlsoDefinesTheOpticalDepthNormalization()
    {
        AtmosphereOpticalDepth.Radii exact =
                AtmosphereOpticalDepth.forShell(OUTER_MESH_RADIUS, SHELL_SCALE);
        AtmosphereOpticalDepth.Radii biased =
                AtmosphereOpticalDepth.forShellWithInnerOverlap(OUTER_MESH_RADIUS, SHELL_SCALE, 0.999F);

        assertTrue(biased.innerRadius() < exact.innerRadius());
        float expectedMaxDepth = 2.0F * (float) Math.sqrt(
                biased.outerRadius() * biased.outerRadius()
                        - biased.innerRadius() * biased.innerRadius());
        assertEquals(expectedMaxDepth, biased.maxOpticalDepth(), 1.0E-5F);
        assertTrue(biased.maxOpticalDepth() > exact.maxOpticalDepth());
    }

    @Test
    void thinMediumAndThickProfilesUseTheirOwnShellRadii()
    {
        AtmosphereOpticalDepth.Radii thin = AtmosphereOpticalDepth.forShell(OUTER_MESH_RADIUS, 1.020F);
        AtmosphereOpticalDepth.Radii medium = AtmosphereOpticalDepth.forShell(OUTER_MESH_RADIUS, 1.055F);
        AtmosphereOpticalDepth.Radii thick = AtmosphereOpticalDepth.forShell(OUTER_MESH_RADIUS, 1.070F);

        assertEquals(OUTER_MESH_RADIUS / 1.020F, thin.innerRadius(), 1.0E-5F);
        assertEquals(OUTER_MESH_RADIUS / 1.055F, medium.innerRadius(), 1.0E-5F);
        assertEquals(OUTER_MESH_RADIUS / 1.070F, thick.innerRadius(), 1.0E-5F);
        assertTrue(thin.maxOpticalDepth() < medium.maxOpticalDepth());
        assertTrue(medium.maxOpticalDepth() < thick.maxOpticalDepth());
    }

    @Test
    void frontShellPathInsidePlanetDiskIsOnlyTheVisibleSide()
    {
        assertEquals(RADII.outerRadius() - RADII.innerRadius(),
                AtmosphereOpticalDepth.pathLength(0.0F, RADII), 1.0E-5F);

        float impact = RADII.innerRadius() * 0.5F;
        float outerPath = (float) Math.sqrt(
                RADII.outerRadius() * RADII.outerRadius() - impact * impact);
        float innerPath = (float) Math.sqrt(
                RADII.innerRadius() * RADII.innerRadius() - impact * impact);

        assertEquals(outerPath - innerPath,
                AtmosphereOpticalDepth.pathLength(impact, RADII), 1.0E-5F);
    }

    @Test
    void pathUsesFrontOnlyThenFullChordAcrossThePlanetLimb()
    {
        float justInside = RADII.innerRadius() - 0.01F;
        float expectedFront = (float) Math.sqrt(
                RADII.outerRadius() * RADII.outerRadius() - justInside * justInside)
                - (float) Math.sqrt(
                RADII.innerRadius() * RADII.innerRadius() - justInside * justInside);
        assertEquals(expectedFront, AtmosphereOpticalDepth.pathLength(justInside, RADII), 1.0E-5F);

        float justOutside = RADII.innerRadius() + 0.01F;
        float expectedChord = 2.0F * (float) Math.sqrt(
                RADII.outerRadius() * RADII.outerRadius() - justOutside * justOutside);
        assertEquals(expectedChord, AtmosphereOpticalDepth.pathLength(justOutside, RADII), 1.0E-5F);
    }

    @Test
    void atmosphereOnlyChordUsesBothShellSides()
    {
        float impact = (float) Math.sqrt((RADII.innerRadius() * RADII.innerRadius()
                + RADII.outerRadius() * RADII.outerRadius()) * 0.5F);
        float outerPath = (float) Math.sqrt(
                RADII.outerRadius() * RADII.outerRadius() - impact * impact);

        assertEquals(2.0F * outerPath,
                AtmosphereOpticalDepth.pathLength(impact, RADII), 1.0E-5F);
    }

    @Test
    void opticalPathFallsToZeroAtAndBeyondOuterShell()
    {
        assertTrue(AtmosphereOpticalDepth.pathLength(RADII.outerRadius(), RADII) <= 1.0E-5F);
        assertEquals(0.0F, AtmosphereOpticalDepth.pathLength(RADII.outerRadius() + 1.0F, RADII), 0.0F);
    }
}
