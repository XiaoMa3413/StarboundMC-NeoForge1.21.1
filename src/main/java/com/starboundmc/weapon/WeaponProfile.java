package com.starboundmc.weapon;

/** Shared authoritative tuning; tooltips read these same values. Damage includes player base for blades. */
public enum WeaponProfile {
    TUNGSTEN_SABRE(7, 2.0f, 600, 0, 0, 0, 0, 0, 0),
    COIL_PISTOL(6, 0, 384, 32, 4, 1, 1, 0, 0),
    DURASTEEL_GLAIVE(10, 1.4f, 1200, 5.5, 60, 0, 3, 0, 0),
    PLASMA_CARBINE(8, 0, 768, 40, 2, 1, 1, 0, 2),
    STARCORE_BLADE(13, 1.6f, 2400, 6, 100, 0, 5, 0, 0),
    STARCORE_RAILGUN(30, 0, 1536, 64, 16, 2, 1, 20, 0);

    public final float damage, attackSpeed;
    public final double range;
    public final int durability, cooldown, ammoCost, targets, chargeTicks, fireSeconds;

    WeaponProfile(float damage, float attackSpeed, int durability, double range, int cooldown,
                  int ammoCost, int targets, int chargeTicks, int fireSeconds) {
        this.damage = damage; this.attackSpeed = attackSpeed; this.durability = durability;
        this.range = range; this.cooldown = cooldown; this.ammoCost = ammoCost;
        this.targets = targets; this.chargeTicks = chargeTicks; this.fireSeconds = fireSeconds;
    }

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean ranged() { return attackSpeed == 0; }
    public float chargedDamage(int ticks) {
        if (chargeTicks == 0) return damage;
        return ticks < chargeTicks / 2 ? 0 : damage * (0.2f + 0.8f * Math.min(ticks, chargeTicks) / chargeTicks);
    }
}
