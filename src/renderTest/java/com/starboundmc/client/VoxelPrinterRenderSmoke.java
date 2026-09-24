package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.VoxelPrintingStationBlock;
import com.starboundmc.network.SyncVoxelMachinePacket;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opt-in rendering check in a fresh world, excluded from normal mod builds. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class VoxelPrinterRenderSmoke {
    private static final BlockPos PRINTER = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"idle", "printing-early", "printing-middle", "printing-late", "complete", "resource-reload"};
    private static boolean started, done;
    private static long start, stageStart;
    private static int stage;
    private static CompletableFuture<?> setup, reload;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.printerSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(35);
            String name = "printer-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240) throw new IllegalStateException("Printer smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) return;
        if (setup == null) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int x = 996; x <= 1005; x++) for (int z = 996; z <= 1004; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int x = 998; x <= 1002; x++) for (int y = 189; y <= 193; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, 1001), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(999, 192, 1000), Blocks.SEA_LANTERN.defaultBlockState());
                level.setBlockAndUpdate(PRINTER, ModBlocks.VOXEL_PRINTING_STATION.get().defaultBlockState()
                        .setValue(VoxelPrintingStationBlock.FACING, Direction.NORTH));
                player.teleportTo(level, 1002.5, 190, 997.2, Set.of(), 31f, 16.7f);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            return;
        }
        if (!setup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        setup.join();
        if (stage == 5 && reload == null) {
            reload = mc.reloadResourcePacks();
            stageStart = 0;
            return;
        }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        int remaining = switch (stage) { case 1 -> 160; case 2 -> 100; case 3 -> 20; default -> 0; };
        ClientVoxelMachineState.apply(new SyncVoxelMachinePacket(PRINTER, remaining, 200, 0,
                ResourceLocation.withDefaultNamespace("diamond_pickaxe"), stage == 0 ? 0 : 1), mc.level.getGameTime());
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(directory.resolve(NAMES[stage] + ".png"));
        }
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("printer-smoke.txt"), "Completed six captures, including resource reload.\n");
            done = true;
            mc.stop();
        }
        stageStart = System.nanoTime();
    }
}
