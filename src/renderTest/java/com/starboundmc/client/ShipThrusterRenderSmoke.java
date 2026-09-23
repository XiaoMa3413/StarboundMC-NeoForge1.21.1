package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipEngineBlock;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
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

/** Opt-in visual fixture. Creates a fresh world, never opens a user save. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ShipThrusterRenderSmoke {
    private static boolean started, done;
    private static long start, stageStart, revision = 1_000_000;
    private static int stage;
    private static CompletableFuture<?> setup;
    private static final String[] NAMES = {"docked", "accelerating", "cruising", "crew-hold", "arriving", "stopped", "four-directions"};

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.thrusterSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true; start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(40);
            String name = "thruster-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 200) throw new IllegalStateException("Thruster smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) return;
        if (setup == null) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                for (int x = 994; x <= 1006; x++) for (int z = 994; z <= 1006; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SEA_LANTERN.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(1000, 190, 1000), ModBlocks.SHIP_ENGINE.get().defaultBlockState());
                player.teleportTo(level, 1004.5, 190, 996.8, Set.of(), 47.7f, 11.8f);
                player.getAbilities().flying = true; player.onUpdateAbilities();
                player.getInventory().setItem(0, new ItemStack(ModBlocks.SHIP_ENGINE.get()));
            });
            return;
        }
        if (!setup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        setup.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        int tick = switch(stage) {case 1 -> 80; case 2, 3, 6 -> 200; case 4 -> 380; default -> 0;};
        FlightPhase phase = switch(stage) {
            case 1 -> FlightPhase.ACCELERATE;
            case 2, 3, 6 -> FlightPhase.HYPERSPACE;
            case 4 -> FlightPhase.ARRIVE;
            default -> FlightPhase.DOCKED;
        };
        ClientPlanetState.applyFlightSnapshot(revision++, 0, phase,
                com.starboundmc.warp.UniverseNavigation.universeDock("sys1:lush"),
                new com.starboundmc.space.UniverseDelta(0, 0, 1), 0, 0, 0, tick, 400, "sys1:molten", stage == 3);
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(directory.resolve(NAMES[stage] + ".png"));
        }
        if (stage == 5) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                int i = 0;
                for (var dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                    level.setBlockAndUpdate(new BlockPos(997 + i * 3, 190, 1000),
                            ModBlocks.SHIP_ENGINE.get().defaultBlockState().setValue(ShipEngineBlock.FACING, dir));
                    i++;
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(level, 1001.5, 195, 990, Set.of(), 0, 27);
            });
        }
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("thruster-smoke.txt"), "Completed all seven render captures.\n");
            done = true; mc.stop();
        }
        stageStart = System.nanoTime();
    }
}
