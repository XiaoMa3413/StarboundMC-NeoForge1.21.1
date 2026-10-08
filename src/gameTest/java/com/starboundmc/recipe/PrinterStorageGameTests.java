package com.starboundmc.recipe;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.economy.VoxelWalletState;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import com.starboundmc.story.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class PrinterStorageGameTests {
    private record ConnectedPlayer(net.minecraft.server.level.ServerPlayer player,
                                   io.netty.channel.embedded.EmbeddedChannel channel) implements AutoCloseable {
        public void close() { player.discard(); channel.finishAndReleaseAll(); }
    }
    private static ConnectedPlayer player(GameTestHelper h, String name) {
        var p = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name),
                net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), connection,
                p, net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(), false));
        return new ConnectedPlayer(p, channel);
    }
    @GameTest(template = "shuttle_test_empty")
    public static void reservationsSurviveReloadAndRefundOnlyTheirOwner(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var state = ModBlocks.VOXEL_PRINTING_STATION.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        var station = (VoxelPrintingStationBlockEntity) level.getBlockEntity(pos);
        var recipe = level.getRecipeManager().getAllRecipesFor(VoxelPrintingRecipe.TYPE).stream()
                .filter(r -> r.id().getPath().equals("print_sublight_ignition_core")).findFirst().orElseThrow();
        try (var owner = player(h, "PrintOwner"); var other = player(h, "PrintOther")) {
            owner.player.setData(ModAttachments.VOXEL_WALLET, new VoxelWalletState(200));
            owner.player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 9));
            h.assertTrue(station.tryEnqueuePrint(level, owner.player, recipe, 3)
                    == VoxelPrintingStationBlockEntity.PrintEnqueueResult.QUEUED, "Print did not reserve");
            h.assertTrue(owner.player.getInventory().countItem(Items.DIAMOND) == 0
                    && owner.player.getData(ModAttachments.VOXEL_WALLET).balance() == 50, "Reservation charged incorrectly");
            var saved = station.saveWithFullMetadata(level.registryAccess());
            h.assertTrue(saved.contains("storage_version") && !saved.contains("items"), "Retired material storage was written");
            var queued = saved.getList("print_queue", Tag.TAG_COMPOUND).getCompound(0).getUUID("id");
            var restored = new VoxelPrintingStationBlockEntity(pos, state);
            restored.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(restored.outstandingCrafts() == 3, "Reload lost reserved crafts");
            h.assertTrue(restored.cancelQueuedPrint(other.player, queued)
                    == VoxelPrintingStationBlockEntity.QueueCancelResult.NOT_OWNER, "Other player canceled reservation");
            h.assertTrue(restored.cancelQueuedPrint(owner.player, queued)
                    == VoxelPrintingStationBlockEntity.QueueCancelResult.CANCELLED, "Owner could not cancel");
            h.assertTrue(restored.outstandingCrafts() == 1 && owner.player.getInventory().countItem(Items.DIAMOND) == 6
                    && owner.player.getData(ModAttachments.VOXEL_WALLET).balance() == 150, "Reloaded refund lost or duplicated resources");
            h.assertTrue(restored.cancelQueuedPrint(owner.player, queued)
                    == VoxelPrintingStationBlockEntity.QueueCancelResult.NOT_FOUND, "Cancellation replay accepted");
            var broken = saved.copy(); broken.remove("active_requester_id");
            boolean rejected = false;
            try { new VoxelPrintingStationBlockEntity(pos, state).loadWithComponents(broken, level.registryAccess()); }
            catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Malformed reservation invented a requester");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void oneOutputSlotMatchesClientAndSupportsShiftExtraction(GameTestHelper h) {
        var state = ModBlocks.VOXEL_PRINTING_STATION.get().defaultBlockState();
        var station = new VoxelPrintingStationBlockEntity(BlockPos.ZERO, state);
        try (var fixture = player(h, "PrintSlots")) {
            var inventory = fixture.player.getInventory();
            var server = new VoxelPrintingStationMenu(1, inventory, station, ContainerLevelAccess.NULL);
            var client = new VoxelPrintingStationMenu(1, inventory);
            h.assertTrue(server.slots.size() == 37 && client.slots.size() == 37, "Menu slot contracts differ");
            h.assertTrue(!server.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Output accepts input");
            station.setItem(0, new ItemStack(Items.DIAMOND, 7));
            var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
            station.clearContent(); station.loadWithComponents(saved, h.getLevel().registryAccess());
            server.quickMoveStack(fixture.player, 0);
            h.assertTrue(station.isEmpty() && inventory.countItem(Items.DIAMOND) == 7, "Shift extraction lost output");
        }
        h.succeed();
    }
}
