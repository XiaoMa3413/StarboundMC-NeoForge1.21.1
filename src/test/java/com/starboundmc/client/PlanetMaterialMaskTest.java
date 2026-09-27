package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetMaterialMaskTest
{
    @Test
    void packedMaterialMasksShareDiffuseDimensionsAndChannelSemantics() throws IOException
    {
        String base = "/assets/starboundmc/textures/planet/";
        assertEquals(imageDimensions(base + "molten.png"),
                imageDimensions(base + "molten_material.png"));
        assertEquals(imageDimensions(base + "lush.png"),
                imageDimensions(base + "lush_material.png"));

        var molten = readImage(base + "molten_material.png");
        var lush = readImage(base + "lush_material.png");
        long moltenPixels = (long) molten.getWidth() * molten.getHeight();
        long emissivePixels = 0;

        for (int y = 0; y < molten.getHeight(); y++)
        {
            for (int x = 0; x < molten.getWidth(); x++)
            {
                int rgba = molten.getRGB(x, y);
                assertEquals(255, (rgba >>> 16) & 0xFF, "Molten R must preserve its smooth response");
                assertEquals(0, rgba & 0xFF, "Molten B is reserved");
                assertEquals(255, (rgba >>> 24) & 0xFF, "Molten mask must stay opaque");
                if (((rgba >>> 8) & 0xFF) > 0)
                    emissivePixels++;
            }
        }

        assertTrue(emissivePixels > 0 && emissivePixels < moltenPixels / 2,
                "emissive mask must select lava regions without lighting the whole sphere");

        for (int y = 0; y < lush.getHeight(); y++)
        {
            for (int x = 0; x < lush.getWidth(); x++)
            {
                int rgba = lush.getRGB(x, y);
                assertEquals(0, (rgba >>> 8) & 0xFF, "Lush G must not emit");
                assertEquals(0, rgba & 0xFF, "Lush B is reserved");
            }
        }
    }

    private static java.awt.image.BufferedImage readImage(String resource) throws IOException
    {
        try (InputStream stream = PlanetMaterialMaskTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "missing material texture " + resource);
            var image = ImageIO.read(stream);
            assertNotNull(image, "could not decode material texture " + resource);
            return image;
        }
    }

    private static Dimension imageDimensions(String resource) throws IOException
    {
        InputStream stream = PlanetMaterialMaskTest.class.getResourceAsStream(resource);
        assertNotNull(stream, "missing texture " + resource);
        try (stream; ImageInputStream imageInput = ImageIO.createImageInputStream(stream))
        {
            assertNotNull(imageInput, "could not read texture " + resource);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            assertTrue(readers.hasNext(), "no image decoder for " + resource);
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(imageInput, true, true);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            }
            finally
            {
                reader.dispose();
            }
        }
    }
}
