// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.story;

import com.starboundmc.network.HudBootstrapStatePacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned HUD snapshots; never depends on an open terminal or client gameplay claims. */
public final class HudStateService {
    private HudStateService() { }

    public static HudBootstrapStatePacket snapshotFor(CoreState core, PlayerStoryState personal) {
        return new HudBootstrapStatePacket(core,
                personal.hasFlag(PlayerStoryFlag.INITIAL_WAKE_BROADCAST),
                personal.hasFlag(PlayerStoryFlag.TERMINAL_CONTACTED),
                personal.hasFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED));
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null)
            return;
        var shared = ShipStateData.get(player.getServer()).getStoryProgress();
        ModNetwork.sendToPlayer(player, snapshotFor(shared.core(),
                player.getData(ModAttachments.PLAYER_STORY)));
    }

    public static void syncAll(MinecraftServer server) {
        if (server != null)
            server.getPlayerList().getPlayers().forEach(HudStateService::syncPlayer);
    }

    public static void acknowledgeCoreLink(ServerPlayer player) {
        if (player == null || player.getServer() == null)
            return;
        var shared = ShipStateData.get(player.getServer()).getStoryProgress();
        if (!shared.isWritable())
            return;
        var before = player.getData(ModAttachments.PLAYER_STORY);
        var after = acknowledgeCoreLink(shared.core(), before);
        if (after != before)
            player.setData(ModAttachments.PLAYER_STORY, after);
    }

    static PlayerStoryState acknowledgeCoreLink(CoreState core, PlayerStoryState personal) {
        // The client can suppress its own repeated animation only after the real core is online.
        // This grants no identity, tutorial, mission, equipment or shared progression.
        return core == CoreState.ONLINE
                ? personal.withFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED) : personal;
    }
}
