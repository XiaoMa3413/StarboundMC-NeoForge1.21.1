package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.network.AddFuelPacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.warp.ShipFuelService;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

/** Opt-in asset and fuel interaction checks, always in a fresh disposable world. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class FuelPanelRenderSmoke {
    private static final BlockPos PANEL = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"north", "east", "south", "west", "menu-loaded", "menu-refuelled", "resource-reload"};
    private static boolean started, done, packetSent;
    private static long start, stageStart;
    private static int stage;
    private static CompletableFuture<?> setup, stageSetup, verification, reload;
    private static ArmorStand camera;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.fuelPanelSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(35);
            String name = "fuel-panel-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240)
            throw new IllegalStateException("Fuel panel smoke timed out at stage " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (setup == null) {
            if (mc.screen != null) return;
            setup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(level, 1000.5, 189.3, 998.5, Set.of(), 0, 0);
                for (int x = 995; x <= 1006; x++) for (int z = 995; z <= 1006; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SMOOTH_STONE.defaultBlockState());
                var block = ModBlocks.FUEL_CONTROLLER.get();
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    var state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                    check(!state.canOcclude(), "Panel occludes neighbouring wall");
                    check(state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing.getClockWise(), "Rotation");
                    check(state.mirror(Mirror.LEFT_RIGHT).getValue(BlockStateProperties.HORIZONTAL_FACING)
                            == (facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing), "Mirror");
                    for (var shape : new net.minecraft.world.phys.shapes.VoxelShape[]{state.getShape(level, PANEL), state.getCollisionShape(level, PANEL)}) {
                        check(Math.abs(shape.min(Direction.Axis.Y) - .125) < 1e-6 && Math.abs(shape.max(Direction.Axis.Y) - .875) < 1e-6, "Vertical bounds");
                        var bounds = shape.bounds();
                        check((facing.getAxis() == Direction.Axis.Z ? bounds.getZsize() : bounds.getXsize()) < .24, "Panel too thick");
                        check(!bounds.contains(rotate(new Vec3(.5, .5, .5), facing)), "Empty front space blocks movement");
                        check(bounds.contains(rotate(new Vec3(.5, .5, .99), facing)), "Panel misses wall back");
                    }
                    BlockPos wall = PANEL.relative(facing.getOpposite());
                    level.setBlockAndUpdate(PANEL, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                    var hit = new BlockHitResult(Vec3.atCenterOf(wall), facing, wall, false);
                    var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
                    check(block.getStateForPlacement(context).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing, "Wall placement facing");
                    level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
                }
                for (Direction face : new Direction[]{Direction.UP, Direction.DOWN}) {
                    var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                            new BlockHitResult(Vec3.atCenterOf(PANEL), face, PANEL, false));
                    check(block.getStateForPlacement(context).getValue(BlockStateProperties.HORIZONTAL_FACING)
                            == context.getHorizontalDirection().getOpposite(), "Top/bottom fallback");
                }
                level.setBlockAndUpdate(PANEL, block.defaultBlockState());
                check(level.getBlockState(PANEL).canSurvive(level, PANEL), "Old unsupported placement invalidated");
                var fuel = (FuelControllerBlockEntity) level.getBlockEntity(PANEL);
                check(fuel.getContainerSize() == 5, "Fuel slots changed");
                fuel.setItem(0, new ItemStack(Items.COAL, 2));
                var restored = new FuelControllerBlockEntity(PANEL, fuel.getBlockState());
                restored.loadWithComponents(fuel.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                check(restored.getItem(0).getCount() == 2 && restored.getContainerSize() == 5, "Fuel inventory NBT round trip");
                ShipStateData.get(server).setFuel(ShipFuelService.MAX_FUEL - 15);
                var menu = new FuelControllerMenu(7, player.getInventory(), fuel, ContainerLevelAccess.create(level, PANEL));
                menu.addAllFuelItems(player);
                check(fuel.getItem(0).getCount() == 1 && ShipFuelService.getFuel(server) == ShipFuelService.MAX_FUEL - 5, "Partial capacity must retain surplus whole item");
                fuel.clearContent();
                ShipStateData.get(server).setFuel(0);
            });
            return;
        }
        if (!setup.isDone()) return;
        setup.join();
        if (stageSetup == null) {
            stageSetup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var player = server.getPlayerList().getPlayers().getFirst();
                player.closeContainer();
                Direction facing = direction(stage);
                for (int x = 998; x <= 1002; x++) for (int z = 998; z <= 1002; z++) for (int y = 189; y <= 193; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!p.equals(PANEL)) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
                for (int offset = -2; offset <= 2; offset++) for (int y = 189; y <= 193; y++)
                    level.setBlockAndUpdate(PANEL.relative(facing.getOpposite()).relative(facing.getClockWise(), offset).atY(y), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                level.setBlockAndUpdate(PANEL.above(2).relative(facing), Blocks.SEA_LANTERN.defaultBlockState());
                var state = ModBlocks.FUEL_CONTROLLER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                level.setBlockAndUpdate(PANEL, state);
                player.teleportTo(level, menuStage() ? 1000.5 : 1005.5, 189.4, menuStage() ? 998.5 : 1005.5, Set.of(), 0, 0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                if (stage == 4) {
                    var fuel = (FuelControllerBlockEntity) level.getBlockEntity(PANEL);
                    fuel.setItem(0, new ItemStack(Items.COAL, 2));
                    fuel.setItem(1, new ItemStack(Items.CHARCOAL));
                    fuel.setItem(2, new ItemStack(Items.BLAZE_POWDER));
                    fuel.setItem(3, new ItemStack(ModItems.FUEL_CRYSTAL.get()));
                    fuel.setItem(4, new ItemStack(Items.COAL));
                    ShipFuelService.syncToPlayer(player);
                }
                if (menuStage()) {
                    state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(PANEL), Direction.NORTH, PANEL, false));
                    check(player.containerMenu instanceof FuelControllerMenu, "Right-click menu failed");
                    check(player.containerMenu.stillValid(player), "Fuel menu invalid next to panel");
                }
            });
            camera = null;
            return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (menuStage()) {
            if (!(mc.screen instanceof FuelControllerScreen)) return;
        } else if (mc.screen != null) return;
        if (camera == null) {
            double angle = (stage < 4 ? stage : 0) * Math.PI / 2;
            double dx = -1.55 * Math.cos(angle) + 2.85 * Math.sin(angle);
            double dz = -1.55 * Math.sin(angle) - 2.85 * Math.cos(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            camera = new ArmorStand(mc.level, 1000.5 + dx, 189.1, 1000.5 + dz);
            camera.moveTo(1000.5 + dx, 189.1, 1000.5 + dz, yaw, 8);
            camera.setYHeadRot(yaw);
            camera.yHeadRotO = yaw;
            camera.yBodyRot = yaw;
            camera.yBodyRotO = yaw;
            camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
        if (stage == 5 && !packetSent) {
            ModNetwork.sendToServer(new AddFuelPacket());
            packetSent = true;
        }
        if (stage == 6 && reload == null) { reload = mc.reloadResourcePacks(); return; }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        if (verification == null) {
            verification = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer();
                var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var fuel = (FuelControllerBlockEntity) level.getBlockEntity(PANEL);
                if (stage == 4) check(!fuel.isEmpty() && ShipFuelService.getFuel(server) == 0, "Premature refuelling");
                if (stage == 5) check(fuel.isEmpty() && ShipFuelService.getFuel(server) == 105, "Refuel packet did not consume exactly 105 fuel");
            });
            return;
        }
        if (!verification.isDone()) return;
        verification.join();
        if (menuStage()) {
            check(mc.player.containerMenu.slots.size() == 41, "Menu slots changed");
            for (int i = 0; i < 5; i++) check(mc.player.containerMenu.getSlot(i).hasItem() == (stage == 4), "Client slot sync failed");
            check(com.starboundmc.network.ClientNetworkState.fuel() == (stage == 4 ? 0 : 105), "Client fuel amount did not synchronize");
        } else {
            var state = mc.level.getBlockState(PANEL);
            check(state.is(ModBlocks.FUEL_CONTROLLER.get()) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction(stage), "Client facing sync");
            var baked = mc.getBlockRenderer().getBlockModel(state);
            check(baked != mc.getModelManager().getMissingModel(), "Missing panel model");
            var quads = baked.getQuads(state, null, RandomSource.create(0));
            check(quads.size() >= 1000, "Incomplete OBJ geometry");
            var screen = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/fuel_panel_screen");
            check(quads.stream().anyMatch(q -> q.getSprite().contents().name().equals(screen)), "Missing screen geometry");
            for (String texture : new String[]{"titanium", "graphite", "edge", "rubber", "amber", "screen", "markings"}) {
                var id = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/fuel_panel_" + texture);
                check(mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(id).contents().name().equals(id), "Missing texture " + texture);
            }
            check(mc.getItemRenderer().getModel(new ItemStack(ModBlocks.FUEL_CONTROLLER.get()), mc.level, mc.player, 0)
                    != mc.getModelManager().getMissingModel(), "Missing item model");
        }
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(NAMES[stage] + ".png")); }
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("fuel-panel-smoke.txt"),
                    "Seven stages passed: four wall facings, thin selection/collision, placement and rotation/mirror, legacy unsupported placement, inventory NBT, surplus retention, real menu handler, AddFuelPacket and 105 fuel consumption, client slots, baked OBJ/materials/item and resource reload.\n");
            mc.setCameraEntity(mc.player);
            done = true;
            mc.stop();
        }
        stageStart = 0;
        stageSetup = null;
        verification = null;
    }

    private static Direction direction(int stage) {
        return switch (stage) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH; };
    }

    private static boolean menuStage() { return stage == 4 || stage == 5; }

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
