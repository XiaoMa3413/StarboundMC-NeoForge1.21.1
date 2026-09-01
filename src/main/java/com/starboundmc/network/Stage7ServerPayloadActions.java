package com.starboundmc.network;

import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.menu.WarpControlMenu;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipWarpManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Adds the stage 7 warp action while retaining all stage 6 authority checks. */
public final class Stage7ServerPayloadActions extends Stage6ServerPayloadActions {
    @Override
    public void startWarp(ServerPlayer player, String entryId) {
        if (player.containerMenu instanceof WarpControlMenu menu && menu.stillValid(player)) {
            ShipWarpManager.startWarp(player, entryId);
        }
    }

    @Override
    public void shipAiAction(ServerPlayer player, int containerId, long requestId,
                             ShipAiActionPacket.Action action, int argument) {
        if (!player.isSpectator()
                && player.containerMenu instanceof ShipAiTerminalMenu menu
                && menu.containerId == containerId
                && menu.stillValid(player)) {
            ShipStoryService.handleTerminalAction(
                    player, containerId, requestId, action, argument);
        }
    }

    @Override
    public void startRefinement(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.startRefinement(player, pos);
    }

    @Override
    public void stopRefinement(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.stopRefinement(player, pos);
    }

    @Override
    public void claimRefinedVoxels(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.claimRefinedVoxels(player, pos);
    }

    @Override
    public void startPrint(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, int quantity) {
        VoxelMachineActions.startPrint(player, pos, recipeId, quantity);
    }

    @Override
    public void cancelPrintQueue(ServerPlayer player, BlockPos pos, java.util.UUID queueId) {
        VoxelMachineActions.cancelPrintQueue(player, pos, queueId);
    }
}
