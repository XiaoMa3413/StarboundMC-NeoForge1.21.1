package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipLightBlock;
import com.starboundmc.block.Stage2Blocks;
import com.starboundmc.world.ShipDimensions;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Opt-in integration and visual checks, in an isolated fresh world. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class MachineryRenderSmoke {
    private static final BlockPos POS = new BlockPos(1000, 189, 1000);
    private static final String[] IDS = {"voxel_refinery", "titanium_alloy_furnace", "matter_manipulator_workbench",
            "life_support_station", "epp_service_station", "ship_console", "ship_engine_unit", "beacon_emitter", "ship_door", "ship_light"};
    private static final String[] MENUS = {"VoxelRefineryMenu", "AlloyFurnaceMenu", "UpgradeMenu", "LifeSupportMenu",
            "EppServiceMenu", "ShipConsoleMenu", "ShipEngineMenu"};
    private static final List<String> REPORT = new ArrayList<>();
    private static boolean started, done, modelsChecked;
    private static long start, stageStart;
    private static int stage, motionFrame;
    private static CompletableFuture<?> setup, stageSetup, reload;
    private static ArmorStand camera;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.machinerySmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true; start = System.nanoTime();
            mc.options.pauseOnLostFocus = false; mc.options.hideGui = true;
            mc.options.renderDistance().set(4); mc.options.fov().set(40);
            String name = "machinery-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime()-start)/1e9 > 300) throw new IllegalStateException("Machinery smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (setup == null) {
            if (mc.screen != null) return;
            setup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for (int x=995; x<=1008; x++) for (int z=995; z<=1008; z++) {
                    level.setBlockAndUpdate(new BlockPos(x,188,z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y=189; y<=194; y++) level.setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
                }
                player.teleportTo(level,1000.5,189.1,998.5,Set.of(),0,0);
                player.getAbilities().flying=true; player.onUpdateAbilities();
                checkMachines(level, player);
                checkLamp(level, player);
                checkDoor(level);
                check(level.getRecipeManager().byKey(id("print_ship_light")).isPresent(), "Lamp recipe did not load");
                REPORT.add("Server: seven menus, machine shapes, 12 lamp attachments, support removal and loot, light level 15, door linkage and passage, recipe.");
            });
            return;
        }
        if (!setup.isDone()) return;
        setup.join();
        if (!modelsChecked && mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) {
            checkModels(mc); modelsChecked=true;
        }
        if (stageSetup == null) {
            stageSetup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for (int x=998;x<=1002;x++) for(int z=998;z<=1002;z++) for(int y=189;y<=192;y++)
                    level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                if (stage != 9 && stage != 11 && stage != 12)
                    level.setBlockAndUpdate(POS.above(3),Blocks.SEA_LANTERN.defaultBlockState());
                var state = stageState();
                if (state.is(ModBlocks.SHIP_LIGHT.get())) {
                    Direction support = switch(state.getValue(ShipLightBlock.FACE)) {
                        case WALL -> state.getValue(ShipLightBlock.FACING).getOpposite();
                        case FLOOR -> Direction.DOWN; case CEILING -> Direction.UP;
                    };
                    var rear=POS.relative(support);
                    level.setBlockAndUpdate(rear,Blocks.GRAY_CONCRETE.defaultBlockState());
                }
                level.setBlockAndUpdate(POS,state);
                if (stage==8 || stage==10) level.setBlockAndUpdate(POS.above(),state);
                player.teleportTo(level,1007.5,189.1,1007.5,Set.of(),0,0);
            });
            camera=null;return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (mc.screen != null) return;
        if (camera == null) {
            double dx=-1.6,dz=-2.8,y=188.6;
            float pitch=18;
            if (stage==8 || stage==10) {dx=-2;dz=-3.5;y=189.0;pitch=12;}
            if (stage==11) {y=189.4;pitch=34;}
            if (stage==12) {y=187.8;pitch=-5;}
            float yaw=(float)Math.toDegrees(Math.atan2(dx,-dz));
            camera=new ArmorStand(mc.level,1000.5+dx,y,1000.5+dz);
            camera.moveTo(1000.5+dx,y,1000.5+dz,yaw,pitch);
            camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;camera.yBodyRot=yaw;camera.yBodyRotO=yaw;camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
        if (stage==13 && reload==null) {reload=mc.reloadResourcePacks();return;}
        if (reload!=null && !reload.isDone()) return;
        if (reload!=null) reload.join();
        if (stageStart==0) stageStart=System.nanoTime();
        double seconds=(System.nanoTime()-stageStart)/1e9;
        if (stage==6 && seconds>=1+motionFrame*.1 && motionFrame<15) capture(mc,"engine-motion-%02d".formatted(motionFrame++));
        if (seconds<(stage==6?3:2)) return;
        check(mc.level.getBlockState(POS).equals(stageState()), "Stage state failed to synchronize: "+stage);
        if (stage==9 || stage==11 || stage==12) {
            check(mc.level.getBrightness(LightLayer.BLOCK,POS)==15,"Lamp emission");
            Direction exposed=switch(stageState().getValue(ShipLightBlock.FACE)) {
                case WALL -> Direction.NORTH;case FLOOR -> Direction.UP;case CEILING -> Direction.DOWN;
            };
            check(mc.level.getBrightness(LightLayer.BLOCK,POS.relative(exposed))>=14,"Lamp failed to light neighbouring air");
        }
        if (stage==13) checkModels(mc);
        capture(mc,stageName());
        if (++stage==14) {
            REPORT.add("Client: all blockstates and item models, shared materials, 4-frame engine animation, lamp model/shape orientation, actual surrounding light and resource reload.");
            Files.writeString(mc.gameDirectory.toPath().resolve("machinery-smoke.txt"),String.join("\n",REPORT)+"\n");
            done=true;mc.setCameraEntity(mc.player);mc.stop();
        }
        stageStart=0;stageSetup=null;
    }

    private static void checkMachines(ServerLevel level,ServerPlayer player) {
        for(int i=0;i<IDS.length-1;i++) {
            var block=block(IDS[i]);
            for(var state:block.getStateDefinition().getPossibleStates()) {
                var shape=state.getShape(level,POS);
                check(!state.canOcclude(),"Machine occludes neighbour: "+IDS[i]);
                check(shape.min(Direction.Axis.X)>=0 && shape.max(Direction.Axis.X)<=1
                        && shape.min(Direction.Axis.Y)>=0 && shape.max(Direction.Axis.Y)<=1
                        && shape.min(Direction.Axis.Z)>=0 && shape.max(Direction.Axis.Z)<=1,"Machine shape bounds: "+IDS[i]);
            }
            level.setBlockAndUpdate(POS,block.defaultBlockState());
            if(i<MENUS.length) {
                block.defaultBlockState().useWithoutItem(level,player,new BlockHitResult(POS.getCenter(),Direction.NORTH,POS,false));
                check(player.containerMenu.getClass().getSimpleName().equals(MENUS[i]),"Menu changed: "+IDS[i]);
                check(player.containerMenu.stillValid(player),"Invalid menu: "+IDS[i]);
                player.closeContainer();
            }
            level.setBlockAndUpdate(POS,Blocks.AIR.defaultBlockState());
        }
        var bench=ModBlocks.MATTER_MANIPULATOR_WORKBENCH.get().defaultBlockState().getCollisionShape(level,POS);
        check(bench.toAabbs().stream().noneMatch(b->b.contains(.5,.25,.25)),"Workbench lost front leg clearance");
        check(ModBlocks.BEACON_EMITTER.get().defaultBlockState().getLightEmission(level,POS)==0,"Beacon changed world lighting");
    }

    private static void checkLamp(ServerLevel level,ServerPlayer player) {
        var lamp=ModBlocks.SHIP_LIGHT.get();
        var pos=POS.offset(4,1,0);
        for(var state:lamp.getStateDefinition().getPossibleStates()) {
            Direction support=switch(state.getValue(ShipLightBlock.FACE)) {
                case FLOOR -> Direction.DOWN;case CEILING -> Direction.UP;
                case WALL -> state.getValue(ShipLightBlock.FACING).getOpposite();
            };
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            check(!state.canSurvive(level,pos),"Floating lamp survived");
            level.setBlockAndUpdate(pos.relative(support),Blocks.STONE.defaultBlockState());
            check(state.canSurvive(level,pos),"Supported lamp cannot survive");
            var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(lamp),
                    new BlockHitResult(pos.relative(support).getCenter(),support.getOpposite(),pos.relative(support),false));
            var placed=lamp.getStateForPlacement(context);
            check(placed!=null && placed.getValue(ShipLightBlock.FACE)==state.getValue(ShipLightBlock.FACE),"Lamp placement attachment");
            if(support.getAxis().isHorizontal())check(placed.getValue(ShipLightBlock.FACING)==support.getOpposite(),"Wall lamp facing");
            check(state.rotate(Rotation.CLOCKWISE_90).getValue(ShipLightBlock.FACING)==state.getValue(ShipLightBlock.FACING).getClockWise(),"Lamp rotation");
            level.setBlockAndUpdate(pos,state);
            var drops=Block.getDrops(state,level,pos,null);
            check(drops.size()==1 && drops.getFirst().is(lamp.asItem()) && drops.getFirst().getCount()==1,"Lamp self drop");
            level.setBlockAndUpdate(pos.relative(support),Blocks.AIR.defaultBlockState());
            check(level.getBlockState(pos).isAir(),"Unsupported lamp failed to detach");
        }
    }

    private static void checkDoor(ServerLevel level) {
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            var closed=ModBlocks.SHIP_DOOR.get().defaultBlockState().setValue(Stage2Blocks.ShipDoor.FACING,facing);
            for(int y=0;y<3;y++)level.setBlockAndUpdate(POS.above(y),closed);
            Stage2Blocks.ShipDoor.setOpen(level,POS.above(),true);
            for(int y=0;y<3;y++) {
                var state=level.getBlockState(POS.above(y));
                check(state.getValue(Stage2Blocks.ShipDoor.OPEN),"Stacked door did not open");
                check(state.getCollisionShape(level,POS.above(y)).toAabbs().stream().noneMatch(b->b.contains(.5,.5,.5)),"Open door blocks passage");
            }
            Stage2Blocks.ShipDoor.setOpen(level,POS.above(),false);
            check(level.getBlockState(POS).getCollisionShape(level,POS).toAabbs().stream().anyMatch(b->b.contains(.5,.5,.5)),"Closed door missing collision");
            for(int y=0;y<3;y++)level.setBlockAndUpdate(POS.above(y),Blocks.AIR.defaultBlockState());
        }
    }

    private static void checkModels(Minecraft mc) {
        for(String name:IDS) {
            var block=block(name);
            for(var state:block.getStateDefinition().getPossibleStates()) {
                var baked=mc.getBlockRenderer().getBlockModel(state);
                check(baked!=mc.getModelManager().getMissingModel(),"Missing model: "+state);
                List<BakedQuad> quads=baked.getQuads(state,null,RandomSource.create(0));
                check(quads.size()>100,"Incomplete mesh: "+state);
                for(var q:quads)check(!q.getSprite().contents().name().getPath().contains("missing"),"Missing texture: "+state);
                if(name.equals("ship_light")) {
                    var bounds=state.getShape(mc.level,POS).bounds();
                    for(var q:quads) {
                        var v=q.getVertices();int stride=v.length/4;
                        for(int i=0;i<4;i++) {
                            float x=Float.intBitsToFloat(v[i*stride]),y=Float.intBitsToFloat(v[i*stride+1]),z=Float.intBitsToFloat(v[i*stride+2]);
                            check(x>=bounds.minX-.001 && x<=bounds.maxX+.001 && y>=bounds.minY-.001 && y<=bounds.maxY+.001
                                    && z>=bounds.minZ-.001 && z<=bounds.maxZ+.001,"Lamp model and collision rotation differ: "+state);
                        }
                    }
                }
            }
            check(mc.getItemRenderer().getModel(new ItemStack(block),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Missing item: "+name);
        }
        for(String texture:new String[]{"shipboard_surfaces","shipboard_signals","ship_engine_unit_core"}) {
            var key=id("block/"+texture);var sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(key);
            check(sprite.contents().name().equals(key),"Missing atlas material "+texture);
            if(texture.equals("ship_engine_unit_core"))check(sprite.contents().getUniqueFrames().count()==4,"Engine animation lost frames");
        }
    }
    private static BlockState stageState() {
        if(stage<10)return block(IDS[stage]).defaultBlockState();
        if(stage==10)return ModBlocks.SHIP_DOOR.get().defaultBlockState().setValue(Stage2Blocks.ShipDoor.OPEN,true);
        if(stage==11 || stage==12)return ModBlocks.SHIP_LIGHT.get().defaultBlockState()
                .setValue(ShipLightBlock.FACE,stage==11?AttachFace.FLOOR:AttachFace.CEILING);
        return ModBlocks.SHIP_ENGINE_UNIT.get().defaultBlockState();
    }
    private static String stageName() {return stage<10?IDS[stage]:switch(stage){case 10->"ship_door_open";case 11->"ship_light_floor";case 12->"ship_light_ceiling";default->"resource_reload";};}
    private static Block block(String name) {return BuiltInRegistries.BLOCK.get(id(name));}
    private static ResourceLocation id(String name) {return ResourceLocation.fromNamespaceAndPath("starboundmc",name);}
    private static void capture(Minecraft mc,String name) throws Exception {
        var directory=mc.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(directory);
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(name+".png"));}
    }
    private static void check(boolean condition,String message) {if(!condition)throw new IllegalStateException(message);}
}
