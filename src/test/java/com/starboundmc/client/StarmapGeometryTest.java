package com.starboundmc.client;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StarmapGeometryTest
{
    @Test
    void projectsBasePixelCentersToDestinationPixelCenters()
    {
        assertEquals(0, StarmapGeometry.projectPixelCenter(0, 250, 250));
        assertEquals(125, StarmapGeometry.projectPixelCenter(125, 250, 250));
        assertEquals(249, StarmapGeometry.projectPixelCenter(249, 250, 250));
        assertEquals(62, StarmapGeometry.projectPixelCenter(125, 125, 250));
        // Exact half-pixel ties consistently choose the higher destination pixel.
        assertEquals(251, StarmapGeometry.projectPixelCenter(125, 500, 250));
    }

    @Test
    void bodyPositionsMatchCachedAndInteractiveLayers()
    {
        assertPosition("sys1:barren", 89, 146);
        assertPosition("sys1:lush", 184, 51);
        assertPosition("sys1:molten", 184, 73);
        assertPosition("sys1:gasgiant", 16, 71);
        assertPosition("sys1:rockymoon", 33, 80);
        assertPosition("sys2:frozen", 125, 26);
    }

    @Test
    void overviewMoonsAreSmallerAndOrbitTheirParentPositions()
    {
        CelestialBodyDefinition lush = UniverseTestSupport.body("sys1:lush");
        CelestialBodyDefinition molten = UniverseTestSupport.body("sys1:molten");
        CelestialBodyDefinition gasGiant = UniverseTestSupport.body("sys1:gasgiant");
        CelestialBodyDefinition rockyMoon = UniverseTestSupport.body("sys1:rockymoon");

        assertEquals(18, StarmapGeometry.overviewDiameter(lush));
        assertEquals(8, StarmapGeometry.overviewDiameter(molten));
        assertEquals(7, StarmapGeometry.overviewDiameter(rockyMoon));
        assertArrayEquals(StarmapGeometry.bodyPosition(lush),
                StarmapGeometry.moonOrbitCenter(molten));
        assertArrayEquals(StarmapGeometry.bodyPosition(gasGiant),
                StarmapGeometry.moonOrbitCenter(rockyMoon));
    }

    @Test
    void adaptiveTexturesKeepMinimumQualityAndRespectTheSizeCap()
    {
        assertEquals(3, StarMapCanvas.visualVersion());
        assertArrayEquals(new int[] { 500, 440 }, StarMapCanvas.textureSizeFor(125, 110));
        assertArrayEquals(new int[] { 1000, 880 }, StarMapCanvas.textureSizeFor(500, 440));
        assertArrayEquals(new int[] { 2048, 1024 }, StarMapCanvas.textureSizeFor(2000, 1000));
    }

    private static void assertPosition(String entryId, int x, int y)
    {
        CelestialBodyDefinition entry = UniverseTestSupport.body(entryId);
        assertArrayEquals(new int[] { x, y }, StarmapGeometry.bodyPosition(entry));
    }
}
