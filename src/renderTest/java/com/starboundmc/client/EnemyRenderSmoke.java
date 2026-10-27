package com.starboundmc.client;

import com.starboundmc.enemy.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Opt-in real spawn-egg / model / live movement / frozen windup evidence. */
@EventBusSubscriber(modid="starboundmc",value=Dist.CLIENT)
public final class EnemyRenderSmoke {
    private static int stage,enemyId;
    private static MachineEnemy serverEnemy;
    private static Villager target;
    private static CompletableFuture<?> work,language;
    private static long started,configured;
    private static boolean finished,reloaded,frozen,worldCapture,arenaPrepared,arenaReady;
    private static CompletableFuture<Boolean> arenaCheck;
    private static EnemyProfile profile(){return EnemyProfile.values()[Math.min(stage/3,3)];}
    private static int mode(){return stage==12?2:stage%3;}
    private static boolean enabled(){return Boolean.getBoolean("starboundmc.debug.enemySmoke");}
    private static String label(){return stage==12?"quarry_warden-stomp-windup":profile().id+"-"+new String[]{"idle","movement","windup"}[mode()];}
    private static MachineEnemy mob(){var e=Minecraft.getInstance().level.getEntity(enemyId);return e instanceof MachineEnemy m?m:null;}

    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(!enabled()||finished||mc.player==null||configured==0||worldCapture)return;
        if(work!=null&&!work.isDone())return;
        var enemy=mob();if(enemy==null)return;var g=event.getGuiGraphics();
        g.fill(0,0,g.guiWidth(),g.guiHeight(),0xff17212c);
        g.drawString(mc.font,"ENEMY / "+label()+" / PHASE="+enemy.phase(),24,20,0xffdce9ee);
        int scale=(int)Math.min(225/profile().height,235/profile().width);
        renderView(g,enemy,155,265,scale,0);renderView(g,enemy,395,265,scale,90);renderView(g,enemy,635,265,scale,180);
        g.drawString(mc.font,"FRONT",130,75,0xff8eb7c1);g.drawString(mc.font,"SIDE",375,75,0xff8eb7c1);g.drawString(mc.font,"BACK",615,75,0xff8eb7c1);
        int i=0;for(var p:EnemyProfile.values())g.renderItem(new ItemStack(ModEnemies.egg(p)),790+i++*30,75);
        g.renderTooltip(mc.font,new ItemStack(ModEnemies.egg(profile())),780,120);
        g.drawString(mc.font,"HP="+enemy.getHealth()+" / "+enemy.getMaxHealth(),770,200,0xffa8c3ce);
        g.drawString(mc.font,"ARMOR="+enemy.getArmorValue(),770,220,0xffa8c3ce);
        g.drawString(mc.font,"Egg created entity #"+enemyId,770,240,0xffa8c3ce);
        g.drawString(mc.font,mode()==2?"Actual windup; AI paused for inspection":"Registered native bone model",24,440,0xffa8c3ce);
    }
    private static void renderView(net.minecraft.client.gui.GuiGraphics g,MachineEnemy enemy,float x,float y,int scale,float angle){
        float body=enemy.yBodyRot,yaw=enemy.getYRot(),pitch=enemy.getXRot(),head=enemy.yHeadRot,old=enemy.yHeadRotO;
        enemy.yBodyRot=enemy.yHeadRot=enemy.yHeadRotO=180+angle;enemy.setYRot(180+angle);enemy.setXRot(0);
        InventoryScreen.renderEntityInInventory(g,x,y,scale,new org.joml.Vector3f(0,enemy.getBbHeight()/2,0),
                new org.joml.Quaternionf().rotateZ((float)Math.PI),new org.joml.Quaternionf(),enemy);
        enemy.yBodyRot=body;enemy.yHeadRot=head;enemy.yHeadRotO=old;enemy.setYRot(yaw);enemy.setXRot(pitch);
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event)throws Exception{
        if(!enabled()||finished)return;var mc=Minecraft.getInstance();var output=mc.gameDirectory.toPath().resolve("screenshots/enemies-v01");
        if(started==0){
            if(mc.screen==null||mc.getOverlay()!=null)return;
            if(language==null){mc.getLanguageManager().setSelected("zh_cn");mc.options.languageCode="zh_cn";language=mc.reloadResourcePacks();return;}
            if(!language.isDone())return;language.join();language=null;started=System.nanoTime();
            Files.createDirectories(output);Files.deleteIfExists(output.resolve("complete.txt"));
            mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            var name="enemy-fixture-"+System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(3413,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);return;
        }
        if((System.nanoTime()-started)/1e9>300)throw new IllegalStateException("Enemy smoke timeout stage="+stage);
        if(mc.player==null||mc.level==null||mc.screen!=null||mc.getOverlay()!=null)return;
        if(!arenaPrepared){
            work=mc.getSingleplayerServer().submit(()->{
                var level=mc.getSingleplayerServer().overworld();
                for(int z=62;z<=65;z++){level.setChunkForced(62,z,true);level.getChunk(62,z);}
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.teleportTo(level,1000.5,101,1000.5,Set.of(),0,0);
            });
            arenaPrepared=true;return;
        }
        if(!arenaReady){
            if(!work.isDone())return;work.join();
            if(arenaCheck!=null){if(!arenaCheck.isDone())return;arenaReady=arenaCheck.join();arenaCheck=null;if(arenaReady)return;}
            arenaCheck=mc.getSingleplayerServer().submit(()->{
                var level=mc.getSingleplayerServer().overworld();
                for(int z=62;z<=65;z++)if(!level.areEntitiesLoaded(ChunkPos.asLong(62,z)))return false;
                return level.isPositionEntityTicking(new BlockPos(1000,101,1006));
            });return;
        }
        if(stage==6&&!reloaded&&configured==0){
            if(language==null)language=mc.reloadResourcePacks();if(!language.isDone())return;language.join();language=null;reloaded=true;
        }
        if(configured==0){
            var profile=profile();int mode=mode();
            work=mc.getSingleplayerServer().submit(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                server.setDifficulty(Difficulty.NORMAL,true);level.setDayTime(6000);
                player.teleportTo(level,1000.5,101,1000.5,Set.of(),0,0);player.setGameMode(GameType.CREATIVE);
                if(mode==0){
                    if(serverEnemy!=null)serverEnemy.discard();if(target!=null)target.discard();
                    for(int x=993;x<=1007;x++)for(int z=996;z<=1042;z++)level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.SMOOTH_STONE.defaultBlockState());
                    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModEnemies.egg(profile)));player.inventoryMenu.broadcastChanges();
                    var floor=new BlockPos(1000,100,1006);var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
                    if(!result.consumesAction())throw new IllegalStateException("Spawn egg rejected");
                    var found=level.getEntitiesOfClass(MachineEnemy.class,new AABB(floor).inflate(5),m->m.profile()==profile);
                    if(found.size()!=1)throw new IllegalStateException("Spawn egg count "+found.size());serverEnemy=found.getFirst();enemyId=serverEnemy.getId();
                    if(serverEnemy.getHealth()!=profile.health||serverEnemy.getArmorValue()!=profile.armor)throw new IllegalStateException("Enemy attributes mismatch");
                    serverEnemy.setNoAi(true);serverEnemy.setYRot(180);serverEnemy.yBodyRot=180;
                    target=EntityType.VILLAGER.create(level);target.setNoAi(true);target.setNoGravity(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
                    target.setPos(1000.5,101,1036.5);level.addFreshEntity(target);
                }else{
                    serverEnemy.setPos(1000.5,101,1006.5);serverEnemy.setDeltaMovement(Vec3.ZERO);serverEnemy.getNavigation().stop();
                    double distance=mode==1?30:profile==EnemyProfile.COIL_SENTRY?10:profile.boss()?3.2:2.2;
                    target.setPos(1000.5,101,1006.5+distance);serverEnemy.setTarget(target);serverEnemy.setNoAi(false);
                }
            });
            configured=System.nanoTime();frozen=false;return;
        }
        if(!work.isDone())return;work.join();if(!mc.level.dimension().equals(Level.OVERWORLD))return;
        var enemy=mob();if(enemy==null)return;
        if(mode()==2&&!frozen){
            if(enemy.phase()!=MachineEnemy.WINDUP||(stage==12&&enemy.action()!=MachineEnemy.STOMP))return;
            work=mc.getSingleplayerServer().submit(()->{if(serverEnemy.phase()!=MachineEnemy.WINDUP)throw new IllegalStateException("Lost windup before freeze");serverEnemy.setNoAi(true);serverEnemy.setDeltaMovement(Vec3.ZERO);});
            frozen=true;configured=System.nanoTime();return;
        }
        if((System.nanoTime()-configured)/1e9<1.8)return;
        if(mc.getItemRenderer().getModel(new ItemStack(ModEnemies.egg(profile())),mc.level,mc.player,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing egg model");
        if(!(mc.getEntityRenderDispatcher().getRenderer(enemy) instanceof com.starboundmc.client.enemy.MachineEnemyRenderer))throw new IllegalStateException("Custom enemy renderer not selected");
        if(mode()==1&&enemy.position().distanceTo(new Vec3(1000.5,101,1006.5))<.1)throw new IllegalStateException("Enemy did not actually move");
        if(mode()==2&&enemy.phase()!=MachineEnemy.WINDUP)throw new IllegalStateException("Windup not synchronized");
        if(worldCapture){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(profile().id+"-world.png"));}
            worldCapture=false;advance(output);return;
        }
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(label()+".png"));}
        Files.writeString(output.resolve(label()+".txt"),"entity="+enemyId+" hp="+enemy.getHealth()+" armor="+enemy.getArmorValue()+" phase="+enemy.phase()+" pos="+enemy.position());
        if(mode()==0){worldCapture=true;return;}advance(output);
    }
    private static void advance(Path output)throws Exception{
        if(++stage==13){Files.writeString(output.resolve("complete.txt"),"Four real spawn eggs, 13 idle/movement/windup triple-view scenes (both boss attacks), 4 in-world views, native attributes and resource reload verified. Windup snapshots pause real AI after its transition.\n");finished=true;Minecraft.getInstance().stop();}
        else configured=0;
    }
}
