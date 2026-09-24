package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.CaptainChairBlock;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.entity.SeatEntity;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Opt-in fresh-world checks; never opens or modifies a user save. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class CaptainChairRenderSmoke {
    private static final BlockPos CHAIR = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"south", "west", "north", "east", "seated", "seated-eye", "dismounted", "resource-reload"};
    private static boolean started, done;
    private static long start, stageStart;
    private static int stage;
    private static CompletableFuture<?> setup, stageSetup, reload;
    private static ArmorStand camera;

    @SubscribeEvent
    public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.chairSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(40);
            String name = "chair-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240) throw new IllegalStateException("Chair smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) return;
        if (setup == null) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                for (int x = 995; x <= 1006; x++) for (int z = 995; z <= 1006; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 189, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 190; y <= 194; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (int x = 998; x <= 1002; x++)
                    level.setBlockAndUpdate(new BlockPos(x, 193, 998), Blocks.SEA_LANTERN.defaultBlockState());
                level.setBlockAndUpdate(CHAIR, ModBlocks.CAPTAIN_CHAIR.get().defaultBlockState());
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    var state = ModBlocks.CAPTAIN_CHAIR.get().defaultBlockState().setValue(CaptainChairBlock.FACING, facing);
                    var shape = state.getShape(level, CHAIR);
                    check(shape.max(Direction.Axis.Y) > 1.4, "Headrest missing from shape");
                    check(shape.min(Direction.Axis.X) >= 0 && shape.max(Direction.Axis.X) <= 1
                            && shape.min(Direction.Axis.Z) >= 0 && shape.max(Direction.Axis.Z) <= 1, "Footprint exceeds block");
                    Vec3 rear = rotate(new Vec3(.5, 1.3, .15), facing);
                    Vec3 gap = rotate(new Vec3(.12, .6, .48), facing);
                    check(shape.toAabbs().stream().anyMatch(b -> b.contains(rear)), "Headrest rotation: " + facing);
                    check(shape.toAabbs().stream().noneMatch(b -> b.contains(gap)), "Arm opening was filled: " + facing);
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
                Direction facing = switch (stage) { case 1 -> Direction.WEST; case 2 -> Direction.NORTH; case 3 -> Direction.EAST; default -> Direction.SOUTH; };
                level.setBlockAndUpdate(CHAIR, ModBlocks.CAPTAIN_CHAIR.get().defaultBlockState().setValue(CaptainChairBlock.FACING, facing));
                if (stage < 4 || stage == 7) {
                    player.teleportTo(level, 1003, 190.2, 1004, Set.of(), 145, 10);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
                if (stage == 4) {
                    // A two-block-high ceiling over the actual seat and all dismount candidates.
                    for (int x = 999; x <= 1001; x++) for (int z = 999; z <= 1001; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 192, z), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                    var hit = new BlockHitResult(Vec3.atCenterOf(CHAIR), Direction.SOUTH, CHAIR, false);
                    level.getBlockState(CHAIR).useWithoutItem(level, player, hit);
                    check(player.getVehicle() instanceof SeatEntity, "Right click did not mount chair");
                    level.getBlockState(CHAIR).useWithoutItem(level, player, hit);
                    check(level.getEntitiesOfClass(SeatEntity.class, new AABB(CHAIR)).size() == 1, "Repeat click duplicated seat");
                    player.setYRot(0);
                    player.setXRot(14);
                    player.setYHeadRot(0);
                    player.yBodyRot = 0;
                }
                if (stage == 5) {
                    check(player.isPassenger(), "Passenger lost while seated");
                    check(player.getEyeY() < 192, "Seated head clips two-block cockpit ceiling");
                }
                if (stage == 6) {
                    player.stopRiding();
                    check(!player.isPassenger(), "Dismount failed");
                    check(player.getBoundingBox().maxY <= 192.01, "Dismount enters cockpit ceiling");
                    check(level.noCollision(player), "Dismount inside chair or wall");
                }
                if (stage == 7) {
                    for (int x = 999; x <= 1001; x++) for (int z = 999; z <= 1001; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 192, z), Blocks.AIR.defaultBlockState());
                    check(level.getEntitiesOfClass(SeatEntity.class, new AABB(CHAIR)).isEmpty(), "Unused seat did not disappear");
                }
            });
            camera = null;
            return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (stage == 4 || stage == 5) {
            mc.player.setYRot(0);
            mc.player.yRotO = 0;
            mc.player.setXRot(stage == 5 ? 28 : 0);
            mc.player.xRotO = mc.player.getXRot();
            mc.player.setYHeadRot(0);
            mc.player.yHeadRotO = 0;
            mc.player.yBodyRot = 0;
            mc.player.yBodyRotO = 0;
        }
        if (stage == 5) mc.setCameraEntity(mc.player);
        else if (camera == null) {
            camera = new ArmorStand(mc.level, 1002.8, 190.55, 1004.25);
            double dx = 2.3, dz = 3.75;
            if (stage < 4) {
                double angle = stage * Math.PI / 2;
                double nx = dx * Math.cos(angle) - dz * Math.sin(angle);
                dz = dx * Math.sin(angle) + dz * Math.cos(angle);
                dx = nx;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            camera.moveTo(1000.5 + dx, 190.1, 1000.5 + dz, yaw, 14);
            camera.setYHeadRot(yaw);
            camera.yHeadRotO = yaw;
            camera.yBodyRot = yaw;
            camera.yBodyRotO = yaw;
            camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
        if (stage == 7 && reload == null) { reload = mc.reloadResourcePacks(); return; }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        if (stage == 7) {
            var id = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/chair_instruments");
            var sprite = mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(id);
            check(sprite.contents().name().equals(id), "Instrument texture missing after reload");
            var item = mc.getItemRenderer().getModel(new ItemStack(ModBlocks.CAPTAIN_CHAIR.get()), mc.level, mc.player, 0);
            check(item != mc.getModelManager().getMissingModel(), "Missing inventory model");
        }
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(directory.resolve(NAMES[stage] + ".png"));
        }
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("chair-smoke.txt"),
                    "Eight captures; four facings, raised shape, arm openings, mounting, repeated click, cockpit clearance, dismount, cleanup, item and resource reload passed.\n");
            mc.setCameraEntity(mc.player);
            done = true;
            mc.stop();
        }
        stageStart = 0;
        stageSetup = null;
    }

    private static Vec3 rotate(Vec3 p, Direction facing) {
        return switch (facing) {
            case WEST -> new Vec3(1 - p.z, p.y, p.x);
            case NORTH -> new Vec3(1 - p.x, p.y, 1 - p.z);
            case EAST -> new Vec3(p.z, p.y, 1 - p.x);
            default -> p;
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
