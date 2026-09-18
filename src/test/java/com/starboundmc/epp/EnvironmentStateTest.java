// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.world.universe.BuiltInUniverse;
import com.starboundmc.world.universe.UniverseCatalog;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentStateTest {
    @Test void heatAndColdDoNotImplyAirlessness() {
        var catalog = UniverseCatalog.of(BuiltInUniverse.systems());
        for (String body : new String[]{"sys1:molten", "sys2:frozen"}) {
            var state = EnvironmentState.from(catalog.body(body).orElseThrow().surface().orElseThrow().environment());
            assertTrue(state.breathable()); assertFalse(state.pressurized());
            assertEquals(body.equals("sys2:frozen") ? 1 : 3, Math.max(state.coldTier(), state.heatTier()));
        }
        var moon = EnvironmentState.from(catalog.body("sys1:rockymoon").orElseThrow().surface().orElseThrow().environment());
        assertFalse(moon.breathable()); assertEquals(0, moon.coldTier() + moon.heatTier() + moon.radiationTier());
    }
    @Test void cabinDoesNotIncludeTheEntireShipDimension() {
        assertTrue(PlayerEnvironmentService.inStarterCabin(new BlockPos(0, 102, -7)));
        assertFalse(PlayerEnvironmentService.inStarterCabin(new BlockPos(40, 102, -7)));
        assertFalse(PlayerEnvironmentService.inStarterCabin(new BlockPos(0, 130, 0)));
        assertFalse(PlayerEnvironmentService.inStarterCabin(new BlockPos(0, 105, 0)), "Roof is outside the authored cabin");
        assertFalse(PlayerEnvironmentService.inStarterCabin(new BlockPos(4, 102, 0)), "Wing is outside the cabin");
    }
}
