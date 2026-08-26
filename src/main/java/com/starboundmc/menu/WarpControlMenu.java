package com.starboundmc.menu;

import net.minecraft.world.entity.player.Player;

/** Marker boundary for menus that may submit a server-authoritative warp request. */
public interface WarpControlMenu {
    boolean stillValid(Player player);
}
