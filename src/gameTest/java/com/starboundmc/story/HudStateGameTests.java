package com.starboundmc.story;

import com.mojang.authlib.GameProfile;
import com.starboundmc.network.HudBootstrapStatePacket;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class HudStateGameTests {
    private record ConnectedPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        public void close() { player.discard(); channel.finishAndReleaseAll(); }
    }

    private static ConnectedPlayer player(GameTestHelper helper, String name) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return new ConnectedPlayer(player, channel);
    }

    @GameTest(template = "shuttle_test_empty")
    public static void personalHudReceiptSurvivesSaveAndDeathWithoutAffectingCrew(GameTestHelper helper) {
        try (var alice = player(helper, "HudAlice"); var bob = player(helper, "HudBob");
             var reload = player(helper, "HudReload"); var respawn = player(helper, "HudRespawn")) {
            alice.player.setData(ModAttachments.PLAYER_STORY,
                    HudStateService.acknowledgeCoreLink(CoreState.ONLINE, PlayerStoryState.DEFAULT));
            reload.player.load(alice.player.saveWithoutId(new CompoundTag()));
            helper.assertTrue(reload.player.getData(ModAttachments.PLAYER_STORY)
                    .hasFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED), "Save/load lost the HUD receipt");
            respawn.player.restoreFrom(alice.player, false);
            helper.assertTrue(respawn.player.getData(ModAttachments.PLAYER_STORY)
                    .hasFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED), "Death lost the personal HUD receipt");
            helper.assertTrue(!bob.player.getData(ModAttachments.PLAYER_STORY)
                    .hasFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED), "Another crew member inherited the receipt");
            helper.assertTrue(!reload.player.getData(ModAttachments.PLAYER_STORY).identityConfirmed(),
                    "Cosmetic receipt changed identity progression");
        }
        helper.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void hudSnapshotsReachPlayersWithoutAnOpenTerminal(GameTestHelper helper) {
        try (var alice = player(helper, "HudLinked"); var bob = player(helper, "HudNew")) {
            alice.player.setData(ModAttachments.PLAYER_STORY,
                    PlayerStoryState.DEFAULT.withFlag(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED));
            HudStateService.syncPlayer(alice.player);
            HudStateService.syncPlayer(bob.player);
            var first = hudPacket(alice.channel);
            var second = hudPacket(bob.channel);
            helper.assertTrue(first != null && second != null, "HUD snapshot requires an open terminal");
            helper.assertTrue(first.core() == second.core(), "Crew received different shared core state");
            helper.assertTrue(first.coreLinkPresented() && !second.coreLinkPresented(),
                    "Personal presentation state leaked across players");
        }
        helper.succeed();
    }

    private static HudBootstrapStatePacket hudPacket(EmbeddedChannel channel) {
        channel.runPendingTasks();
        Object packet;
        while ((packet = channel.readOutbound()) != null) {
            if (packet instanceof ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof HudBootstrapStatePacket hud)
                return hud;
        }
        return null;
    }
}
