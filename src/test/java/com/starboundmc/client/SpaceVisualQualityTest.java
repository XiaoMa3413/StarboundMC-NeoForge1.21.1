package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceVisualQualityTest
{
    @Test
    void performanceDisablesCloudsButKeepsAtmosphere()
    {
        assertFalse(SpaceVisualQuality.PERFORMANCE.cloudsEnabled(true));
        assertTrue(SpaceVisualQuality.PERFORMANCE.atmosphereEnabled(false));
        assertFalse(SpaceVisualQuality.PERFORMANCE.cloudShadowsEnabled(true));
    }

    @Test
    void balancedDisablesCloudShadowsWhileHighAndUltraEnableThem()
    {
        for (SpaceVisualQuality quality : new SpaceVisualQuality[]{
                SpaceVisualQuality.BALANCED, SpaceVisualQuality.HIGH, SpaceVisualQuality.ULTRA})
        {
            assertTrue(quality.cloudsEnabled(false));
            assertTrue(quality.atmosphereEnabled(false));
            assertEquals(quality != SpaceVisualQuality.BALANCED, quality.cloudShadowsEnabled(false));
        }
    }

    @Test
    void customUsesEachIndividualToggle()
    {
        assertTrue(SpaceVisualQuality.CUSTOM.cloudsEnabled(true));
        assertFalse(SpaceVisualQuality.CUSTOM.cloudsEnabled(false));
        assertTrue(SpaceVisualQuality.CUSTOM.atmosphereEnabled(true));
        assertFalse(SpaceVisualQuality.CUSTOM.atmosphereEnabled(false));
        assertTrue(SpaceVisualQuality.CUSTOM.cloudShadowsEnabled(true));
        assertFalse(SpaceVisualQuality.CUSTOM.cloudShadowsEnabled(false));
    }
}
