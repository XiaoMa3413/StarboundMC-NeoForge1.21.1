package com.starboundmc.weapon;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-owned combat; vanilla synchronizes cooldowns and persists inventory/durability. */
public final class WeaponCombat {
    private WeaponCombat() {}

    public static net.minecraft.world.damagesource.DamageSource boltDamage(ServerLevel level, Player player) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("starboundmc", "weapon_bolt"));
        return new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key), player);
    }

    public static boolean canHit(Player owner, LivingEntity target) {
        return target != owner && target.isAlive() && !target.isSpectator()
                && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
                && (!(target instanceof Player other) || owner.canHarmPlayer(other)
                    && owner.getServer() != null && owner.getServer().isPvpAllowed());
    }

    public static boolean hasAmmo(Player player, Item ammo, int cost) {
        if (player.getAbilities().instabuild) return true;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(ammo)) count += stack.getCount();
            if (count >= cost) return true;
        }
        return false;
    }

    public static boolean consumeAmmo(Player player, Item ammo, int cost) {
        if (!hasAmmo(player, ammo, cost)) return false;
        if (player.getAbilities().instabuild) return true;
        for (int i = 0; i < player.getInventory().getContainerSize() && cost > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.is(ammo)) continue;
            int consumed = Math.min(cost, stack.getCount());
            stack.shrink(consumed); cost -= consumed;
        }
        player.getInventory().setChanged();
        return true;
    }

    public static Vec3 wallEnd(Player player, double range) {
        var start = player.getEyePosition();
        return player.level().clip(new ClipContext(start, start.add(player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
    }

    public static List<LivingEntity> rayTargets(Player player, Vec3 end, int limit) {
        var start = player.getEyePosition();
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.3),
                target -> canHit(player, target) && target.getBoundingBox().inflate(0.15).clip(start, end).isPresent())
                .stream().sorted(Comparator.comparingDouble(target -> target.getBoundingBox().inflate(0.15)
                        .clip(start, end).orElse(end).distanceToSqr(start))).limit(limit).toList();
    }

    public static final double BLAST_RADIUS = 3.5;
    public static void blast(ServerLevel level, Player player, Vec3 impact, float damage, List<LivingEntity> directHits) {
        // Bias onto the incoming side of a solid surface; a wall shields entities behind it.
        var origin = impact.subtract(player.getLookAngle().scale(.05));
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("starboundmc", "weapon_blast"));
        var source = new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key), player);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(BLAST_RADIUS),
                entity -> canHit(player, entity) && !directHits.contains(entity))) {
            var center = target.getBoundingBox().getCenter();
            double distance = center.distanceTo(origin);
            if (distance >= BLAST_RADIUS) continue;
            if (level.clip(new ClipContext(origin, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != net.minecraft.world.phys.HitResult.Type.MISS) continue;
            float splash = damage * (float)(1 - distance / BLAST_RADIUS);
            if (target.hurt(source, splash)) target.knockback(.7 * (1 - distance / BLAST_RADIUS),
                    origin.x - target.getX(), origin.z - target.getZ());
        }
    }

    public static void beam(ServerLevel level, Vec3 start, Vec3 end, boolean plasma) {
        var delta = end.subtract(start);
        int steps = Math.min(80, Math.max(1, (int) (delta.length() * 2)));
        for (int i = 0; i <= steps; i++) {
            var pos = start.add(delta.scale((double) i / steps));
            level.sendParticles(plasma ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
}
