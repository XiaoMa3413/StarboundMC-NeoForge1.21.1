package com.starboundmc.enemy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded replanning and tactical destinations; navigation owns obstacle avoidance. */
final class EnemyCombatMovement {
    private final MachineEnemy enemy;
    private int repath, stalled, side = 1;
    private Vec3 sampledPosition;

    EnemyCombatMovement(MachineEnemy enemy) { this.enemy = enemy; }

    void tick() {
        if (repath > 0) repath--;
    }

    void reset() { repath = 0; stalled = 0; sampledPosition = null; }

    void stop() {
        enemy.getNavigation().stop();
        enemy.getMoveControl().setWantedPosition(enemy.getX(), enemy.getY(), enemy.getZ(), 0);
    }

    void approach(LivingEntity target, double speed) {
        route(target.position(), speed);
    }

    void skitterRecover(LivingEntity target) {
        if (enemy.distanceTo(target) > 5) { approach(target, 1.2); return; }
        Vec3 away = away(target);
        route(target.position().add(away.scale(2.6)).add(tangent(away).scale(side * 1.5)), 1.1);
    }

    void fly(LivingEntity target) {
        if (repath > 0) return;
        Vec3 away = away(target);
        double radius = enemy.distanceTo(target) < 6 ? 10 : 9;
        Vec3 candidate = target.position().add(away.scale(radius)).add(tangent(away).scale(side * 3)).add(0, 1.0, 0);
        // Try both sides of the firing lane. If neither sees the target, follow a path around the cover.
        for (int attempt = 0; attempt < 2; attempt++) {
            Vec3 alternative = target.position().add(away.scale(radius)).add(tangent(away).scale(side * 4.5)).add(0, 1.4, 0);
            if (clearDestination(alternative) && seesTarget(alternative, target)) { candidate = alternative; break; }
            side = -side;
        }
        if (!clearDestination(candidate)) candidate = target.position().add(0, 1.6, 0);
        route(candidate, 1.0);
    }

    void idleHover() {
        if (repath > 0 || enemy.tickCount % 45 != 0) return;
        Vec3 destination = enemy.position().add((enemy.getRandom().nextDouble() - .5) * 3,
                enemy.onGround() ? .9 : (enemy.getRandom().nextDouble() - .5) * .4,
                (enemy.getRandom().nextDouble() - .5) * 3);
        if (clearDestination(destination)) route(destination, .55);
    }

    private Vec3 away(LivingEntity target) {
        Vec3 delta = enemy.position().subtract(target.position()).multiply(1, 0, 1);
        return delta.lengthSqr() < .01 ? new Vec3(0, 0, 1) : delta.normalize();
    }

    private static Vec3 tangent(Vec3 vector) { return new Vec3(-vector.z, 0, vector.x); }

    private boolean clearDestination(Vec3 destination) {
        return enemy.level().noCollision(enemy, enemy.getBoundingBox().move(destination.subtract(enemy.position())));
    }

    private boolean seesTarget(Vec3 destination, LivingEntity target) {
        return enemy.level().clip(new ClipContext(destination.add(0, enemy.getEyeHeight(), 0), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, enemy)).getType() == HitResult.Type.MISS;
    }

    private void route(Vec3 destination, double speed) {
        if (repath > 0) return;
        repath = 8 + enemy.getId() % 3;
        if (sampledPosition != null && enemy.position().distanceToSqr(sampledPosition) < .025) stalled++;
        else stalled = 0;
        sampledPosition = enemy.position();
        if (stalled >= 3) {
            side = -side;
            stalled = 0;
            // Discard the cached partial route; force a new search after reaching an obstruction.
            enemy.getNavigation().stop();
        }
        boolean found = enemy.getNavigation().moveTo(destination.x, destination.y, destination.z, 0, speed);
        if (!found) { side = -side; repath = 12; }
    }
}
