package com.starboundmc.enemy;

import net.minecraft.gametest.framework.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** Attack decisions plus real world ticks: navigation, collision and moving targets. */
@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class EnemyCombatGameTests {
    @GameTest(template="enemy_test_empty")
    public static void tracksEarlyButKeepsLastSixTicksDodgeable(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.SCRAP_SKITTER); var target = f.target(0, 3, 100);
            enemy.setTarget(target); f.stepToPhase(enemy, MachineEnemy.WINDUP);
            target.setPos(f.base.add(1.2, 0, 3)); f.step(enemy, 4);
            h.assertTrue(Math.abs(enemy.getYRot()) > 10, "Early warning failed to track the moving target");
            f.step(enemy, 3); float committedYaw = enemy.getYRot();
            target.setPos(f.base.add(-3, 0, 2)); f.step(enemy, 4);
            h.assertTrue(Math.abs(Mth.wrapDegrees(enemy.getYRot() - committedYaw)) < .01, "Late warning became an undodgeable turn");
            f.step(enemy, 12); h.assertTrue(target.getHealth() == 100, "Committed pounce hit a target outside its lane");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty")
    public static void guardBashesThenTelegraphsDrillAfterRecovery(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.BREACH_GUARD); var target = f.target(0, 1.5, 100);
            enemy.setTarget(target); f.stepToPhase(enemy, MachineEnemy.WINDUP);
            h.assertTrue(enemy.action() == MachineEnemy.BASH, "Guard did not choose its close-range shield bash");
            f.stepToPhase(enemy, MachineEnemy.RECOVERY);
            h.assertTrue(target.getHealth() == 96 && !enemy.shieldRaised(), "Shield bash damage/recovery opening incorrect");
            target.setPos(f.base.add(0, 0, 3.8)); f.stepToPhase(enemy, MachineEnemy.WINDUP);
            h.assertTrue(enemy.action() == MachineEnemy.DRILL && target.getHealth() == 96, "Drill follow-up skipped its warning");
            f.stepToPhase(enemy, MachineEnemy.ACTIVE);
            h.assertTrue(!enemy.shieldRaised(), "Guard kept front shield protection during committed drill");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty")
    public static void bossAvoidsOutOfReachSwingAndChoosesRush(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.QUARRY_WARDEN); var target = f.target(0, 4.9, 100);
            enemy.setTarget(target); f.step(enemy, 25);
            h.assertTrue(enemy.phase() == MachineEnemy.IDLE, "Boss swung beyond both melee attack ranges");
            target.setPos(f.base.add(0, 0, 10)); f.step(enemy, 1);
            h.assertTrue(enemy.phase() == MachineEnemy.WINDUP && enemy.action() == MachineEnemy.CHARGE, "Boss failed to select gap-closing rush");
            f.stepToPhase(enemy, MachineEnemy.RECOVERY); target.setPos(f.base.add(0, 0, 3.5));
            f.stepToPhase(enemy, MachineEnemy.WINDUP);
            h.assertTrue(enemy.action() == MachineEnemy.STOMP, "Rush did not lead to a separately telegraphed stomp");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty")
    public static void rangedSalvoHasTwoSpacedBolts(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.COIL_SENTRY); var target = f.target(0, 10, 100);
            enemy.setTarget(target); f.stepToPhase(enemy, MachineEnemy.ACTIVE);
            f.step(enemy, 1); h.assertTrue(f.bolts(enemy).size() == 1, "First salvo shot missing");
            f.step(enemy, 10); h.assertTrue(f.bolts(enemy).size() == 1, "Salvo shots have no readable interval");
            f.step(enemy, 2); h.assertTrue(f.bolts(enemy).size() == 2, "Second salvo shot missing");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty")
    public static void breakingSightCancelsRemainingSalvo(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.COIL_SENTRY); var target = f.target(0, 10, 100);
            enemy.setTarget(target); f.stepToPhase(enemy, MachineEnemy.ACTIVE); f.step(enemy, 1);
            f.wall(5); f.step(enemy, 14);
            h.assertTrue(f.bolts(enemy).size() == 1, "Second salvo shot ignored newly blocked sight");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty")
    public static void targetChangeCancelsCommittedAttack(GameTestHelper h) {
        try (var f = new EnemyGameTests.Fixture(h)) {
            var enemy = f.enemy(EnemyProfile.BREACH_GUARD); var first = f.target(0, 2, 100); var second = f.target(0, -2, 100);
            enemy.setTarget(first); f.stepToPhase(enemy, MachineEnemy.ACTIVE);
            enemy.setTarget(second); f.step(enemy, 1);
            h.assertTrue(enemy.phase() == MachineEnemy.IDLE && first.getHealth() == 100 && second.getHealth() == 100,
                    "Attack silently retargeted after committing");
        } h.succeed();
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=260)
    public static void liveSkitterChasesAndHitsMovingTarget(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.SCRAP_SKITTER); var target = f.target(0, 7, 100);
        target.setNoGravity(true); enemy.setTarget(target); enemy.setNoAi(false);
        h.onEachTick(() -> target.setPos(f.base.add(Math.sin(h.getTick() * .06) * 1.2, 0, 7)));
        h.runAfterDelay(220, () -> {
            try (f) {
                h.assertTrue(enemy.position().distanceTo(f.base) > 2, "Skitter never left its spawn position");
                h.assertTrue(target.getHealth() < 100, "Skitter could not hit a slowly strafing target in live simulation");
            } h.succeed();
        });
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=300)
    public static void liveDroneFindsFiringLaneAroundCover(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.COIL_SENTRY); var target = f.target(0, 10, 100);
        f.wall(5); enemy.setTarget(target); enemy.setNoAi(false);
        h.runAfterDelay(260, () -> {
            try (f) {
                h.assertTrue(enemy.position().distanceTo(f.base) > 2, "Drone stayed idle behind an obstruction");
                h.assertTrue(target.getHealth() < 100, "Drone failed to navigate to a visible firing lane");
            } h.succeed();
        });
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=130)
    public static void liveDroneRetreatsFromCloseTarget(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.COIL_SENTRY); var target = f.target(0, 2, 100);
        enemy.setTarget(target); enemy.setNoAi(false);
        h.runAfterDelay(100, () -> {
            try (f) {
                h.assertTrue(enemy.distanceTo(target) > 5, "Drone could not open space from a close target");
                Vec3 forward = Vec3.directionFromRotation(0, enemy.getYRot());
                Vec3 toTarget = target.position().subtract(enemy.position()).multiply(1, 0, 1).normalize();
                h.assertTrue(forward.dot(toTarget) > .6, "Flight controls turned the cannon away from the target");
            } h.succeed();
        });
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=180)
    public static void liveGuardDrillClosesGapAndHits(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.BREACH_GUARD); var target = f.target(0, 4.4, 100);
        enemy.setTarget(target); enemy.setNoAi(false);
        h.runAfterDelay(140, () -> {
            try (f) {
                h.assertTrue(enemy.position().distanceTo(f.base) > 1, "Guard never advanced during its drill attack");
                h.assertTrue(target.getHealth() < 100, "Guard drill could not connect in live simulation");
            } h.succeed();
        });
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=220)
    public static void liveBossRushClosesGapAndHits(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.QUARRY_WARDEN); var target = f.target(0, 11, 100);
        enemy.setTarget(target); enemy.setNoAi(false);
        h.runAfterDelay(180, () -> {
            try (f) {
                h.assertTrue(enemy.position().distanceTo(f.base) > 5, "Boss failed to close a distant target's gap");
                h.assertTrue(target.getHealth() < 100, "Boss rush and follow-up failed to connect");
            } h.succeed();
        });
    }

    @GameTest(template="enemy_test_empty", timeoutTicks=150)
    public static void liveRushStopsAtNewWallWithoutHittingThroughIt(GameTestHelper h) {
        var f = new EnemyGameTests.Fixture(h); var enemy = f.enemy(EnemyProfile.QUARRY_WARDEN); var target = f.target(0, 11, 100);
        enemy.setTarget(target); enemy.setNoAi(false); boolean[] wallPlaced = {false};
        h.onEachTick(() -> {
            if (!wallPlaced[0] && enemy.phase() == MachineEnemy.ACTIVE && enemy.action() == MachineEnemy.CHARGE) {
                f.wall(4); wallPlaced[0] = true;
            }
            if (wallPlaced[0] && enemy.phase() == MachineEnemy.RECOVERY) {
                try (f) {
                    h.assertTrue(enemy.getZ() < f.base.z + 3, "Rush tunneled through its newly obstructed lane");
                    h.assertTrue(target.getHealth() == 100, "Rush damaged a target behind a wall");
                } h.succeed();
            }
        });
    }
}
