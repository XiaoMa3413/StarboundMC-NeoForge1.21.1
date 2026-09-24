package com.starboundmc.world;

import com.starboundmc.block.TransporterBlock;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TransporterEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Effects are emitted only after the authoritative travel operation succeeds. */
public final class TransporterEffects {
    private TransporterEffects() {}
    public record Origin(ServerLevel level, Vec3 position, boolean device) {}

    public static Origin origin(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos pos = player.containerMenu instanceof TeleporterMenu menu && menu.stillValid(player)
                ? menu.pos : player.blockPosition();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof TransporterBlock) {
            BlockPos base = TransporterBlock.basePos(pos, state);
            var lower = level.getBlockState(base);
            if (lower.getBlock() instanceof TransporterBlock && lower.getValue(TransporterBlock.PART) == 0)
                return new Origin(level, TransporterBlock.landingPosition(base, lower), true);
        }
        return new Origin(level, player.position(), false);
    }

    public static void afterTravel(ServerPlayer player, Origin source) {
        broadcast(source.level, source.position, source.device, false);
        var level = player.serverLevel();
        var state = level.getBlockState(player.blockPosition());
        boolean device = state.getBlock() instanceof TransporterBlock && state.getValue(TransporterBlock.PART) == 0;
        broadcast(level, player.position(), device, true);
    }

    private static void broadcast(ServerLevel level, Vec3 pos, boolean device, boolean arrival) {
        var packet = new TransporterEffectPacket(level.dimension().location(), pos, device, arrival);
        // Explicit nearby recipients include a traveller whose destination chunk is not tracked yet.
        for (var observer : level.players())
            if (observer.distanceToSqr(pos) <= 64 * 64) ModNetwork.sendToPlayer(observer, packet);
    }
}
