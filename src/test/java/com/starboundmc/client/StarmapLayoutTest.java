package com.starboundmc.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapLayoutTest
{
    @ParameterizedTest
    @CsvSource({
            "320,180,true",
            "480,270,true",
            "640,360,false",
            "960,540,false"
    })
    void everyResponsiveRegionFitsInsideThePanel(int width, int height, boolean compact)
    {
        StarmapLayout layout = StarmapLayout.calculate(width, height);

        assertTrue(layout.panelWidth() <= width);
        assertTrue(layout.panelHeight() <= height);
        assertTrue(layout.canvas().fitsInside(layout.panelWidth(), layout.panelHeight()));
        assertTrue(layout.detail().fitsInside(layout.panelWidth(), layout.panelHeight()));
        assertTrue(layout.actionButton().fitsInside(layout.panelWidth(), layout.panelHeight()));
        assertTrue(layout.backButton().fitsInside(layout.panelWidth(), layout.panelHeight()));
        assertTrue(layout.closeButton().fitsInside(layout.panelWidth(), layout.panelHeight()));
        assertTrue(layout.viewport().scale() > 0.0F);
        assertEquals(layout.canvas().x(), layout.viewport().x());
        assertEquals(layout.canvas().y(), layout.viewport().y());
        assertEquals(layout.canvas().width(), layout.viewport().width());
        assertEquals(layout.canvas().height(), layout.viewport().height());
        assertEquals(expectedTextureProjection(layout.canvas().x(), layout.canvas().width(),
                        StarmapLayout.BASE_CANVAS_WIDTH, 184),
                layout.viewport().projectX(184));
        assertEquals(expectedTextureProjection(layout.canvas().y(), layout.canvas().height(),
                        StarmapLayout.BASE_CANVAS_HEIGHT, 51),
                layout.viewport().projectY(51));
        assertTrue(layout.actionButton().x() >= layout.detail().x());
        assertTrue(layout.actionButton().right() <= layout.detail().right());
        assertTrue(layout.actionButton().y() >= layout.detail().y());
        assertTrue(layout.actionButton().bottom() <= layout.detail().bottom());
        float aspect = layout.canvas().width() / (float) layout.canvas().height();
        assertTrue(Math.abs(aspect - StarmapLayout.BASE_CANVAS_WIDTH
                / (float) StarmapLayout.BASE_CANVAS_HEIGHT) < 0.02F);
        if (compact)
        {
            assertTrue(layout.compact());
            assertTrue(layout.detail().width() <= 240);
            assertTrue(layout.detail().width() <= Math.max(156,
                    Math.round(layout.panelWidth() * 0.42F)));
            assertFalse(layout.detailVisible(false));
            assertTrue(layout.detailVisible(true));
        }
        else
        {
            assertFalse(layout.compact());
            assertTrue(layout.detailVisible(false));
        }
    }

    private static int expectedTextureProjection(int origin, int destinationSize,
                                                  int baseSize, int baseCoordinate)
    {
        return origin + StarmapGeometry.projectPixelCenter(
                baseCoordinate, destinationSize, baseSize);
    }
}
