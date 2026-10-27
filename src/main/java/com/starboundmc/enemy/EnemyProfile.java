package com.starboundmc.enemy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/** Initial spawn-egg playtest roster; gameplay values are server-owned. */
public enum EnemyProfile {
    SCRAP_SKITTER("scrap_skitter", 40, 2, .28, 4, 1.3f, .9f, .5f, 8, 0x72777a, 0xd6a52d),
    COIL_SENTRY("coil_sentry", 72, 4, .18, 5, 1.05f, 1.3f, .75f, 16, 0xc9c8c0, 0x48dce7),
    BREACH_GUARD("breach_guard", 180, 14, .18, 8, 1.45f, 2.35f, 2.05f, 45, 0x42515e, 0xb76632),
    QUARRY_WARDEN("quarry_warden", 480, 12, .16, 8, 3.0f, 2.85f, 2.1f, 160, 0xc69c32, 0x45cdd7);

    public final String id;
    public final double health, armor, speed, damage;
    public final float width, height, eyeHeight;
    public final int experience, eggBase, eggSpots;
    EnemyProfile(String id, double health, double armor, double speed, double damage,
            float width, float height, float eyeHeight, int experience, int eggBase, int eggSpots) {
        this.id=id; this.health=health; this.armor=armor; this.speed=speed; this.damage=damage;
        this.width=width; this.height=height; this.eyeHeight=eyeHeight;
        this.experience=experience; this.eggBase=eggBase; this.eggSpots=eggSpots;
    }
    public boolean flying() { return this == COIL_SENTRY; }
    public boolean boss() { return this == QUARRY_WARDEN; }
    public static EnemyProfile of(EntityType<?> type) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key.getNamespace().equals("starboundmc"))
            for (var value : values()) if (value.id.equals(key.getPath())) return value;
        throw new IllegalArgumentException("Unknown mechanical enemy: " + key);
    }
}
