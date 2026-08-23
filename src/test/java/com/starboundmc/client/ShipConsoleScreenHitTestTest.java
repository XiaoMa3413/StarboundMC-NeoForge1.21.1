package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipConsoleScreenHitTestTest
{
    @Test
    void disabledActionTooltipUsesOnlyTheButtonRectangle()
    {
        assertTrue(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 100, 50));
        assertTrue(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 179.999, 69.999));

        assertFalse(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 99.999, 60));
        assertFalse(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 180, 60));
        assertFalse(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 120, 70));
        assertFalse(ShipConsoleScreen.isPointInside(100, 50, 80, 20, 400, 200));
    }
}
