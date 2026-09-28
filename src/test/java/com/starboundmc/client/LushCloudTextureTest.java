package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LushCloudTextureTest
{
    private static final String SURFACE_RESOURCE = "/assets/starboundmc/textures/planet/lush.png";
    private static final String RESOURCE = "/assets/starboundmc/textures/planet/lush_clouds.png";

    @Test
    void earthDayMapIsPackagedAsAnOpaqueSurfaceTexture() throws IOException
    {
        BufferedImage image;
        try (InputStream stream = LushCloudTextureTest.class.getResourceAsStream(SURFACE_RESOURCE))
        {
            assertNotNull(stream, "missing Lush surface map " + SURFACE_RESOURCE);
            image = ImageIO.read(stream);
        }

        assertNotNull(image, "could not decode Lush surface map");
        assertEquals(4096, image.getWidth());
        assertEquals(2048, image.getHeight());
        assertEquals(255, alpha(image.getRGB(0, 0)));
        assertEquals(255, alpha(image.getRGB(image.getWidth() / 2, image.getHeight() / 2)));
    }

    @Test
    void solarSystemScopeCloudMapHasResolutionCoverageAndWrappedLongitude() throws IOException
    {
        BufferedImage image;
        try (InputStream stream = LushCloudTextureTest.class.getResourceAsStream(RESOURCE))
        {
            assertNotNull(stream, "missing cloud map " + RESOURCE);
            image = ImageIO.read(stream);
        }

        assertNotNull(image, "could not decode cloud map");
        assertEquals(4096, image.getWidth());
        assertEquals(2048, image.getHeight());

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        long visiblePixels = 0;
        long nonzeroPixels = 0;
        double seamDifference = 0.0;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int a = alpha(pixels[y * image.getWidth() + x]);
                if (a > 25)
                    visiblePixels++;
                if (a > 0)
                    nonzeroPixels++;
            }

            seamDifference += Math.abs(alpha(pixels[y * image.getWidth()])
                    - alpha(pixels[y * image.getWidth() + image.getWidth() - 1]));
        }

        double coverage = (double) visiblePixels / (image.getWidth() * (long) image.getHeight());
        double nonzeroCoverage = (double) nonzeroPixels / (image.getWidth() * (long) image.getHeight());
        assertTrue(coverage >= 0.30 && coverage <= 0.60,
                "cloud coverage should leave most of the planetary surface visible");
        assertTrue(nonzeroCoverage > 0.40 && nonzeroCoverage < 0.65,
                "texture alpha must contain both cloud and clear regions");
        assertTrue(seamDifference / image.getHeight() < 8.0,
                "longitude edges should meet without a visible seam");
    }

    private static int alpha(int argb)
    {
        return (argb >>> 24) & 0xFF;
    }
}
