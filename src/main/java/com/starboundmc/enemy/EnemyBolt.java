package com.starboundmc.enemy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Visible, finite-lifetime projectile with vanilla server collision and wall occlusion. */
public final class EnemyBolt extends ThrowableProjectile {
    public EnemyBolt(EntityType<? extends EnemyBolt> type, Level level) { super(type,level); }
    public EnemyBolt(Level level, MachineEnemy owner) { super(ModEnemies.BOLT.get(),owner,level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected double getDefaultGravity() { return 0; }
    @Override protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof MachineEnemy) && super.canHitEntity(entity);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) level().addParticle(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),0,0,0);
        else if (tickCount>80) discard();
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity victim
                && getOwner() instanceof MachineEnemy owner && owner.canHarmTarget(victim))
            victim.hurt(damageSources().mobProjectile(this,owner),5);
    }
    @Override protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY(),getZ(),6,.12,.12,.12,.03);
            discard();
        }
    }
}
