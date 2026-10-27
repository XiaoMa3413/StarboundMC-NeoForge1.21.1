package com.starboundmc.enemy;

import com.starboundmc.StarboundMC;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

/** No biome modifiers or natural-spawn entries: enemies are exposed through eggs only. */
public final class ModEnemies {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, StarboundMC.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StarboundMC.MODID);
    private static final Map<EnemyProfile, DeferredHolder<EntityType<?>, EntityType<MachineEnemy>>> ENEMIES = new EnumMap<>(EnemyProfile.class);
    private static final Map<EnemyProfile, DeferredItem<DeferredSpawnEggItem>> EGGS = new EnumMap<>(EnemyProfile.class);
    public static final DeferredHolder<EntityType<?>, EntityType<EnemyBolt>> BOLT;
    static {
        for (var profile : EnemyProfile.values()) {
            var type = TYPES.register(profile.id, () -> EntityType.Builder.of(MachineEnemy::new, MobCategory.MONSTER)
                    .sized(profile.width, profile.height).eyeHeight(profile.eyeHeight)
                    .clientTrackingRange(profile.boss() ? 12 : 8).updateInterval(2).build(profile.id));
            ENEMIES.put(profile, type);
            EGGS.put(profile, ITEMS.register(profile.id+"_spawn_egg", () -> new DeferredSpawnEggItem(
                    type, profile.eggBase, profile.eggSpots, new Item.Properties())));
        }
        BOLT = TYPES.register("enemy_plasma_bolt", () -> EntityType.Builder.<EnemyBolt>of(EnemyBolt::new, MobCategory.MISC)
                .sized(.2f,.2f).clientTrackingRange(8).updateInterval(1).noSave().build("enemy_plasma_bolt"));
    }
    public static EntityType<MachineEnemy> type(EnemyProfile profile) { return ENEMIES.get(profile).get(); }
    public static Item egg(EnemyProfile profile) { return EGGS.get(profile).get(); }
    public static void register(IEventBus bus) { TYPES.register(bus); ITEMS.register(bus); bus.addListener(ModEnemies::attributes); }
    public static void creative(CreativeModeTab.Output output) { for (var profile : EnemyProfile.values()) output.accept(egg(profile)); }
    public static void attributes(EntityAttributeCreationEvent event) {
        for (var profile : EnemyProfile.values()) event.put(type(profile), net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, profile.health).add(Attributes.ARMOR, profile.armor)
                .add(Attributes.MOVEMENT_SPEED, profile.speed).add(Attributes.ATTACK_DAMAGE, profile.damage)
                .add(Attributes.FOLLOW_RANGE, profile.boss() ? 40 : 28)
                .add(Attributes.KNOCKBACK_RESISTANCE, profile.boss() ? .9 : profile==EnemyProfile.BREACH_GUARD ? .6 : .15)
                .add(Attributes.FLYING_SPEED, profile.speed).add(Attributes.STEP_HEIGHT, profile.boss() ? 1.25 : .65).build());
    }
    private ModEnemies() {}
}
