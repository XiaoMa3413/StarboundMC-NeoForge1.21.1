package com.starboundmc.network;

import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.menu.WarpControlMenu;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipWarpManager;
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
}
