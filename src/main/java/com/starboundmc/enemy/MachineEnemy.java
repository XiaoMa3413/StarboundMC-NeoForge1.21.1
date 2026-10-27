package com.starboundmc.enemy;

import java.util.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server attack state machine. Synched fields drive cosmetic bone animation only. */
public final class MachineEnemy extends Monster {
    public static final int IDLE=0, WINDUP=1, ACTIVE=2, RECOVERY=3;
    public static final int POUNCE=0, SHOT=1, DRILL=2, SWEEP=3, STOMP=4;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(MachineEnemy.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(MachineEnemy.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION=SynchedEntityData.defineId(MachineEnemy.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEQUENCE=SynchedEntityData.defineId(MachineEnemy.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHIELD=SynchedEntityData.defineId(MachineEnemy.class,EntityDataSerializers.BOOLEAN);
    private int remaining, cooldown=20, phaseStarted, attackNumber;
    private boolean struck;
    private float lockedYaw;
    private Vec3 aim=Vec3.ZERO;
    private final ServerBossEvent bossBar;

    public MachineEnemy(EntityType<? extends MachineEnemy> type, Level level) {
        super(type,level); xpReward=profile().experience;
        bossBar=profile().boss()?new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS):null;
        if (profile().flying()) {
            setNoGravity(true);moveControl=new FlyingMoveControl(this,20,true);
            navigation=new FlyingPathNavigation(this,level);
        }
        setPersistenceRequired();
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        var result=super.finalizeSpawn(level,difficulty,reason,data);
        if(profile().flying())setPos(getX(),getY()+.8,getZ());
        return result;
    }
    public EnemyProfile profile() { return EnemyProfile.of(getType()); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);builder.define(PHASE,IDLE);builder.define(ACTION,POUNCE);
        builder.define(DURATION,0);builder.define(SEQUENCE,0);builder.define(SHIELD,false);
    }
    public int phase() { return entityData.get(PHASE); }
    public int action() { return entityData.get(ACTION); }
    public boolean shieldRaised() { return entityData.get(SHIELD); }
    public float animationProgress(float partialTick) {
        int duration=entityData.get(DURATION);
        return duration==0?0:Mth.clamp((tickCount-phaseStarted+partialTick)/duration,0,1);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);if (SEQUENCE.equals(key)) phaseStarted=tickCount;
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.7) {
            @Override public boolean canUse() { return !profile().flying() && getTarget()==null && phase()==IDLE && super.canUse(); }
        });
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8) {
            @Override public boolean canUse() { return getTarget()==null && phase()==IDLE && super.canUse(); }
        });
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public boolean canHarmTarget(LivingEntity victim) {
        return victim!=this && victim.isAlive() && !victim.isSpectator() && !isAlliedTo(victim)
                && !(victim instanceof MachineEnemy) && (!(victim instanceof Player player) || !player.isCreative() && !player.getAbilities().instabuild);
    }
    private void begin(int phase,int duration) {
        remaining=duration;entityData.set(DURATION,duration);entityData.set(PHASE,phase);
        entityData.set(SEQUENCE,entityData.get(SEQUENCE)+1);phaseStarted=tickCount;
    }
    private boolean enraged() { return profile().boss() && getHealth()<getMaxHealth()*.5f; }
    private int windup() {
        return switch(action()) { case POUNCE -> 12;case SHOT -> 20;case DRILL -> 24;case SWEEP -> enraged()?24:30;case STOMP -> enraged()?28:36;default -> throw new IllegalStateException("Enemy action"); };
    }
    private int activeDuration() { return action()==POUNCE?12:action()==SHOT?5:10; }
    private int recovery() { return action()==STOMP?65:action()==SWEEP?45:action()==DRILL?28:action()==SHOT?20:22; }
    private int impactDelay() { return action()==SHOT?0:action()==STOMP?5:action()==SWEEP?3:2; }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossBar!=null) bossBar.setProgress(getHealth()/getMaxHealth());
        if (!isAlive()) return;
        var target=getTarget();
        if (target!=null && !canHarmTarget(target)) { setTarget(null);target=null; }
        entityData.set(SHIELD,profile()==EnemyProfile.BREACH_GUARD && target!=null && phase()!=RECOVERY);
        if (phase()!=IDLE) {
            getNavigation().stop();setYRot(lockedYaw);yBodyRot=lockedYaw;yHeadRot=lockedYaw;
            getLookControl().setLookAt(aim.x,aim.y,aim.z);
            if (target==null) { begin(IDLE,0);cooldown=20;return; }
            if (phase()==WINDUP && tickCount%4==0) telegraph();
            if (phase()==ACTIVE) {
                int elapsed=entityData.get(DURATION)-remaining;
                if (action()==POUNCE) {
                    if (!struck && inFront(target,1.65,80)) { strike(target,4);struck=true; }
                } else if (!struck && elapsed>=impactDelay()) { impact();struck=true; }
            }
            if (--remaining<=0) {
                if (phase()==WINDUP) {
                    begin(ACTIVE,activeDuration());struck=false;
                    if (action()==POUNCE) { var direction=aim.subtract(position()).multiply(1,0,1).normalize();setDeltaMovement(direction.scale(.42).add(0,.18,0));hasImpulse=true; }
                } else if (phase()==ACTIVE) begin(RECOVERY,recovery());
                else { begin(IDLE,0);cooldown=enraged()?8:16; }
            }
            return;
        }
        if (cooldown>0) cooldown--;
        if (target==null) {
            if (profile().flying() && tickCount%35==0)
                getMoveControl().setWantedPosition(getX()+(random.nextDouble()-.5)*3,getY()+(onGround()?.8:(random.nextDouble()-.5)*.5),getZ()+(random.nextDouble()-.5)*3,.7);
            return;
        }
        double distance=distanceTo(target);
        getLookControl().setLookAt(target,30,30);
        double reach=switch(profile()) { case SCRAP_SKITTER -> 3.2;case COIL_SENTRY -> 18;case BREACH_GUARD -> 3.1;case QUARRY_WARDEN -> 5.0; };
        boolean room=profile()!=EnemyProfile.COIL_SENTRY || distance>=6;
        if (cooldown==0 && distance<=reach && room && hasLineOfSight(target)) {
            getNavigation().stop();aim=target.getEyePosition();
            var delta=aim.subtract(getEyePosition());lockedYaw=(float)(Mth.atan2(delta.z,delta.x)*180/Math.PI)-90;
            setYRot(lockedYaw);yBodyRot=lockedYaw;yHeadRot=lockedYaw;
            entityData.set(ACTION,switch(profile()) { case SCRAP_SKITTER -> POUNCE;case COIL_SENTRY -> SHOT;case BREACH_GUARD -> DRILL;case QUARRY_WARDEN -> attackNumber++%2==0?SWEEP:STOMP; });
            setDeltaMovement(getDeltaMovement().multiply(.3,1,.3));begin(WINDUP,windup());
            playSound(SoundEvents.NOTE_BLOCK_BIT.value(),.7f,profile().boss()?.6f:1.3f);
        } else if (profile().flying()) {
            if (distance<7) {
                var away=position().subtract(target.position()).multiply(1,0,1).normalize();
                getMoveControl().setWantedPosition(getX()+away.x*4,target.getY()+1.0,getZ()+away.z*4,1);
            } else if (distance>12) getMoveControl().setWantedPosition(target.getX(),target.getY()+1.0,target.getZ(),1);
            else getNavigation().stop();
        } else getNavigation().moveTo(target,1);
    }
    private boolean inFront(LivingEntity victim,double reach,double halfAngle) {
        if (!canHarmTarget(victim) || !hasLineOfSight(victim) || distanceTo(victim)>reach+victim.getBbWidth()*.5) return false;
        var delta=victim.position().subtract(position()).multiply(1,0,1);
        if (delta.lengthSqr()<.001) return true;
        var forward=getLookAngle().multiply(1,0,1).normalize();
        return forward.dot(delta.normalize())>=Math.cos(halfAngle*Math.PI/180);
    }
    private void strike(LivingEntity victim,float damage) {
        if (victim.hurt(damageSources().mobAttack(this),damage)) victim.knockback(.4,getX()-victim.getX(),getZ()-victim.getZ());
    }
    private void impact() {
        if (!(level() instanceof ServerLevel server)) return;
        if (action()==SHOT) {
            var bolt=new EnemyBolt(level(),this);bolt.setPos(getEyePosition().add(getLookAngle().scale(.55)));
            var direction=aim.subtract(bolt.position()).normalize();bolt.shoot(direction.x,direction.y,direction.z,.55f,0);
            server.addFreshEntity(bolt);playSound(SoundEvents.BLAZE_SHOOT,.65f,1.8f);return;
        }
        if (action()==DRILL) { var victim=getTarget();if (victim!=null && inFront(victim,2.8,40)) strike(victim,8); }
        else {
            double radius=action()==STOMP?4.5:3.8;
            for (var victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius),
                    this::canHarmTarget)) {
                if (action()==STOMP) { if (canHarmTarget(victim) && distanceTo(victim)<=radius && hasLineOfSight(victim)) strike(victim,10); }
                else if (inFront(victim,radius,80)) strike(victim,8);
            }
            server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY()+.15,getZ(),2,radius*.3,.1,radius*.3,0);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+.4,getZ(),12,.7,.3,.7,.08);
        playSound(SoundEvents.IRON_GOLEM_ATTACK,1,profile().boss()?.65f:1);
    }
    private void telegraph() {
        if (!(level() instanceof ServerLevel server)) return;
        if (action()==STOMP || action()==SWEEP) {
            double radius=action()==STOMP?4.5:3.8;
            for (int k=0;k<16;k++) {
                double angle=k*Math.PI/8;
                server.sendParticles(ParticleTypes.SMALL_FLAME,getX()+Math.cos(angle)*radius,getY()+.08,getZ()+Math.sin(angle)*radius,1,0,0,0,0);
            }
        } else server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getEyeY(),getZ(),3,.25,.2,.25,.02);
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        if (!level().isClientSide && profile().boss() && phase()!=RECOVERY && source.getEntity()!=null) damage*=.55f;
        if (!level().isClientSide && source.getEntity()!=null && source.getSourcePosition()!=null && shieldRaised()) {
            var incoming=source.getSourcePosition().subtract(position()).multiply(1,0,1);
            var forward=getLookAngle().multiply(1,0,1).normalize();
            if (incoming.lengthSqr()>.001 && incoming.normalize().dot(forward)>.5) { damage*=.35f;playSound(SoundEvents.SHIELD_BLOCK,.7f,.7f); }
        }
        return super.hurt(source,damage);
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player);if (bossBar!=null)bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player);if (bossBar!=null)bossBar.removePlayer(player); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return !profile().flying() && super.causeFallDamage(distance,multiplier,source); }
}
