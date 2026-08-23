package com.starboundmc.world.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapBodyVisualTest
{
    @Test
    void basicProfilePreservesLegacyMarkerValues()
    {
        StarmapBodyVisual visual = StarmapBodyVisual.basic(0xFF58C458, 18);

        assertEquals(StarmapBodyType.GENERIC, visual.getBodyType());
        assertEquals(0xFF58C458, visual.getPrimaryColor());
        assertEquals(18, visual.getMarkerSize());
        assertFalse(visual.hasAtmosphere());
        assertFalse(visual.hasBands());
        assertFalse(visual.hasRings());
    }

    @Test
    void builderCapturesOptionalProceduralFeatures()
    {
        StarmapBodyVisual visual = StarmapBodyVisual.builder(
                        StarmapBodyType.GAS_GIANT, 0xFFE8A860, 22, 42L)
                .secondaryColor(0xFFB86648)
                .atmosphere(0xFFFFD6A0, 0.6F)
                .bands(0.8F)
                .rings(0xFFD8C8A0, 0.4F)
                .texture("starboundmc:textures/gui/starmap/bodies/gas_giant.png")
                .focusTexture("starboundmc:textures/gui/starmap/bodies/gas_giant_focus.png")
                .textureMask("starboundmc:textures/starmap/masks/clouds.png")
                .build();

        assertTrue(visual.hasAtmosphere());
        assertTrue(visual.hasBands());
        assertTrue(visual.hasRings());
        assertEquals(42L, visual.getTextureSeed());
        assertEquals("starboundmc:textures/gui/starmap/bodies/gas_giant.png",
                visual.getTextureId());
        assertEquals("starboundmc:textures/gui/starmap/bodies/gas_giant_focus.png",
                visual.getFocusTextureId());
        assertEquals("starboundmc:textures/starmap/masks/clouds.png", visual.getTextureMaskId());
    }

    @Test
    void builderRejectsInvalidStrengths()
    {
        StarmapBodyVisual.Builder builder = StarmapBodyVisual.builder(
                StarmapBodyType.ROCKY, 0xFFFFFFFF, 10, 1L);

        assertThrows(IllegalArgumentException.class, () -> builder.surfaceDetail(1.1F));
        assertThrows(IllegalArgumentException.class, () -> builder.atmosphere(0xFFFFFFFF, -0.1F));
        assertThrows(IllegalArgumentException.class, () -> builder.texture(" "));
        assertThrows(IllegalArgumentException.class, () -> builder.focusTexture(" "));
    }
}
