package com.starboundmc.network;

import com.starboundmc.menu.WarpControlMenu;
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
}
