// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import net.minecraft.world.phys.Vec3;

public record ShipBeaconBearing(double distance, double height, double turnDegrees) {
    public static ShipBeaconBearing from(Vec3 player, Vec3 beacon, float yaw) {
        Vec3 delta = beacon.subtract(player);
        double angle = Math.toDegrees(Math.atan2(-delta.x, delta.z)) - yaw;
        angle = ((angle + 180) % 360 + 360) % 360 - 180;
        return new ShipBeaconBearing(delta.length(), delta.y, angle);
    }
}
