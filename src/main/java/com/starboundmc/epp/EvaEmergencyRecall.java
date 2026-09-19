// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.story.ModAttachments;
import com.starboundmc.warp.ShipCrewSafety;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** A fail-safe for accidental unpowered EVA, available before the ship core is online. */
public final class EvaEmergencyRecall {
    private EvaEmergencyRecall() { }

    public static boolean canRequest(ServerPlayer player) {
        return ShipCrewSafety.blocksDeparture(player) && EvaMovement.serverMode(player) == EvaState.DRIFT;
    }

    public static void request(ServerPlayer player) {
        if (!canRequest(player)) return;
        if (returnToCabin(player)) {
            EvaMovement.update(player);
            player.displayClientMessage(Component.translatable("message.starboundmc.eva.recalled"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.starboundmc.eva.recall_blocked"), true);
        }
    }

    /** Same-level rescue only. Never overwrite blocks or fall back to another dimension. */
    static boolean returnToCabin(ServerPlayer player) {
        var level = player.serverLevel();
        BlockPos preferred = ShipDimensions.shipTeleporterDestination(level);
        var candidates = new java.util.ArrayList<BlockPos>();
        candidates.add(preferred);
        BlockPos.betweenClosed(-2, 101, -8, 2, 104, 12).forEach(pos -> {
            if (PlayerEnvironmentService.inStarterCabin(pos) && !pos.equals(preferred)) candidates.add(pos.immutable());
        });
        for (var pos : candidates) {
            if (tryDestination(player, pos)) return true;
        }
        return false;
    }

    static boolean tryDestination(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        if (!PlayerEnvironmentService.inStarterCabin(pos) || !PlayerEnvironmentService.at(level, pos).breathable()) return false;
        var destination = Vec3.atBottomCenterOf(pos);
        var box = player.getDimensions(Pose.STANDING).makeBoundingBox(destination);
        if (!level.noCollision(player, box)
                || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                || level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return false;
        player.teleportTo(level, destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.getData(ModAttachments.EVA).clearInput();
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        return true;
    }
}
