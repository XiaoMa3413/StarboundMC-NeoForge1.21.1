package com.starboundmc.weapon;

import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/** Bounded server particle bursts are visible to nearby players, including the shooter at 64m. */
public final class WeaponEffects {
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(.15f, .65f, 1f), 1.4f);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(.65f, .18f, 1f), 1.6f);
    private static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(.1f, 1f, .65f), 1.3f);
    private static final DustParticleOptions TRAIL_BLUE = new DustParticleOptions(new Vector3f(.15f, .65f, 1f), .55f);
    private static final DustParticleOptions TRAIL_PURPLE = new DustParticleOptions(new Vector3f(.65f, .18f, 1f), .6f);
    private static final DustParticleOptions CHARGE_BLUE = new DustParticleOptions(new Vector3f(.15f, .65f, 1f), .25f);
    private static final DustParticleOptions CHARGE_PURPLE = new DustParticleOptions(new Vector3f(.65f, .18f, 1f), .3f);
    private static void point(ServerLevel level, ParticleOptions particle, Vec3 position) {
        for (var viewer : level.players()) if (viewer.distanceToSqr(position) < 96 * 96)
            level.sendParticles(viewer, particle, true, position.x, position.y, position.z, 1, 0, 0, 0, 0);
    }
    public static void charge(ServerLevel level, Vec3 position) {
        point(level, CHARGE_BLUE, position); point(level, CHARGE_PURPLE, position.add(0, .08, 0));
    }
    public static void railTrail(ServerLevel level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        int steps = Math.min(128, Math.max(1, (int)(delta.length() * 3)));
        var direction = delta.normalize();
        var side = direction.cross(Math.abs(direction.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        var up = side.cross(direction).normalize();
        for (int i = 0; i <= steps; i++) {
            var center = start.add(delta.scale((double)i / steps));
            double angle = i * .65;
            var offset = side.scale(Math.cos(angle) * .12).add(up.scale(Math.sin(angle) * .12));
            point(level, TRAIL_BLUE, center.add(offset)); point(level, TRAIL_PURPLE, center.subtract(offset));
            if (i % 4 == 0) point(level, ParticleTypes.END_ROD, center);
        }
    }
    public static void detonation(ServerLevel level, Vec3 center) {
        point(level, ParticleTypes.EXPLOSION_EMITTER, center);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            for (int ring = 1; ring <= 2; ring++) {
                double r = ring * 1.45;
                var offset = ring == 1 ? new Vec3(Math.cos(angle) * r, .05, Math.sin(angle) * r)
                        : new Vec3(Math.cos(angle) * r, Math.sin(angle) * r, 0);
                point(level, ring == 1 ? BLUE : PURPLE, center.add(offset));
            }
            if (i % 2 == 0) point(level, ParticleTypes.END_ROD,
                    center.add(Math.cos(angle) * 1.5, Math.sin(angle * 3) * 1.2, Math.sin(angle) * 1.5));
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, .8f);
    }
    public static void blade(ServerLevel level, Vec3 origin, Vec3 look, WeaponProfile profile) {
        boolean star = profile == WeaponProfile.STARCORE_BLADE;
        var forward = look.normalize();
        var side = forward.cross(Math.abs(forward.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        origin = origin.add(0, star ? -.45 : -1.1, 0);
        if (star) side = side.scale(.8).add(side.cross(forward).normalize().scale(.6));
        for (int ring = 1; ring <= 3; ring++) for (int i = -18; i <= 18; i++) {
            double angle = i * 1.1 / 18;
            double radius = profile.range * ring / 3.0;
            var pos = origin.add(forward.scale(Math.cos(angle) * radius)).add(side.scale(Math.sin(angle) * radius));
            point(level, star ? (i % 2 == 0 ? PURPLE : BLUE) : TEAL, pos);
            if (ring == 3 && i % 4 == 0) point(level, star ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK, pos);
        }
        point(level, ParticleTypes.SWEEP_ATTACK, origin.add(forward.scale(1.5)));
        if (star) level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, .65f);
    }
}
