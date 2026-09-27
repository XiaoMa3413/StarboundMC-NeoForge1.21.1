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
    private static final String RESOURCE = "/assets/starboundmc/textures/planet/lush_clouds.png";

    @Test
    void generatedCloudMapHasResolutionCoverageAndWrappedLongitude() throws IOException
    {
        BufferedImage image;
        try (InputStream stream = LushCloudTextureTest.class.getResourceAsStream(RESOURCE))
        {
            assertNotNull(stream, "missing cloud map " + RESOURCE);
            image = ImageIO.read(stream);
        }

        assertNotNull(image, "could not decode cloud map");
        assertEquals(2048, image.getWidth());
        assertEquals(1024, image.getHeight());

        long visiblePixels = 0;
        long nonzeroPixels = 0;
        double seamDifference = 0.0;
        int northMin = 255;
        int northMax = 0;
        int southMin = 255;
        int southMax = 0;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int a = alpha(image.getRGB(x, y));
                if (a > 25)
                    visiblePixels++;
                if (a > 0)
                    nonzeroPixels++;
                if (y == 0)
                {
                    northMin = Math.min(northMin, a);
                    northMax = Math.max(northMax, a);
                }
                if (y == image.getHeight() - 1)
                {
                    southMin = Math.min(southMin, a);
                    southMax = Math.max(southMax, a);
                }
            }

            seamDifference += Math.abs(alpha(image.getRGB(0, y))
                    - alpha(image.getRGB(image.getWidth() - 1, y)));
        }

        double coverage = (double) visiblePixels / (image.getWidth() * (long) image.getHeight());
        double nonzeroCoverage = (double) nonzeroPixels / (image.getWidth() * (long) image.getHeight());
        assertTrue(coverage >= 0.30 && coverage <= 0.60,
                "cloud coverage should leave most of the planetary surface visible");
        assertTrue(nonzeroCoverage > 0.30 && nonzeroCoverage < 0.65,
                "texture alpha must contain both cloud and clear regions");
        assertTrue(seamDifference / image.getHeight() < 8.0,
                "longitude edges should meet without a visible seam");
        assertTrue(northMin == northMax && southMin == southMax,
                "pole texels should not contain longitude-dependent high-frequency detail");
    }

    private static int alpha(int argb)
    {
        return (argb >>> 24) & 0xFF;
    }
}
