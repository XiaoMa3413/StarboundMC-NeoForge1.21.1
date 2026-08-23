package com.starboundmc.network;

import com.starboundmc.menu.UpgradeMenu;
import net.minecraft.server.level.ServerPlayer;

/** Stage 4 business actions attached to the stage 3 authority boundary. */
public final class Stage4ServerPayloadActions implements ServerPayloadActions {
    @Override
    public void upgradeMatterManipulator(ServerPlayer player, int track) {
        if (player.containerMenu instanceof UpgradeMenu menu && menu.stillValid(player)) {
            menu.tryUpgrade(player, track);
        }
    }
}
