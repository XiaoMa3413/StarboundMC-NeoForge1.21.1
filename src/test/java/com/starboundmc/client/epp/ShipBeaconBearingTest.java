// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipBeaconBearingTest {
    @Test void compassTracksHeadingAndSignedAltitude() {
        var ahead = ShipBeaconBearing.from(Vec3.ZERO, new Vec3(0, 3, 4), 0);
        assertEquals(5, ahead.distance(), 1e-8); assertEquals(3, ahead.height()); assertEquals(0, ahead.turnDegrees());
        assertEquals(90, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(-5, 0, 0), 0).turnDegrees());
        assertEquals(-90, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(5, 0, 0), 0).turnDegrees());
        assertEquals(0, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(-5, 0, 0), 450).turnDegrees());
        assertEquals(-3, ShipBeaconBearing.from(new Vec3(0, 3, 0), Vec3.ZERO, 0).height());
    }
}
