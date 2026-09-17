package com.starboundmc.story;

import com.mojang.authlib.GameProfile;
import com.starboundmc.item.ModItems;
import com.starboundmc.network.ShipAiActionPacket;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class NovaTaskGameTests {
    private record ConnectedPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        public void close() { player.discard(); channel.finishAndReleaseAll(); }
    }
    private static ConnectedPlayer player(GameTestHelper h, String name) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return new ConnectedPlayer(player, channel);
    }
    private static NovaTaskProgress completed() {
        return NovaTaskProgress.DEFAULT.observe(true, true, true, true, true);
    }
    @GameTest(template = "shuttle_test_empty")
    public static void personalClaimsSurvivePlayerSaveAndRejectReplay(GameTestHelper h) {
        try (var a = player(h, "NovaAlice"); var b = player(h, "NovaBob"); var reload = player(h, "NovaReload")) {
            a.player.setData(ModAttachments.NOVA_TASKS, completed());
            b.player.setData(ModAttachments.NOVA_TASKS, completed());
            h.assertTrue(NovaTaskService.grantReward(a.player, NovaTask.SURFACE), "First claim rejected");
            h.assertTrue(!NovaTaskService.grantReward(a.player, NovaTask.SURFACE), "Duplicate claim paid twice");
            h.assertTrue(NovaTaskService.grantReward(b.player, NovaTask.SURFACE), "Other player's claim blocked");
            h.assertTrue(a.player.getInventory().countItem(ModItems.MATTER_MANIPULATOR_MODULE.get()) == 1,
                    "Incorrect first player reward count");
            var saved = a.player.saveWithoutId(new CompoundTag());
            reload.player.load(saved);
            h.assertTrue(reload.player.getData(ModAttachments.NOVA_TASKS).claimed(NovaTask.SURFACE), "Claim ledger lost on load");
            h.assertTrue(reload.player.getInventory().countItem(ModItems.MATTER_MANIPULATOR_MODULE.get()) == 1,
                    "Inventory lost on load");
            h.assertTrue(!NovaTaskService.grantReward(reload.player, NovaTask.SURFACE), "Reload replayed claim");
            // A forged request without the bound terminal menu cannot claim another task.
            ShipStoryService.handleTerminalAction(b.player, 99, 1, ShipAiActionPacket.Action.CLAIM_TASK_REWARD, NovaTask.REPAIR.id());
            h.assertTrue(!b.player.getData(ModAttachments.NOVA_TASKS).claimed(NovaTask.REPAIR), "Unbound terminal request accepted");
            h.assertTrue(b.player.getInventory().countItem(ModItems.MATTER_MANIPULATOR_MODULE.get()) == 1,
                    "Forged request created reward");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void fullInventoryKeepsRewardAndStackSpaceAllowsOneClaim(GameTestHelper h) {
        try (var p = player(h, "NovaFull")) {
            p.player.setData(ModAttachments.NOVA_TASKS, completed());
            for (int slot = 0; slot < 36; slot++) p.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            h.assertTrue(!NovaTaskService.grantReward(p.player, NovaTask.SURFACE), "Full inventory accepted reward");
            h.assertTrue(p.player.getData(ModAttachments.NOVA_TASKS).claimable(NovaTask.SURFACE), "Full inventory consumed claim");
            var modules = new ItemStack(ModItems.MATTER_MANIPULATOR_MODULE.get());
            modules.setCount(modules.getMaxStackSize() - 1);
            int expected = modules.getMaxStackSize();
            p.player.getInventory().setItem(9, modules);
            h.assertTrue(NovaTaskService.grantReward(p.player, NovaTask.SURFACE), "Available stack space rejected");
            h.assertTrue(p.player.getInventory().getItem(9).getCount() == expected, "Stack count incorrect");
            h.assertTrue(!NovaTaskService.grantReward(p.player, NovaTask.REPAIR), "Full module stack overfilled");
            p.player.getInventory().setItem(10, ItemStack.EMPTY);
            h.assertTrue(NovaTaskService.grantReward(p.player, NovaTask.REPAIR), "Retry after clearing slot rejected");
            h.assertTrue(p.player.getInventory().getItem(10).getCount() == 1, "Retry paid incorrect amount");
        }
        h.succeed();
    }
}
