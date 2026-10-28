package com.starboundmc.enemy;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/** Follow flying-navigation waypoints without turning the cannon towards a strafe destination. */
final class EnemyFlightControl extends MoveControl {
    private final MachineEnemy enemy;

    EnemyFlightControl(MachineEnemy enemy) {
        super(enemy);
        this.enemy = enemy;
    }

    @Override public void tick() {
        enemy.setNoGravity(true);
        enemy.setXxa(0);
        enemy.setYya(0);
        enemy.setZza(0);
        enemy.setSpeed(0);
        Vec3 desired = Vec3.ZERO;
        if (operation == Operation.MOVE_TO && !enemy.committed()) {
            Vec3 delta = new Vec3(wantedX, wantedY, wantedZ).subtract(enemy.position());
            if (delta.lengthSqr() > .04) desired = delta.normalize().scale(Math.min(delta.length() * .35, .28 * speedModifier));
        }
        operation = Operation.WAIT;
        enemy.setDeltaMovement(enemy.getDeltaMovement().lerp(desired, desired.lengthSqr() == 0 ? .45 : .25));
    }
}
