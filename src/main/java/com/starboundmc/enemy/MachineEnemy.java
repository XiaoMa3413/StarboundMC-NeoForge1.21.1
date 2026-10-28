package com.starboundmc.enemy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/** Server-owned combat: acquire, track, commit, strike, recover and reposition. */
public final class MachineEnemy extends Monster {
    public static final int IDLE = 0, WINDUP = 1, ACTIVE = 2, RECOVERY = 3;
    public static final int POUNCE = 0, SHOT = 1, DRILL = 2, SWEEP = 3, STOMP = 4, BASH = 5, CHARGE = 6;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(MachineEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MachineEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(MachineEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEQUENCE = SynchedEntityData.defineId(MachineEnemy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHIELD = SynchedEntityData.defineId(MachineEnemy.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossBar;
    private final EnemyCombatMovement movement;
    private int remaining, cooldown = 12, phaseStarted, chargeCooldown, lastAttack = -1, followUp = -1;
    private boolean struck;
    private float lockedYaw;
    private Vec3 aim = Vec3.ZERO, attackDirection = Vec3.ZERO, previousTargetPosition;
    private LivingEntity attackTarget, observedTarget;
    private Vec3 targetVelocity = Vec3.ZERO;

    public MachineEnemy(EntityType<? extends MachineEnemy> type, Level level) {
        super(type, level);
        xpReward = profile().experience;
        bossBar = profile().boss() ? new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS) : null;
        movement = new EnemyCombatMovement(this);
        if (profile().flying()) {
            setNoGravity(true);
            moveControl = new EnemyFlightControl(this);
            navigation = new FlyingPathNavigation(this, level);
        } else {
            moveControl = new MoveControl(this) {
                @Override public void tick() {
                    if (committed()) { setSpeed(0); setZza(0); setXxa(0); }
                    else super.tick();
                }
            };
        }
        setPersistenceRequired();
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        if (profile().flying()) setPos(getX(), getY() + .8, getZ());
        return result;
    }

    public EnemyProfile profile() { return EnemyProfile.of(getType()); }
    public int phase() { return entityData.get(PHASE); }
    public int action() { return entityData.get(ACTION); }
    public boolean shieldRaised() { return entityData.get(SHIELD); }
    public boolean committed() { return phase() == WINDUP || phase() == ACTIVE; }
    public float animationTicks(float partialTick) { return Math.max(0, tickCount - phaseStarted + partialTick); }
    public float animationProgress(float partialTick) {
        int duration = entityData.get(DURATION);
        return duration == 0 ? 0 : Mth.clamp(animationTicks(partialTick) / duration, 0, 1);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, IDLE); builder.define(ACTION, POUNCE); builder.define(DURATION, 0);
        builder.define(SEQUENCE, 0); builder.define(SHIELD, false);
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SEQUENCE.equals(key)) phaseStarted = tickCount;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .7) {
            @Override public boolean canUse() { return !profile().flying() && getTarget() == null && phase() == IDLE && super.canUse(); }
            @Override public boolean canContinueToUse() { return getTarget() == null && phase() == IDLE && super.canContinueToUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8) {
            @Override public boolean canUse() { return getTarget() == null && phase() == IDLE && super.canUse(); }
            @Override public boolean canContinueToUse() { return getTarget() == null && phase() == IDLE && super.canContinueToUse(); }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean canHarmTarget(LivingEntity victim) {
        return victim != this && victim.isAlive() && !victim.isSpectator() && victim.level() == level() && !isAlliedTo(victim)
                && !(victim instanceof MachineEnemy)
                && (!(victim instanceof Player player) || !player.isCreative() && !player.getAbilities().instabuild);
    }

    private void begin(int phase, int duration) {
        remaining = duration;
        entityData.set(DURATION, duration); entityData.set(PHASE, phase);
        entityData.set(SEQUENCE, entityData.get(SEQUENCE) + 1); phaseStarted = tickCount;
        updateShield();
    }

    private void updateShield() {
        entityData.set(SHIELD, profile() == EnemyProfile.BREACH_GUARD && getTarget() != null
                && (phase() == IDLE || phase() == WINDUP || phase() == ACTIVE && action() == BASH));
    }

    private boolean enraged() { return profile().boss() && getHealth() < getMaxHealth() * .5f; }
    private int windup() {
        return switch (action()) {
            case POUNCE -> 12; case SHOT -> 18; case DRILL -> 20; case BASH -> 12;
            case SWEEP -> enraged() ? 22 : 28; case STOMP -> enraged() ? 28 : 34; case CHARGE -> enraged() ? 24 : 28;
            default -> throw new IllegalStateException("Unknown enemy attack");
        };
    }
    private int lockTicks() { return action() == CHARGE ? 10 : action() == SWEEP || action() == STOMP || action() == DRILL ? 8 : 6; }
    private int activeDuration() {
        return switch (action()) { case POUNCE -> 10; case SHOT -> 16; case BASH -> 8; case CHARGE -> 18; default -> 12; };
    }
    private int recovery() {
        return switch (action()) { case POUNCE -> 16; case SHOT -> 14; case BASH -> 18; case DRILL -> 24;
            case CHARGE -> 26; case SWEEP -> enraged() ? 24 : 30; case STOMP -> enraged() ? 32 : 40;
            default -> throw new IllegalStateException("Unknown enemy attack"); };
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
        if (!isAlive()) return;
        movement.tick();
        if (chargeCooldown > 0) chargeCooldown--;
        LivingEntity target = getTarget();
        if (target != null && !canHarmTarget(target)) { setTarget(null); target = null; }
        observe(target);
        updateShield();
        if (phase() != IDLE) {
            if (target == null || target != attackTarget) { cancelAttack(); return; }
            if (phase() == RECOVERY) tickRecovery(target);
            else tickAttack(target);
            return;
        }
        if (cooldown > 0) cooldown--;
        if (target == null) { if (profile().flying()) movement.idleHover(); return; }
        face(target.getEyePosition(), profile().boss() ? 8 : 16);
        int selected = selectAttack(target);
        if (cooldown == 0 && selected >= 0 && hasLineOfSight(target)) startAttack(selected, target, false);
        else if (profile().flying()) movement.fly(target);
        else movement.approach(target, profile() == EnemyProfile.SCRAP_SKITTER ? 1.2 : 1.12);
    }

    private void observe(LivingEntity target) {
        targetVelocity = Vec3.ZERO;
        if (target != null && target == observedTarget && previousTargetPosition != null) {
            Vec3 delta = target.position().subtract(previousTargetPosition);
            if (delta.lengthSqr() < 2.25) targetVelocity = delta.multiply(1, 0, 1);
        }
        previousTargetPosition = target == null ? null : target.position();
        observedTarget = target;
    }

    private int selectAttack(LivingEntity target) {
        double distance = horizontalDistance(target);
        if (!profile().flying() && Math.abs(target.getY() - getY()) > 1.8) return -1;
        return switch (profile()) {
            case SCRAP_SKITTER -> distance <= 4.6 ? POUNCE : -1;
            case COIL_SENTRY -> distance >= 4 && distance <= 18 && Math.abs(target.getY() - getY()) < 9 ? SHOT : -1;
            case BREACH_GUARD -> distance < 1.9 && lastAttack != BASH ? BASH : distance <= 4.6 ? DRILL : -1;
            case QUARRY_WARDEN -> {
                if (distance > 5.2 && distance <= 13 && chargeCooldown == 0) yield CHARGE;
                if (distance <= 4.5 && (lastAttack == SWEEP || !withinAngle(target, 65) || distance > 3.8 + target.getBbWidth() * .5)) yield STOMP;
                yield distance <= 3.8 + target.getBbWidth() * .5 ? SWEEP : -1;
            }
        };
    }

    private boolean followUpInRange(int attack, LivingEntity target) {
        return hasLineOfSight(target) && Math.abs(target.getY() - getY()) <= 1.8
                && horizontalDistance(target) <= (attack == STOMP ? 4.5 : 4.6);
    }

    private void startAttack(int attack, LivingEntity target, boolean chained) {
        entityData.set(ACTION, attack);
        attackTarget = target; struck = false; movement.stop(); movement.reset();
        followUp = !chained && (attack == BASH || attack == CHARGE || attack == SWEEP && enraged())
                ? attack == BASH ? DRILL : STOMP : -1;
        aim = predictedAim(target);
        face(aim, profile().boss() ? 12 : 24);
        lockedYaw = getYRot();
        setDeltaMovement(getDeltaMovement().multiply(.1, profile().flying() ? .1 : 1, .1));
        begin(WINDUP, windup());
        if (attack == CHARGE) chargeCooldown = 120;
        playSound(SoundEvents.NOTE_BLOCK_BIT.value(), .7f, profile().boss() ? .6f : 1.3f);
    }

    private Vec3 predictedAim(LivingEntity target) {
        double ticks = action() == SHOT ? Math.min(12, distanceTo(target) / .7 + lockTicks()) : 3;
        Vec3 lead = targetVelocity.scale(ticks);
        double cap = action() == SHOT ? 2.5 : .65;
        if (lead.length() > cap) lead = lead.normalize().scale(cap);
        return target.getEyePosition().add(lead);
    }

    private void tickAttack(LivingEntity target) {
        movement.stop();
        if (phase() == WINDUP) {
            if (remaining > lockTicks() && hasLineOfSight(target)) {
                aim = predictedAim(target);
                face(aim, profile().boss() ? 8 : 16);
                lockedYaw = getYRot();
            }
            holdFacing();
            if (tickCount % 4 == 0) telegraph();
            if (--remaining <= 0) {
                // Lost sight during the warning: reposition instead of firing at a wall.
                if (action() == SHOT && !hasLineOfSight(target)) { begin(RECOVERY, 10); return; }
                begin(ACTIVE, activeDuration());
                attackDirection = Vec3.directionFromRotation(0, lockedYaw);
                if (action() == POUNCE) setDeltaMovement(attackDirection.scale(.62).add(0, .24, 0));
                hasImpulse = true;
            }
            return;
        }
        holdFacing();
        int elapsed = entityData.get(DURATION) - remaining;
        if (action() == SHOT) {
            if ((elapsed == 0 || elapsed == 12) && hasLineOfSight(target)) fireBolt();
        } else if (action() == POUNCE || action() == DRILL || action() == CHARGE) {
            advanceStrike(target, elapsed);
        } else if (!struck && elapsed >= (action() == STOMP ? 5 : 3)) {
            impact(); struck = true;
        }
        if (--remaining <= 0) {
            lastAttack = action();
            begin(RECOVERY, recovery());
            setDeltaMovement(getDeltaMovement().multiply(.3, 1, .3));
        }
    }

    private void advanceStrike(LivingEntity target, int elapsed) {
        double reach = action() == POUNCE ? 1.4 : action() == DRILL ? 2.5 : 2.5;
        if (!struck && inFront(target, reach, action() == DRILL ? 45 : 65)) {
            if (elapsed >= (action() == DRILL ? 3 : 0)) {
                strike(target, action() == POUNCE ? 4 : 8, action() == CHARGE ? 1.0 : .4);
                struck = true;
            }
        }
        boolean advance = !struck && (action() == CHARGE || elapsed < (action() == POUNCE ? 6 : 7));
        if (advance && !horizontalCollision) {
            double speed = action() == POUNCE ? .62 : action() == DRILL ? .32 : .46;
            Vec3 next = attackDirection.scale(speed);
            // Do not drive off a ledge during a committed ground rush.
            if (action() != POUNCE && !level().getBlockState(blockPosition().offset((int)Math.signum(next.x), -1, (int)Math.signum(next.z))).blocksMotion()) return;
            setDeltaMovement(next.x, getDeltaMovement().y, next.z); hasImpulse = true;
        } else if (action() == CHARGE) {
            remaining = Math.min(remaining, 2);
            setDeltaMovement(getDeltaMovement().multiply(.25, 1, .25));
        }
    }

    private void tickRecovery(LivingEntity target) {
        int elapsed = entityData.get(DURATION) - remaining;
        face(target.getEyePosition(), profile().boss() ? 6 : 12);
        if (elapsed < (profile().flying() ? 3 : 8)) movement.stop();
        else if (profile().flying()) movement.fly(target);
        else if (profile() == EnemyProfile.SCRAP_SKITTER) movement.skitterRecover(target);
        else if (horizontalDistance(target) > (profile().boss() ? 3.8 : 2.8)) movement.approach(target, .75);
        else movement.stop();
        if (--remaining <= 0) {
            int next = followUp; followUp = -1;
            if (next >= 0 && followUpInRange(next, target)) startAttack(next, target, true);
            else { begin(IDLE, 0); cooldown = profile().boss() ? 10 : 6; attackTarget = null; movement.reset(); }
        }
    }

    private void cancelAttack() {
        followUp = -1; attackTarget = null; movement.stop(); movement.reset();
        begin(IDLE, 0); cooldown = 8;
        setDeltaMovement(getDeltaMovement().multiply(.2, 1, .2));
    }

    private void face(Vec3 point, float maxTurn) {
        Vec3 delta = point.subtract(getEyePosition());
        float desired = (float)(Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90;
        setYRot(Mth.approachDegrees(getYRot(), desired, maxTurn));
        yBodyRot = getYRot(); yHeadRot = getYRot();
        getLookControl().setLookAt(point.x, point.y, point.z, maxTurn, 30);
    }

    private void holdFacing() {
        setYRot(lockedYaw); yBodyRot = lockedYaw; yHeadRot = lockedYaw;
        Vec3 forward = Vec3.directionFromRotation(0, lockedYaw);
        getLookControl().setLookAt(getX() + forward.x * 8, aim.y, getZ() + forward.z * 8, 30, 30);
    }

    private double horizontalDistance(LivingEntity target) { return target.position().subtract(position()).horizontalDistance(); }
    private boolean withinAngle(LivingEntity victim, double halfAngle) {
        Vec3 delta = victim.position().subtract(position()).multiply(1, 0, 1);
        return delta.lengthSqr() < .001 || Vec3.directionFromRotation(0, getYRot()).dot(delta.normalize()) >= Math.cos(halfAngle * Mth.DEG_TO_RAD);
    }
    private boolean inFront(LivingEntity victim, double reach, double halfAngle) {
        return canHarmTarget(victim) && hasLineOfSight(victim) && Math.abs(victim.getY() - getY()) <= 1.8
                && horizontalDistance(victim) <= reach + victim.getBbWidth() * .5 && withinAngle(victim, halfAngle);
    }
    private void strike(LivingEntity victim, float damage, double knockback) {
        if (victim.hurt(damageSources().mobAttack(this), damage)) {
            victim.knockback(knockback, getX() - victim.getX(), getZ() - victim.getZ());
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + .7, victim.getZ(), 6, .2, .2, .2, .05);
            playSound(SoundEvents.IRON_GOLEM_ATTACK, .7f, profile().boss() ? .65f : 1.2f);
        }
    }
    private void fireBolt() {
        if (!(level() instanceof ServerLevel server)) return;
        var bolt = new EnemyBolt(level(), this);
        bolt.setPos(getEyePosition().add(Vec3.directionFromRotation(0, lockedYaw).scale(.65)));
        Vec3 direction = aim.subtract(bolt.position()).normalize();
        bolt.shoot(direction.x, direction.y, direction.z, .7f, 0);
        server.addFreshEntity(bolt);
        playSound(SoundEvents.BLAZE_SHOOT, .65f, 1.8f);
    }
    private void impact() {
        if (!(level() instanceof ServerLevel server)) return;
        if (action() == BASH) {
            if (attackTarget != null && inFront(attackTarget, 2.0, 65)) strike(attackTarget, 4, 1.0);
            return;
        }
        double radius = action() == STOMP ? 4.5 : 3.8;
        for (var victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), this::canHarmTarget)) {
            if (action() == STOMP) {
                if (horizontalDistance(victim) <= radius && Math.abs(victim.getY() - getY()) <= 1.6 && hasLineOfSight(victim)) strike(victim, 10, .65);
            } else if (inFront(victim, radius, 80)) strike(victim, 8, .55);
        }
        if (action() == STOMP) server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + .15, getZ(), 2, 1, .1, 1, 0);
        else {
            Vec3 front = Vec3.directionFromRotation(0, lockedYaw).scale(2);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + front.x, getY() + 1, getZ() + front.z, 3, .5, .2, .5, 0);
        }
    }
    private void telegraph() {
        if (!(level() instanceof ServerLevel server)) return;
        if (action() == CHARGE) {
            Vec3 forward = Vec3.directionFromRotation(0, lockedYaw), side = new Vec3(-forward.z, 0, forward.x).scale(1.1);
            for (int i = 1; i <= 6; i++) for (int sign : new int[]{-1, 1}) {
                Vec3 p = position().add(forward.scale(i * 1.2)).add(side.scale(sign));
                server.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y + .1, p.z, 1, 0, 0, 0, 0);
            }
        } else if (action() == STOMP || action() == SWEEP) {
            for (int k = 0; k < 16; k++) {
                double offset = action() == STOMP ? k * Math.PI / 8 : (-80 + 160.0 * k / 15) * Mth.DEG_TO_RAD;
                double angle = lockedYaw * Mth.DEG_TO_RAD + offset;
                double radius = action() == STOMP ? 4.5 : 3.8;
                server.sendParticles(ParticleTypes.SMALL_FLAME, getX() - Math.sin(angle) * radius,
                        getY() + .08, getZ() + Math.cos(angle) * radius, 1, 0, 0, 0, 0);
            }
        } else server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY(), getZ(), 3, .25, .2, .25, .02);
    }

    @Override public boolean hurt(DamageSource source, float damage) {
        if (!level().isClientSide && profile().boss() && phase() != RECOVERY && source.getEntity() != null) damage *= .55f;
        if (!level().isClientSide && source.getEntity() != null && source.getSourcePosition() != null && shieldRaised()) {
            Vec3 incoming = source.getSourcePosition().subtract(position()).multiply(1, 0, 1);
            if (incoming.lengthSqr() > .001 && incoming.normalize().dot(Vec3.directionFromRotation(0, getYRot())) > .5) {
                damage *= .35f; playSound(SoundEvents.SHIELD_BLOCK, .7f, .7f);
            }
        }
        return super.hurt(source, damage);
    }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); if (bossBar != null) bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); if (bossBar != null) bossBar.removePlayer(player); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return !profile().flying() && super.causeFallDamage(distance, multiplier, source); }
}
