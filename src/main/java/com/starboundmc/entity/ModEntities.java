package com.starboundmc.entity;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    // Stage 2 replaces this registration shell with the preserved SeatEntity.
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StarboundMC.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Stage1SeatEntity>> SEAT =
            ENTITIES.register("seat", () -> EntityType.Builder.of(Stage1SeatEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.6F)
                    .clientTrackingRange(5)
                    .updateInterval(1)
                    .noSave()
                    .build("seat"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }

    public static final class Stage1SeatEntity extends Entity {
        public Stage1SeatEntity(EntityType<? extends Stage1SeatEntity> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }
    }
}
