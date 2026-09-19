// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.warp;

import com.starboundmc.epp.PlayerEnvironmentService;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** MVP aboard test uses the authored cabin, never distance or the entire dimension. */
public final class ShipCrewSafety {
    private ShipCrewSafety() { }
    public static boolean isAboard(ServerPlayer player) {
        return isAboard(player.level().dimension(), player.blockPosition());
    }
    public static boolean isAboard(ResourceKey<Level> dimension, BlockPos position) {
        return dimension.equals(ShipDimensions.SHIP_LEVEL) && PlayerEnvironmentService.inStarterCabin(position);
    }
    public static boolean blocksDeparture(ServerPlayer player) {
        return blocksDeparture(player.level().dimension(), player.blockPosition(), player.isAlive(), player.isSpectator());
    }
    public static boolean blocksDeparture(ResourceKey<Level> dimension, BlockPos position, boolean alive, boolean spectator) {
        return alive && !spectator && dimension.equals(ShipDimensions.SHIP_LEVEL) && !isAboard(dimension, position);
    }
    public static boolean hasOutsideCrew(Iterable<ServerPlayer> players) {
        for (var player : players) if (blocksDeparture(player)) return true;
        return false;
    }
}
