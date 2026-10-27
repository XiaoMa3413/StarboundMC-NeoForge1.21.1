package com.starboundmc.enemy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

/** Real registered eggs, server-world damage, collision and attack-state contracts. */
@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class EnemyGameTests {
    @GameTest(template="enemy_test_empty")
    public static void fourRealSpawnEggsCreatePersistentEnemiesWithAttributes(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var player=h.makeMockPlayer(GameType.CREATIVE);
            for(var profile:EnemyProfile.values()) {
                var stack=new ItemStack(ModEnemies.egg(profile));player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var floor=BlockPos.containing(f.base).below();
                var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
                h.assertTrue(result.consumesAction(),"Spawn egg rejected block interaction");
                var mobs=f.level.getEntitiesOfClass(MachineEnemy.class,new AABB(BlockPos.containing(f.base)).inflate(5),m->m.profile()==profile);
                h.assertTrue(mobs.size()==1,"Egg did not create exactly one "+profile.id);
                var enemy=mobs.getFirst();f.entities.add(enemy);
                h.assertTrue(enemy.getHealth()==profile.health && enemy.getArmorValue()==profile.armor,"Registered attributes missing");
                h.assertTrue(enemy.isPersistenceRequired(),"Egg enemy must persist for testing");
                if(profile.flying())h.assertTrue(enemy.getY()>f.base.y+.6,"Flying egg enemy did not spawn hovering");
                enemy.discard();
            }
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void noEnemyIsInAnyRegisteredBiomeSpawnList(GameTestHelper h) {
        var types=new HashSet<EntityType<?>>();for(var p:EnemyProfile.values())types.add(ModEnemies.type(p));
        for(var biome:h.getLevel().registryAccess().registryOrThrow(Registries.BIOME))
            for(var category:MobCategory.values())for(var entry:biome.getMobSettings().getMobs(category).unwrap())
                h.assertTrue(!types.contains(entry.type),"Enemy was added to a natural spawn list");
        h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void pounceTelegraphsAndDamagesOnlyOnce(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.SCRAP_SKITTER);var victim=f.target(0,1.2,40);enemy.setTarget(victim);
            f.stepToPhase(enemy,MachineEnemy.WINDUP);
            h.assertTrue(victim.getHealth()==40,"Pounce hit before telegraph");
            f.stepToPhase(enemy,MachineEnemy.ACTIVE);h.assertTrue(victim.getHealth()==40,"Pounce skipped windup");
            f.step(enemy,12);h.assertTrue(victim.getHealth()==36,"Pounce should hit once for 4");
            f.step(enemy,8);h.assertTrue(victim.getHealth()==36,"Recovery repeated the pounce hit");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void movingOutDuringWindupAvoidsPounce(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.SCRAP_SKITTER);var victim=f.target(0,1.2,40);enemy.setTarget(victim);
            f.stepToPhase(enemy,MachineEnemy.WINDUP);victim.setPos(f.base.add(0,0,8));
            f.step(enemy,30);h.assertTrue(victim.getHealth()==40,"Dodge did not avoid pounce");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void rangedEnemyUsesDelayedVisibleProjectile(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.COIL_SENTRY);var victim=f.target(0,10,40);enemy.setTarget(victim);
            f.stepToPhase(enemy,MachineEnemy.WINDUP);h.assertTrue(f.bolts(enemy).isEmpty(),"Shot had no charge warning");
            f.stepToPhase(enemy,MachineEnemy.ACTIVE);f.step(enemy,1);
            var bolts=f.bolts(enemy);h.assertTrue(bolts.size()==1 && victim.getHealth()==40,"Expected one travelling projectile, not instant damage");
            var bolt=bolts.getFirst();for(int i=0;i<30&&!bolt.isRemoved();i++)bolt.tick();
            h.assertTrue(victim.getHealth()==35 && bolt.isRemoved(),"Projectile did not hit exactly once for 5");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void wallsBlockBothChargeSightAndTravellingBolt(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.COIL_SENTRY);var victim=f.target(0,10,40);enemy.setTarget(victim);f.wall(5);
            f.step(enemy,100);h.assertTrue(f.bolts(enemy).isEmpty(),"Enemy charged through a wall");
            var bolt=new EnemyBolt(f.level,enemy);bolt.setPos(enemy.getEyePosition());
            var direction=victim.getEyePosition().subtract(bolt.position()).normalize();bolt.shoot(direction.x,direction.y,direction.z,.55f,0);f.level.addFreshEntity(bolt);
            for(int i=0;i<30&&!bolt.isRemoved();i++)bolt.tick();
            h.assertTrue(bolt.isRemoved()&&victim.getHealth()==40,"Bolt passed through a solid wall");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void guardShieldsFrontButAllowsFlanksAndRecovery(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.BREACH_GUARD);var front=f.target(0,2,100);var rear=f.target(0,-2,100);
            enemy.setTarget(front);f.step(enemy,1);h.assertTrue(enemy.shieldRaised(),"Guard did not raise shield");
            float start=enemy.getHealth();enemy.hurt(f.level.damageSources().mobAttack(front),10);float frontDamage=start-enemy.getHealth();
            enemy.invulnerableTime=0;start=enemy.getHealth();enemy.hurt(f.level.damageSources().mobAttack(rear),10);float rearDamage=start-enemy.getHealth();
            h.assertTrue(rearDamage>frontDamage*1.5f,"Shield also protected the flank");
            enemy.setTarget(front);f.stepToPhase(enemy,MachineEnemy.RECOVERY);f.step(enemy,1);
            h.assertTrue(!enemy.shieldRaised(),"Recovery kept shield protection");
            enemy.invulnerableTime=0;start=enemy.getHealth();enemy.hurt(f.level.damageSources().mobAttack(front),10);
            h.assertTrue(start-enemy.getHealth()>frontDamage*1.5f,"Recovery did not expose front");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void bossStompHasWarningSightOcclusionAndNoTerrainDamage(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.QUARRY_WARDEN);var victim=f.target(0,3.2,100);
            var screened=f.target(2.2,2,100);enemy.setTarget(victim);
            // Narrow wall shields only the second victim, leaving the primary target visible.
            for(int y=0;y<4;y++)f.level.setBlock(BlockPos.containing(f.base.add(1,y,1)),Blocks.BEDROCK.defaultBlockState(),3);
            int limit=220;
            while((enemy.phase()!=MachineEnemy.WINDUP||enemy.action()!=MachineEnemy.STOMP)&&limit-->0)f.step(enemy,1);
            h.assertTrue(limit>0,"Boss never selected its ground slam");float before=victim.getHealth(),protectedBefore=screened.getHealth();
            f.stepToPhase(enemy,MachineEnemy.ACTIVE);h.assertTrue(victim.getHealth()==before,"Stomp hit before visible stroke");
            f.step(enemy,7);
            h.assertTrue(victim.getHealth()==before-10,"Ground slam damage missing: before="+before+" after="+victim.getHealth());
            h.assertTrue(screened.getHealth()==protectedBefore,"Slam damaged through a wall");
            h.assertTrue(f.level.getBlockState(BlockPos.containing(f.base.add(1,1,1))).is(Blocks.BEDROCK),"Slam destroyed terrain");
        }h.succeed();
    }
    @GameTest(template="enemy_test_empty")
    public static void deadEnemyCannotFinishAttackAndSavedTypeKeepsHealth(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var enemy=f.enemy(EnemyProfile.BREACH_GUARD);var victim=f.target(0,2,40);enemy.setTarget(victim);
            enemy.setHealth(123);var tag=new CompoundTag();enemy.save(tag);
            var loaded=EntityType.loadEntityRecursive(tag,f.level,e->e);
            h.assertTrue(loaded instanceof MachineEnemy m&&m.profile()==EnemyProfile.BREACH_GUARD&&m.getHealth()==123,"Saved enemy lost identity or health");
            f.stepToPhase(enemy,MachineEnemy.WINDUP);enemy.setHealth(0);f.step(enemy,50);
            h.assertTrue(victim.getHealth()==40,"Dead enemy completed its attack");
            var creative=h.makeMockPlayer(GameType.CREATIVE);h.assertTrue(!enemy.canHarmTarget(creative),"Creative player was a valid target");
        }h.succeed();
    }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;final Vec3 base;final List<LivingEntity> entities=new ArrayList<>();
        Fixture(GameTestHelper h) {
            level=h.getLevel();level.getServer().setDifficulty(Difficulty.NORMAL,true);var p=h.absolutePos(new BlockPos(16,5,10));base=Vec3.atBottomCenterOf(p);
            for(int x=-6;x<=6;x++)for(int z=-5;z<=16;z++)level.setBlock(p.offset(x,-1,z),Blocks.BEDROCK.defaultBlockState(),3);
        }
        MachineEnemy enemy(EnemyProfile profile) {
            var mob=Objects.requireNonNull(ModEnemies.type(profile).create(level));mob.setPos(base);mob.setYRot(0);mob.setNoAi(true);level.addFreshEntity(mob);entities.add(mob);return mob;
        }
        Villager target(double x,double z,float health) {
            var mob=Objects.requireNonNull(EntityType.VILLAGER.create(level));mob.setPos(base.add(x,0,z));mob.setNoAi(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth(health);level.addFreshEntity(mob);entities.add(mob);return mob;
        }
        void step(MachineEnemy enemy,int ticks) { for(int i=0;i<ticks;i++){enemy.tickCount++;for(var living:entities)if(living.invulnerableTime>0)living.invulnerableTime--;enemy.customServerAiStep();} }
        void stepToPhase(MachineEnemy enemy,int phase) {
            int limit=250;while(enemy.phase()!=phase&&limit-->0)step(enemy,1);
            if(limit<=0)throw new IllegalStateException("Enemy did not reach phase "+phase+" current="+enemy.phase());
        }
        List<EnemyBolt> bolts(MachineEnemy owner) { return level.getEntitiesOfClass(EnemyBolt.class,new AABB(BlockPos.containing(base)).inflate(22),b->b.getOwner()==owner); }
        void wall(int z) { for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)level.setBlock(BlockPos.containing(base.add(x,y,z)),Blocks.BEDROCK.defaultBlockState(),3); }
        @Override public void close(){for(var mob:entities){if(mob instanceof MachineEnemy enemy)for(var bolt:bolts(enemy))bolt.discard();mob.discard();}}
    }
}
