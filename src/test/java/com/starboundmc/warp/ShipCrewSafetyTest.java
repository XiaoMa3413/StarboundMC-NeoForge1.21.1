// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.warp;

import com.starboundmc.world.ShipDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipCrewSafetyTest {
    @Test void onlyTheCabinCountsAsAboardEvenIfRoofIsCloserToCenter() {
        assertTrue(ShipCrewSafety.isAboard(ShipDimensions.SHIP_LEVEL, new BlockPos(0, 102, -7)));
        for (BlockPos outside : new BlockPos[] {new BlockPos(0, 105, 0), new BlockPos(4, 102, 0), new BlockPos(100, 100, 100)})
            assertTrue(ShipCrewSafety.blocksDeparture(ShipDimensions.SHIP_LEVEL, outside, true, false));
    }
    @Test void surfaceCrewDeadPlayersAndSpectatorsDoNotLockShip() {
        var outside = new BlockPos(20, 102, 0);
        assertFalse(ShipCrewSafety.blocksDeparture(Level.OVERWORLD, outside, true, false));
        assertFalse(ShipCrewSafety.blocksDeparture(ShipDimensions.SHIP_LEVEL, outside, false, false));
        assertFalse(ShipCrewSafety.blocksDeparture(ShipDimensions.SHIP_LEVEL, outside, true, true));
        assertFalse(ShipCrewSafety.isAboard(Level.OVERWORLD, new BlockPos(0, 102, 0)));
    }
}
