// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipBeaconBearingTest {
    @Test void renderedCompassPointsRightForTargetsOnCameraRightAtEveryHeading() {
        for (int yaw = -360; yaw <= 360; yaw += 45) {
            double angle = Math.toRadians(yaw);
            Vec3 forward = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
            Vec3 right = new Vec3(-Math.cos(angle), 0, -Math.sin(angle));
            var ahead = ShipBeaconBearing.from(Vec3.ZERO, forward, yaw);
            var behind = ShipBeaconBearing.from(Vec3.ZERO, forward.scale(-1), yaw);
            var toRight = ShipBeaconBearing.from(Vec3.ZERO, right, yaw);
            var toLeft = ShipBeaconBearing.from(Vec3.ZERO, right.scale(-1), yaw);
            assertEquals(0, ahead.arrowX(), 1e-8); assertEquals(-1, ahead.arrowY(), 1e-8);
            assertEquals(0, behind.arrowX(), 1e-8); assertEquals(1, behind.arrowY(), 1e-8);
            assertEquals(1, toRight.arrowX(), 1e-8); assertEquals(0, toRight.arrowY(), 1e-8);
            assertEquals(-1, toLeft.arrowX(), 1e-8); assertEquals(0, toLeft.arrowY(), 1e-8);
        }
    }
    @Test void compassTracksHeadingAndSignedAltitude() {
        var ahead = ShipBeaconBearing.from(Vec3.ZERO, new Vec3(0, 3, 4), 0);
        assertEquals(5, ahead.distance(), 1e-8); assertEquals(3, ahead.height()); assertEquals(0, ahead.turnDegrees());
        assertEquals(90, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(-5, 0, 0), 0).turnDegrees());
        assertEquals(-90, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(5, 0, 0), 0).turnDegrees());
        assertEquals(0, ShipBeaconBearing.from(Vec3.ZERO, new Vec3(-5, 0, 0), 450).turnDegrees());
        assertEquals(-3, ShipBeaconBearing.from(new Vec3(0, 3, 0), Vec3.ZERO, 0).height());
    }
}
