package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipAiTerminalBlock;
import com.starboundmc.menu.ShipAiTerminalMenu;
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

/** Opt-in visual checks in a fresh disposable world, never a user save. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class NovaTerminalRenderSmoke {
    private static final BlockPos TERMINAL = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"north", "east", "south", "west", "animation", "menu", "resource-reload"};
    private static boolean started, done;
    private static long start, stageStart;
    private static int stage, animationFrame;
    private static CompletableFuture<?> setup, stageSetup, reload;
    private static ArmorStand camera;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.novaTerminalSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(40);
            String name = "nova-terminal-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240)
            throw new IllegalStateException("NOVA terminal smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (setup == null) {
            if (mc.screen != null) return;
            setup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                for (int x = 995; x <= 1006; x++) for (int z = 995; z <= 1006; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 189; y <= 194; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    var state = ModBlocks.SHIP_AI_TERMINAL.get().defaultBlockState().setValue(ShipAiTerminalBlock.FACING, facing);
                    var shape = state.getShape(level, TERMINAL);
                    check(shape.min(Direction.Axis.Y) > 0 && shape.max(Direction.Axis.Y) <= 1, "Wall footprint height");
                    check(shape.min(Direction.Axis.X) >= 0 && shape.max(Direction.Axis.X) <= 1
                            && shape.min(Direction.Axis.Z) >= 0 && shape.max(Direction.Axis.Z) <= 1, "Footprint exceeds block");
                    check(shape.toAabbs().stream().noneMatch(b -> b.contains(rotate(new Vec3(.5,.5,.25), facing))),
                            "Clear space in front was filled: " + facing);
                    check(shape.toAabbs().stream().anyMatch(b -> b.contains(rotate(new Vec3(.5,.5,.94), facing))),
                            "Rear mounting shape missing: " + facing);
                    check(!state.canOcclude(), "Terminal occludes neighbouring wall");
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
                Direction facing = direction();
                for (int x = 998; x <= 1002; x++) for (int z = 998; z <= 1002; z++)
                    for (int y = 189; y <= 192; y++) level.setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
                var rear = TERMINAL.relative(facing.getOpposite());
                for (int offset = -1; offset <= 1; offset++) for (int dy = -1; dy <= 1; dy++) {
                    var wall = rear.relative(facing.getClockWise(), offset).offset(0,dy,0);
                    level.setBlockAndUpdate(wall, Blocks.GRAY_CONCRETE.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(1000,193,1000), Blocks.SEA_LANTERN.defaultBlockState());
                var state = ModBlocks.SHIP_AI_TERMINAL.get().defaultBlockState().setValue(ShipAiTerminalBlock.FACING, facing);
                level.setBlockAndUpdate(TERMINAL, state);
                player.teleportTo(level, stage == 5 ? 1000.5 : 1005.5, 189.1,
                        stage == 5 ? 998.5 : 1005.5, Set.of(), 0, 0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                if (stage == 5) {
                    state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(TERMINAL), Direction.NORTH, TERMINAL, false));
                    check(player.containerMenu instanceof ShipAiTerminalMenu, "Right click did not open NOVA menu");
                    check(player.containerMenu.stillValid(player), "NOVA menu invalid beside terminal");
                }
            });
            camera = null;
            return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (stage == 5) { if (!(mc.screen instanceof ShipAiTerminalScreen)) return; }
        else if (mc.screen != null) return;
        if (camera == null) {
            double angle = (stage < 4 ? stage : 0) * Math.PI / 2;
            double initialX = stage == 4 ? 0 : -1.5;
            double dx = initialX * Math.cos(angle) + 2.8 * Math.sin(angle);
            double dz = initialX * Math.sin(angle) - 2.8 * Math.cos(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            camera = new ArmorStand(mc.level, 1000.5+dx, 189.3, 1000.5+dz);
            camera.moveTo(1000.5+dx,189.3,1000.5+dz,yaw,10);
            camera.setYHeadRot(yaw); camera.yHeadRotO=yaw;
            camera.yBodyRot=yaw; camera.yBodyRotO=yaw; camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
        if (stage == 6 && reload == null) { reload = mc.reloadResourcePacks(); return; }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        double seconds = (System.nanoTime()-stageStart)/1e9;
        if (stage == 4 && seconds >= 1 + animationFrame * .1 && animationFrame < 55) {
            capture(mc, "motion-%02d".formatted(animationFrame++));
        }
        if (seconds < (stage == 4 ? 6.6 : 3)) return;
        if (stage != 5) {
            var state = mc.level.getBlockState(TERMINAL);
            check(state.getValue(ShipAiTerminalBlock.FACING) == direction(), "Facing failed to synchronize");
            var baked = mc.getBlockRenderer().getBlockModel(state);
            check(baked != mc.getModelManager().getMissingModel(), "Missing block model");
            var quads = baked.getQuads(state, null, RandomSource.create(0));
            check(quads.size() >= 1400, "Incomplete OBJ: " + quads.size());
            var screenId = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/nova_screen");
            check(quads.stream().anyMatch(q -> q.getSprite().contents().name().equals(screenId)), "Missing portrait faces");
            var sprite = mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(screenId);
            check(sprite.contents().getUniqueFrames().count() == 48, "Animation did not load all 48 frames");
            for (String name : new String[]{"screen","titanium","graphite","edge","recess","cyan","ident"}) {
                var id = ResourceLocation.fromNamespaceAndPath("starboundmc", "block/nova_"+name);
                check(mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(id)
                        .contents().name().equals(id), "Missing material " + name);
            }
            check(mc.getItemRenderer().getModel(new ItemStack(ModBlocks.SHIP_AI_TERMINAL.get()),mc.level,mc.player,0)
                    != mc.getModelManager().getMissingModel(), "Missing item model");
        }
        capture(mc,NAMES[stage]);
        if (stage == 5) mc.player.closeContainer();
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("nova-terminal-smoke.txt"),
                    "Passed: four facings, wall footprint and front clearance, baked OBJ, seven materials, 48-frame animation, right-click menu, item and resource reload. 55 motion captures saved.\n");
            mc.setCameraEntity(mc.player); done=true; mc.stop();
        }
        stageStart=0; stageSetup=null;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var directory=mc.gameDirectory.toPath().resolve("screenshots"); Files.createDirectories(directory);
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(name+".png")); }
    }
    private static Direction direction() {
        return switch(stage) {case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH;};
    }
    private static Vec3 rotate(Vec3 p, Direction facing) {
        return switch(facing) {case EAST -> new Vec3(1-p.z,p.y,p.x); case SOUTH -> new Vec3(1-p.x,p.y,1-p.z);
            case WEST -> new Vec3(p.z,p.y,1-p.x); default -> p;};
    }
    private static void check(boolean condition, String message) { if(!condition)throw new IllegalStateException(message); }
}
