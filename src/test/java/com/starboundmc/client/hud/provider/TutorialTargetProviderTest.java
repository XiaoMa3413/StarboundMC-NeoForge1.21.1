package com.starboundmc.client.hud.provider;

import com.starboundmc.client.hud.HudBootController;
import com.starboundmc.client.hud.ar.ArContext;
import com.starboundmc.client.hud.ar.ArTarget;
import com.starboundmc.story.CoreState;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.ShipStructure;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialTargetProviderTest {
    private final TutorialTargetProvider provider = new TutorialTargetProvider();

    @AfterEach
    void resetBoot() {
        HudBootController.INSTANCE.reset();
    }

    @Test
    void safeModeLocksMarkerToTheRealShipTerminal() {
        HudBootController.INSTANCE.applyServerState(CoreState.OFFLINE, true, false);
        var targets = new ArrayList<ArTarget>();
        provider.collect(new ArContext(ShipDimensions.SHIP_LEVEL, Vec3.ZERO), targets::add);

        assertEquals(1, targets.size());
        assertEquals(Vec3.atCenterOf(ShipStructure.SHIP_AI_TERMINAL_POS),
                targets.getFirst().worldPosition());
    }

    @Test
    void markerRequiresShipDimensionAndUncontactedTerminal() {
        HudBootController.INSTANCE.applyServerState(CoreState.OFFLINE, true, false);
        var targets = new ArrayList<ArTarget>();
        provider.collect(new ArContext(Level.OVERWORLD, Vec3.ZERO), targets::add);
        assertTrue(targets.isEmpty());

        HudBootController.INSTANCE.applyServerState(CoreState.OFFLINE, true, true);
        provider.collect(new ArContext(ShipDimensions.SHIP_LEVEL, Vec3.ZERO), targets::add);
        assertTrue(targets.isEmpty());
    }
}
