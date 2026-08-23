package com.starboundmc.client;

import com.starboundmc.world.starmap.StarmapBodyType;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapBodyShadingTest
{
    private static final StarmapBodyVisual VISUAL = StarmapBodyVisual.builder(
                    StarmapBodyType.ROCKY, 0xFFC8A060, 18, 0x5A17B4E1L)
            .secondaryColor(0xFF765936)
            .surfaceDetail(0.7F)
            .build();

    @Test
    void sideFacingTheStarIsBrighterThanTheFarSide()
    {
        int lit = StarmapBodyShading.shade(VISUAL, -0.55D, 0.0D, -1.0D, 0.0D);
        int shadow = StarmapBodyShading.shade(VISUAL, 0.55D, 0.0D, -1.0D, 0.0D);

        assertTrue(luminance(lit) > luminance(shadow));
    }

    @Test
    void proceduralDetailIsStableForTheSameSeed()
    {
        int first = StarmapBodyShading.shade(VISUAL, 0.2D, -0.3D, -1.0D, 0.0D);
        int repeated = StarmapBodyShading.shade(VISUAL, 0.2D, -0.3D, -1.0D, 0.0D);

        assertEquals(first, repeated);
        assertEquals(0xFF000000, first & 0xFF000000);
    }

    @Test
    void differentSeedsCanProduceDifferentSurfaceColour()
    {
        StarmapBodyVisual other = StarmapBodyVisual.builder(
                        StarmapBodyType.ROCKY, 0xFFC8A060, 18, 0x12345678L)
                .secondaryColor(0xFF765936)
                .surfaceDetail(0.7F)
                .build();

        assertNotEquals(
                StarmapBodyShading.shade(VISUAL, 0.2D, -0.3D, -1.0D, 0.0D),
                StarmapBodyShading.shade(other, 0.2D, -0.3D, -1.0D, 0.0D));
    }

    @Test
    void textureShadingPreservesAlphaAndLightsTheStarFacingSide()
    {
        int lit = StarmapBodyShading.shadeTexture(
                0x804080C0, -0.55D, 0.0D, -1.0D, 0.0D);
        int shadow = StarmapBodyShading.shadeTexture(
                0x804080C0, 0.55D, 0.0D, -1.0D, 0.0D);

        assertEquals(0x80, lit >>> 24);
        assertEquals(0x80, shadow >>> 24);
        assertTrue(luminance(lit) > luminance(shadow));
    }

    private static double luminance(int color)
    {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        return red * 0.2126D + green * 0.7152D + blue * 0.0722D;
    }
}
