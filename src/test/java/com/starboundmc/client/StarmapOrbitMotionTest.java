package com.starboundmc.client;

import com.starboundmc.client.starmap.StarmapOrbitMotion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapOrbitMotionTest
{
    @Test
    void slowsTheReferenceOrbitWellBelowThePreviousBaseline()
    {
        assertEquals(0.0015F, StarmapOrbitMotion.speedForRadius(52.0F), 0.000001F);
        assertTrue(StarmapOrbitMotion.speedForRadius(52.0F) < 0.0035F);
    }

    @Test
    void appliesAVisibleKeplerStyleFalloffAcrossPlanetaryOrbits()
    {
        float inner = StarmapOrbitMotion.speedForRadius(52.0F);
        float middle = StarmapOrbitMotion.speedForRadius(84.0F);
        float outer = StarmapOrbitMotion.speedForRadius(116.0F);

        assertTrue(middle < inner);
        assertTrue(outer < middle);
        assertTrue(inner / outer > 3.0F);
        assertEquals(inner * Math.pow(52.0D / 116.0D, 1.5D), outer, 0.000001D);
    }

    @Test
    void slowsMoonOrbitsAndStillMakesTheOuterMoonSlower()
    {
        float innerMoon = StarmapOrbitMotion.moonSpeedForRadius(20.0F);
        float outerMoon = StarmapOrbitMotion.moonSpeedForRadius(22.0F);

        assertEquals(0.0030F, innerMoon, 0.000001F);
        assertTrue(outerMoon < innerMoon);
        assertTrue(innerMoon < 0.0040F);
        float moon = StarmapOrbitMotion.moonPhase(1.0F, 20.0F);
        assertEquals(innerMoon, moon, 0.000001F);
    }

    @Test
    void acceptsFractionalRenderClockForContinuousMotion()
    {
        float previous = StarmapOrbitMotion.phase(12.0D, 84.0F);
        float interpolated = StarmapOrbitMotion.phase(12.5D, 84.0F);
        float next = StarmapOrbitMotion.phase(13.0D, 84.0F);

        assertTrue(previous < interpolated);
        assertTrue(interpolated < next);
        assertEquals((previous + next) * 0.5F, interpolated, 0.000001F);
    }

    @Test
    void keepsSubTickPrecisionAfterLongRunningSessions()
    {
        double clock = 1_000_000_000.0D;
        float previous = StarmapOrbitMotion.phase(clock, 84.0F);
        float interpolated = StarmapOrbitMotion.phase(clock + 0.5D, 84.0F);
        double angularDelta = Math.atan2(Math.sin(interpolated - previous),
                Math.cos(interpolated - previous));

        assertTrue(Float.isFinite(previous));
        assertTrue(previous >= 0.0F && previous < Math.PI * 2.0D);
        assertEquals(StarmapOrbitMotion.speedForRadius(84.0F) * 0.5D,
                angularDelta, 0.000001D);
    }
}
