package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.StarmapTerminalBlock;
import com.starboundmc.client.starmap.StarmapTerminalScreen;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.warp.ShipStateData;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opt-in rendering and interaction checks in a disposable, freshly created world. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class StarmapConsoleRenderSmoke {
    private static final BlockPos CONSOLE = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"north", "east", "south", "west", "menu-offline", "menu-online", "resource-reload"};
    private static boolean started, done;
    private static long start, stageStart;
    private static int stage;
    private static CompletableFuture<?> setup, stageSetup, reload;
    private static ArmorStand camera;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.starmapConsoleSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(40);
            String name = "starmap-console-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240)
            throw new IllegalStateException("Starmap console smoke timed out at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (setup == null) {
            if (mc.screen != null) return;
            setup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                for (int x = 995; x <= 1006; x++) for (int z = 995; z <= 1006; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 189, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 190; y <= 194; y++)
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (int x = 998; x <= 1002; x++)
                    level.setBlockAndUpdate(new BlockPos(x, 193, 998), Blocks.SEA_LANTERN.defaultBlockState());
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    var state = ModBlocks.STARMAP_TERMINAL.get().defaultBlockState()
                            .setValue(StarmapTerminalBlock.FACING, facing);
                    var shape = state.getShape(level, CONSOLE);
                    var collision = state.getCollisionShape(level, CONSOLE);
                    check(shape.min(Direction.Axis.Y) == 0 && shape.max(Direction.Axis.Y) < 1,
                            "Unexpected vertical bounds " + facing);
                    check(shape.min(Direction.Axis.X) >= 0 && shape.max(Direction.Axis.X) <= 1
                            && shape.min(Direction.Axis.Z) >= 0 && shape.max(Direction.Axis.Z) <= 1,
                            "Footprint exceeds block " + facing);
                    check(!state.canOcclude(), "Open console occludes neighbouring blocks");
                    for (var current : new net.minecraft.world.phys.shapes.VoxelShape[]{shape, collision}) {
                        check(current.toAabbs().stream().noneMatch(b -> b.contains(rotate(new Vec3(.5, .4, .2), facing))),
                                "Front foot clearance filled " + facing);
                        check(current.toAabbs().stream().noneMatch(b -> b.contains(rotate(new Vec3(.5, .82, .2), facing))),
                                "Empty space above low deck filled " + facing);
                        check(current.toAabbs().stream().anyMatch(b -> b.contains(rotate(new Vec3(.5, .4, .65), facing))),
                                "Pedestal missing " + facing);
                        check(current.toAabbs().stream().anyMatch(b -> b.contains(rotate(new Vec3(.5, .88, .81), facing))),
                                "Raised rear deck missing " + facing);
                    }
                }
            });
            return;
        }
        if (!setup.isDone()) return;
        setup.join();
        if (stageSetup == null) {
            stageSetup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                Direction facing = direction(stage);
                var state = ModBlocks.STARMAP_TERMINAL.get().defaultBlockState().setValue(StarmapTerminalBlock.FACING, facing);
                level.setBlockAndUpdate(CONSOLE, state);
                // Keep the test player out of the model captures; approach only for interaction.
                player.teleportTo(level, menuStage() ? 1000.5 : 1005.5,
                        190.05, menuStage() ? 998.6 : 1005.5, Set.of(), 0, 20);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                if (stage == 5) {
                    // Change only this newly created disposable world's authoritative state.
                    var server = mc.getSingleplayerServer();
                    var ship = ShipStateData.get(server);
                    long now = server.overworld().getGameTime();
                    check(ship.beginCoreReboot(now, 1), "Could not begin fixture core reboot");
                    check(ship.finishCoreRebootIfDue(now + 1), "Could not finish fixture core reboot");
                }
                if (menuStage()) {
                    var hit = new BlockHitResult(Vec3.atCenterOf(CONSOLE), Direction.NORTH, CONSOLE, false);
                    state.useWithoutItem(level, player, hit);
                    check(player.containerMenu instanceof StarmapTerminalMenu, "Right-click did not open starmap menu");
                    check(player.containerMenu.stillValid(player), "Starmap menu invalid beside console");
                }
            });
            camera = null;
            return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (menuStage()) {
            if (!(mc.screen instanceof StarmapTerminalScreen)) return;
            if (!ClientShipEnvironmentState.hasSupportedSnapshot(mc.player.containerMenu.containerId)) return;
        } else if (mc.screen != null) return;
        if (camera == null) {
            double angle = (stage < 4 ? stage : 0) * Math.PI / 2;
            double dx = -2.3 * Math.cos(angle) + 3.75 * Math.sin(angle);
            double dz = -2.3 * Math.sin(angle) - 3.75 * Math.cos(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            camera = new ArmorStand(mc.level, 1000.5 + dx, 190.1, 1000.5 + dz);
            camera.moveTo(1000.5 + dx, 190.1, 1000.5 + dz, yaw, 17);
            camera.setYHeadRot(yaw);
            camera.yHeadRotO = yaw;
            camera.yBodyRot = yaw;
            camera.yBodyRotO = yaw;
            camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
        if (stage == 6 && reload == null) { reload = mc.reloadResourcePacks(); return; }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        if (menuStage()) {
            check(ClientShipEnvironmentState.isLocked(mc.player.containerMenu.containerId) == (stage == 4),
                    "Menu lock did not follow authoritative offline/online core state");
        } else {
            var state = mc.level.getBlockState(CONSOLE);
            check(state.is(ModBlocks.STARMAP_TERMINAL.get()) && state.getValue(StarmapTerminalBlock.FACING) == direction(stage),
                    "Client facing did not synchronise");
            var baked = mc.getBlockRenderer().getBlockModel(state);
            check(baked != mc.getModelManager().getMissingModel(), "Missing block model");
            var quads = baked.getQuads(state, null, RandomSource.create(0));
            check(quads.size() >= 900, "Incomplete OBJ geometry: " + quads.size());
            var chartId = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/terminal_chart");
            check(quads.stream().anyMatch(q -> q.getSprite().contents().name().equals(chartId)), "No chart quads");
            for (String texture : new String[]{"chart", "instruments", "titanium", "graphite", "edge", "amber", "cyan"}) {
                var id = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/terminal_" + texture);
                var sprite = mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(id);
                check(sprite.contents().name().equals(id), "Missing atlas texture: " + texture);
            }
            var item = mc.getItemRenderer().getModel(new ItemStack(ModBlocks.STARMAP_TERMINAL.get()), mc.level, mc.player, 0);
            check(item != mc.getModelManager().getMissingModel(), "Missing item model");
        }
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(directory.resolve(NAMES[stage] + ".png"));
        }
        if (menuStage()) mc.player.closeContainer();
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("starmap-console-smoke.txt"),
                    "Seven captures: four facings, shaped collision/selection, front clearance, no occlusion, right-click menu with authoritative offline/online snapshots, baked OBJ/chart, seven materials, item and resource reload passed.\n");
            mc.setCameraEntity(mc.player);
            done = true;
            mc.stop();
        }
        stageStart = 0;
        stageSetup = null;
    }

    private static Direction direction(int stage) {
        return switch (stage) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH; };
    }

    private static boolean menuStage() {
        return stage == 4 || stage == 5;
    }

    private static Vec3 rotate(Vec3 p, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(1 - p.z, p.y, p.x);
            case SOUTH -> new Vec3(1 - p.x, p.y, 1 - p.z);
            case WEST -> new Vec3(p.z, p.y, 1 - p.x);
            default -> p;
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
