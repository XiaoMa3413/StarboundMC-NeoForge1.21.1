package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.TransporterBlock;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.*;
import com.starboundmc.warp.ShipStateData;
import com.starboundmc.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;

/** Opt-in, fresh-world verification using real placement, mining, menus, packets and rendering. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class TransporterRenderSmoke {
    private static final BlockPos BASE = new BlockPos(1000, 190, 1000), TARGET = BASE.east(4);
    private static final String[] NAMES = {"north", "east", "south", "west", "menu-lower", "menu-middle", "menu-upper",
            "core-offline", "named-arrival", "ship-arrival", "resource-reload", "inventory", "surface-arrival", "cross-dimension-return"};
    private static boolean started, done, sent;
    private static int stage;
    private static long start, settled, effectSeen;
    private static CompletableFuture<?> setup, stageSetup, verify, reload;
    private static ArmorStand camera;

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.transporterSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true; start = System.nanoTime();
            mc.options.pauseOnLostFocus = false; mc.options.hideGui = true;
            mc.options.renderDistance().set(4); mc.options.fov().set(35);
            String name = "transporter-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413,false,false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        check((System.nanoTime()-start)/1e9 < 300, "Timeout at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (setup == null) {
            if (mc.screen != null) return;
            setup = mc.getSingleplayerServer().submit(() -> serverChecks(mc));
            return;
        }
        if (!setup.isDone()) return;
        setup.join();
        if (stageSetup == null) {
            stageSetup = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer(); var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
                var player = server.getPlayerList().getPlayers().getFirst(); player.closeContainer();
                if (stage < 8 || stage == 10 || stage == 11) {
                    level.setBlockAndUpdate(BASE, ModBlocks.TELEPORTER.get().defaultBlockState().setValue(TransporterBlock.FACING, facing()));
                    check(TransporterBlock.ensureAssembly(level, BASE), "Assembly failed");
                    player.teleportTo(level,stage<4||stage==10?1006.5:1000.5,190.5625,998.6,0,0);
                    player.getAbilities().flying = true; player.onUpdateAbilities();
                }
                if (stage >= 4 && stage <= 7) open(player, level, BASE.above(Math.min(2,stage-4)));
                if (stage == 8) {
                    var state = ShipStateData.get(server); state.beginCoreReboot(0,1); state.finishCoreRebootIfDue(1);
                    check(com.starboundmc.story.ShipEnvironmentService.isCoreOnline(server), "Core not online");
                    open(player,level,BASE.above(2));
                }
                if (stage == 9) open(player,level,TARGET.above());
                if (stage == 11) { player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.TELEPORTER.get()));player.inventoryMenu.broadcastChanges(); }
                if (stage == 12) { check(Stage6TravelService.teleportToShip(player),"Surface fixture ship travel");open(player,level,ShipStructure.SHIP_TELEPORTER_POS); }
            });
            camera=null;return;
        }
        if (!stageSetup.isDone()) return;
        stageSetup.join();
        if(stage<12&&!mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL))return;
        if (stage >= 4 && stage <= 8 && !(stage == 8 && sent) && !(mc.screen instanceof TeleporterScreen)) return;
        if (stage == 9 && !sent && !(mc.screen instanceof TeleporterScreen)) return;
        if (camera == null || camera.level()!=mc.level) {
            BlockPos focus = stage==8?TARGET:stage==9||stage==12||stage==13?ShipStructure.SHIP_TELEPORTER_POS:BASE;
            double angle=(stage<4?stage:0)*Math.PI/2,dx=-2.4*Math.cos(angle)+4.6*Math.sin(angle),dz=-2.4*Math.sin(angle)-4.6*Math.cos(angle);
            if(stage==9||stage==13){dx=0;dz=3.2;}
            float yaw=(float)Math.toDegrees(Math.atan2(dx,-dz));
            camera=new ArmorStand(mc.level,focus.getX()+.5+dx,focus.getY()+.4,focus.getZ()+.5+dz);
            camera.moveTo(camera.getX(),camera.getY(),camera.getZ(),yaw,6);camera.setYHeadRot(yaw);
            camera.yHeadRotO=yaw;camera.yBodyRot=yaw;camera.yBodyRotO=yaw;camera.setOldPosAndRot();
            if(stage!=9 && stage!=11)mc.setCameraEntity(camera);else mc.setCameraEntity(mc.player);
            if(stage>=12)mc.setCameraEntity(mc.player);
        }
        if(stage==11&&!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) {
            mc.options.hideGui=false;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if (settled==0) settled=System.nanoTime();
        if ((System.nanoTime()-settled)/1e9<2) return;
        if (stage==7 && !sent) { ModNetwork.sendToServer(new TeleporterUsePacket("n|"+TeleporterManager.key(ShipDimensions.SHIP_LEVEL,TARGET)));sent=true;settled=System.nanoTime();return; }
        if (stage==8 && !sent) {
            ModNetwork.sendToServer(new TeleporterRenamePacket("Transporter Alpha"));
            ModNetwork.sendToServer(new TeleporterUsePacket("n|"+TeleporterManager.key(ShipDimensions.SHIP_LEVEL,TARGET)));
            mc.setScreen(null);sent=true;return;
        }
        if (stage==9 && !sent) { mc.setCameraEntity(mc.player);ModNetwork.sendToServer(new TeleporterUsePacket("ship"));mc.setScreen(null);sent=true;return; }
        if(stage==12&&!sent){ModNetwork.sendToServer(new TeleporterUsePacket("planet"));mc.setScreen(null);mc.options.hideGui=true;sent=true;return;}
        if(stage==13&&!sent){ModNetwork.sendToServer(new TeleportToShipPacket());sent=true;return;}
        if(stage==12&&mc.level.dimension()!=Level.OVERWORLD)return;
        if(stage==13&&mc.level.dimension()!=ShipDimensions.SHIP_LEVEL)return;
        if(stage==13)mc.setCameraEntity(camera);
        if (stage==8 || stage==9 || stage>=12) {
            var field=TransporterEffectRenderer.class.getDeclaredField("EFFECTS");field.setAccessible(true);
            boolean renderedArrival=false;
            for(Object effect:(ArrayDeque<?>)field.get(null)){
                var arrival=effect.getClass().getDeclaredField("arrival");arrival.setAccessible(true);
                var began=effect.getClass().getDeclaredField("start");began.setAccessible(true);
                long tick=began.getLong(effect);
                if(arrival.getBoolean(effect)&&tick>=0&&mc.level.getGameTime()-tick>=3)renderedArrival=true;
            }
            if(!renderedArrival){
                if((System.nanoTime()-settled)/1e9>16){
                    var pos=stage==8?TARGET:ShipStructure.SHIP_TELEPORTER_POS;
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(mc.gameDirectory.toPath().resolve("waiting-arrival.png"));}
                    throw new IllegalStateException("Arrival not rendered stage="+stage+" pos="+mc.player.position()+" state="+mc.level.getBlockState(pos)+" compiled="+mc.levelRenderer.isSectionCompiled(pos)+" top="+mc.levelRenderer.isSectionCompiled(pos.above(2))+" camera="+mc.getCameraEntity().position());
                }
                return;
            }
        }
        if(stage==10 && reload==null){reload=mc.reloadResourcePacks();return;}
        if(reload!=null&&!reload.isDone())return;
        if(reload!=null)reload.join();
        if(verify==null){verify=mc.getSingleplayerServer().submit(()->{
            var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();var level=player.serverLevel();
            if(stage==0){for(int p=0;p<3;p++)check(level.getBlockState(BASE.south(3).above(p)).is(ModBlocks.TELEPORTER.get()),"Legacy load completion failed");level.removeBlock(BASE.south(3),false);}
            if(stage==7)check(player.position().distanceToSqr(new Vec3(1000.5,190.5625,998.6))<.05,"Offline travel moved player");
            if(stage==8||stage==9||stage==13){BlockPos pos=stage==8?TARGET:ShipStructure.SHIP_TELEPORTER_POS;
                check(player.position().distanceToSqr(TransporterBlock.landingPosition(pos,level.getBlockState(pos)))<.002,"Wrong real deck landing");
                check(level.noCollision(player,player.getBoundingBox().deflate(.001)),"Passenger intersects station");
            }
            if(stage==8)check("Transporter Alpha".equals(TeleporterManager.getName(server,level.dimension(),BASE)),"Rename packet failed");
            if(stage==12)check(level.dimension()==Level.OVERWORLD,"Surface packet did not cross dimension");
        });return;}
        if(!verify.isDone())return;verify.join();
        if(stage<4||stage==10){
            for(int p=0;p<3;p++){
                var state=mc.level.getBlockState(BASE.above(p));check(state.getValue(TransporterBlock.PART)==p,"Part did not sync");
                var model=mc.getBlockRenderer().getBlockModel(state);check(model!=mc.getModelManager().getMissingModel(),"Missing part model");
                check(model.getQuads(state,null,RandomSource.create()).size()>200,"Incomplete OBJ part");
            }
            for(String texture:new String[]{"titanium","warm","graphite","rubber","light","cyan","instruments","deck"}){
                var id=ResourceLocation.parse("starboundmc:block/transporter_"+texture);
                check(mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(id).contents().name().equals(id),"Missing "+texture);
            }
            check(mc.getItemRenderer().getModel(new ItemStack(ModBlocks.TELEPORTER.get()),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Missing item");
        }
        var directory=mc.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(directory);
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(NAMES[stage]+".png"));}
        if(++stage==NAMES.length){
            Files.writeString(mc.gameDirectory.toPath().resolve("transporter-smoke.txt"),"PASS: 14 stages; four facings and part geometry; placement, blocked space, fluids and build limit; real survival/creative mining all parts; legacy chunk load completion; upper-part menus, core-offline refusal, live rename and travel packets, precise named and ship arrival, cross-dimension surface/ship travel, client arrival VFX after chunk readiness, textures, inventory item and resource reload.\n");
            done=true;mc.setCameraEntity(mc.player);mc.stop();
        }
        settled=0;effectSeen=0;sent=false;stageSetup=null;verify=null;
    }

    private static void serverChecks(Minecraft mc) {
        var server=mc.getSingleplayerServer();var level=server.getLevel(ShipDimensions.SHIP_LEVEL);var player=server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(level,1000.5,190.5625,998.6,0,0);
        for(int x=997;x<=1007;x++)for(int z=997;z<=1004;z++)for(int y=189;y<=194;y++)
            level.setBlockAndUpdate(new BlockPos(x,y,z),y==189?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(BASE.north(3).above(4),Blocks.SEA_LANTERN.defaultBlockState());
        var block=ModBlocks.TELEPORTER.get();var state=block.defaultBlockState().setValue(TransporterBlock.FACING,Direction.NORTH);
        BlockPos test=BASE.south(3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));
        for(Direction dir:Direction.Plane.HORIZONTAL){
            player.setYRot(dir.toYRot());
            var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(block),new BlockHitResult(test.getCenter(),Direction.UP,test,false));
            var placement=block.getStateForPlacement(context);check(placement!=null&&placement.getValue(TransporterBlock.FACING)==dir.getOpposite(),"Placement facing");
            level.setBlockAndUpdate(test,placement);block.setPlacedBy(level,test,placement,player,new ItemStack(block));
            for(int p=0;p<3;p++)check(level.getBlockState(test.above(p)).getValue(TransporterBlock.PART)==p,"Placement missing part");
            check(level.noCollision(player,player.getDimensions(Pose.STANDING).makeBoundingBox(TransporterBlock.landingPosition(test,placement)).deflate(.001)),"Facing headroom");
            check(placement.rotate(Rotation.CLOCKWISE_90).getValue(TransporterBlock.FACING)==dir.getOpposite().getClockWise(),"Rotate");
            check(placement.mirror(Mirror.LEFT_RIGHT).getValue(TransporterBlock.FACING)==Mirror.LEFT_RIGHT.mirror(dir.getOpposite()),"Mirror");
            level.destroyBlock(test,false);
        }
        for(var obstacle:new net.minecraft.world.level.block.state.BlockState[]{Blocks.STONE.defaultBlockState(),Blocks.WATER.defaultBlockState()}){
            level.setBlockAndUpdate(test.above(2),obstacle);level.setBlockAndUpdate(test,state);
            check(!TransporterBlock.ensureAssembly(level,test)&&level.getBlockState(test.above(2))==obstacle,"Overwrote obstruction");
            check(level.isEmptyBlock(test.above()),"Partial assembly after refusal");level.removeBlock(test,false);level.setBlockAndUpdate(test.above(2),Blocks.AIR.defaultBlockState());
        }
        for(GameType mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE})for(int part=0;part<3;part++){
            player.setGameMode(mode);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            level.setBlockAndUpdate(test,state);check(TransporterBlock.ensureAssembly(level,test),"Mining fixture blocked");
            check(player.hasCorrectToolForDrops(state,level,test),"Teleporter not harvestable by pickaxe");
            TeleporterManager.setName(server,level.dimension(),test,"Mining target");
            check(player.gameMode.destroyBlock(test.above(part)),"Mine part "+part);
            for(int p=0;p<3;p++)check(level.isEmptyBlock(test.above(p)),"Orphan after mining "+part);
            check(TeleporterManager.getName(server,level.dimension(),test)==null,"Stale registry after mining");
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(test).inflate(4));
            int count=drops.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();
            check(count==(mode==GameType.SURVIVAL?1:0),"Drop count "+count+" for "+mode+" part "+part);drops.forEach(ItemEntity::discard);
        }
        player.setGameMode(GameType.CREATIVE);
        level.setBlockAndUpdate(TARGET,state);TransporterBlock.ensureAssembly(level,TARGET);
        TeleporterManager.setName(server,level.dimension(),TARGET,"Transporter Beta");
        TeleporterManager.setName(server,level.dimension(),TARGET.above(),"Invalid upper");
        check(TeleporterManager.validEntries(server).stream().noneMatch(e->e.pos().equals(TARGET.above())),"Upper part accepted as anchor");
        level.setBlockAndUpdate(test,state);level.setBlockAndUpdate(test.above(2),Blocks.STONE.defaultBlockState());
        Vec3 previous=player.position();check(!TransporterBlock.teleportHere(player,level,test)&&player.position().equals(previous),"Blocked bay moved player");
        level.removeBlock(test,false);level.removeBlock(test.above(2),false);
        BlockPos ceiling=new BlockPos(1000,level.getMaxBuildHeight()-2,1000);
        var high=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(block),new BlockHitResult(ceiling.getCenter(),Direction.UP,ceiling,false));
        check(block.getStateForPlacement(high)==null,"Build height placement accepted");
        var chunk=level.getChunkAt(test);
        chunk.getSection(chunk.getSectionIndex(test.getY())).setBlockState(test.getX()&15,test.getY()&15,test.getZ()&15,state);
        TransporterAssemblyEvents.load(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk,false));
        var legacy=ShipStructure.SHIP_TELEPORTER_POS;level.getChunkAt(legacy);
        check(level.getBlockState(legacy).is(block),"Archived ship anchor missing");
        check(level.getBlockState(legacy).getValue(TransporterBlock.FACING)==Direction.SOUTH,"Legacy ship must open into cabin");
    }
    private static Direction facing(){return switch(stage){case 1->Direction.EAST;case 2->Direction.SOUTH;case 3->Direction.WEST;default->Direction.NORTH;};}
    private static void open(ServerPlayer player,ServerLevel level,BlockPos pos){
        level.getBlockState(pos).useWithoutItem(level,player,new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false));
        check(player.containerMenu instanceof TeleporterMenu menu&&menu.pos.equals(TransporterBlock.basePos(pos,level.getBlockState(pos)))&&menu.stillValid(player),"Menu anchor from part");
    }
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
