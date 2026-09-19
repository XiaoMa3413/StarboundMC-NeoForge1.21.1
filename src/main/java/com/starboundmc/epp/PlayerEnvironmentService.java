// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.universe.ServerUniverseCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;

public final class PlayerEnvironmentService {
    private static final TreeMap<String, BiFunction<ServerLevel, BlockPos, Optional<EnvironmentState>>> ZONES = new TreeMap<>();
    private PlayerEnvironmentService() { }
    /** Register at common setup; providers query live world state and must not retain a world/player. */
    public static void registerZone(String id, BiFunction<ServerLevel, BlockPos, Optional<EnvironmentState>> zone) {
        if (ZONES.putIfAbsent(id, zone) != null) throw new IllegalArgumentException("Duplicate environment zone " + id);
    }
    public static EnvironmentState at(ServerPlayer player) {
        return at(player.serverLevel(), player.blockPosition());
    }
    public static EnvironmentState at(ServerLevel level, BlockPos pos) {
        for (var provider : ZONES.values()) {
            var zone = provider.apply(level, pos);
            if (zone.isPresent()) return zone.get();
        }
        if (level.dimension().equals(ShipDimensions.SHIP_LEVEL))
            return inStarterCabin(pos) ? EnvironmentState.SHIP_INTERIOR : EnvironmentState.SPACE;
        return ServerUniverseCatalog.current().bodyByDimension(level.dimension().location())
                .flatMap(body -> body.surface()).map(surface -> EnvironmentState.from(surface.environment()))
                .orElse(EnvironmentState.SAFE);
    }
    /** Authored starter cabin, deliberately NOT a general sealed-room detector. */
    public static boolean inStarterCabin(BlockPos pos) {
        if (pos.getX() < -2 || pos.getX() > 2 || pos.getY() < 101 || pos.getY() > 104) return false;
        return pos.getZ() >= -8 && pos.getZ() <= 9
                || pos.getZ() >= 10 && pos.getZ() <= 12 && pos.getY() <= 103;
    }
}
