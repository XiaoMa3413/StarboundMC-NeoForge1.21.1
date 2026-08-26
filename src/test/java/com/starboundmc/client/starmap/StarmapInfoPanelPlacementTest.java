package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapInfoPanelPlacementTest {
    @Test
    void defaultsToTheRightWhenBothSidesFit() {
        var placement = StarmapInfoPanelPlacement.place(0, 0, 1000, 600,
                220, 122, 500, 300, 20, null);

        assertEquals(StarmapInfoPanelPlacement.Side.RIGHT, placement.side());
        assertEquals(534.0F, placement.x());
        assertFalse(placement.contains(500, 300));
    }

    @Test
    void flipsAwayFromTheRightEdge() {
        var placement = StarmapInfoPanelPlacement.place(0, 0, 1000, 600,
                220, 122, 930, 300, 20, StarmapInfoPanelPlacement.Side.RIGHT);

        assertEquals(StarmapInfoPanelPlacement.Side.LEFT, placement.side());
        assertTrue(placement.x() + placement.width() < 930);
    }

    @Test
    void movesAboveATargetNearTheBottomEdge() {
        var placement = StarmapInfoPanelPlacement.place(0, 0, 1000, 600,
                220, 122, 500, 560, 20, null);

        assertEquals(StarmapInfoPanelPlacement.Side.ABOVE, placement.side());
        assertTrue(placement.y() + placement.height() < 560);
    }

    @Test
    void keepsThePreviousSideWhileItStillFits() {
        var placement = StarmapInfoPanelPlacement.place(0, 0, 1000, 600,
                220, 122, 500, 300, 20, StarmapInfoPanelPlacement.Side.LEFT);

        assertEquals(StarmapInfoPanelPlacement.Side.LEFT, placement.side());
    }
}
