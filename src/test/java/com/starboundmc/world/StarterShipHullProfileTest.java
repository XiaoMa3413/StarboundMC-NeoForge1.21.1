package com.starboundmc.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterShipHullProfileTest
{
    @Test
    void reportsApprovedStarterShipBounds()
    {
        assertEquals(21, StarterShipHullProfile.length());
        assertEquals(13, StarterShipHullProfile.maximumWidth());
        assertEquals(8, StarterShipHullProfile.maximumHeight());
    }

    @Test
    void keepsSharedCabinCompactAndTapersTheBow()
    {
        assertEquals(4, StarterShipHullProfile.sliceAt(0).halfWidth());
        assertEquals(3, StarterShipHullProfile.sliceAt(7).halfWidth());
        assertEquals(2, StarterShipHullProfile.sliceAt(9).halfWidth());
        assertEquals(0, StarterShipHullProfile.sliceAt(11).halfWidth());

        assertTrue(StarterShipHullProfile.isCoreCabinColumn(-2, 0));
        assertTrue(StarterShipHullProfile.isCoreCabinColumn(2, -6));
        assertFalse(StarterShipHullProfile.isCoreCabinColumn(3, 0));
        assertFalse(StarterShipHullProfile.isCoreCabinColumn(0, 4));
    }

    @Test
    void exposesOnlyTheOuterMainHullVolumeAsShell()
    {
        assertTrue(StarterShipHullProfile.containsMainVolume(0, 102, 0));
        assertFalse(StarterShipHullProfile.isMainShell(0, 102, 0));
        assertTrue(StarterShipHullProfile.isMainShell(4, 102, 0));
        assertTrue(StarterShipHullProfile.isMainShell(0, 100, 0));
        assertFalse(StarterShipHullProfile.containsMainVolume(5, 102, 0));
    }

    @Test
    void separatesEnginePodsBeforeTheirForwardMount()
    {
        assertTrue(StarterShipHullProfile.containsEnginePod(5, 103, -6));
        assertTrue(StarterShipHullProfile.containsEnginePod(-6, 103, -6));
        assertFalse(StarterShipHullProfile.containsMainVolume(4, 103, -6));
        assertFalse(StarterShipHullProfile.containsEnginePod(4, 103, -6));

        assertTrue(StarterShipHullProfile.containsMainVolume(4, 103, -4));
        assertTrue(StarterShipHullProfile.containsEnginePod(5, 103, -4));
    }

    @Test
    void addsOnlyTheShortCentralKeelBelowTheDeck()
    {
        assertTrue(StarterShipHullProfile.isKeel(0, 99, 0));
        assertTrue(StarterShipHullProfile.isKeel(0, 99, -4));
        assertFalse(StarterShipHullProfile.isKeel(1, 99, 0));
        assertFalse(StarterShipHullProfile.isKeel(0, 99, 4));
    }
}
