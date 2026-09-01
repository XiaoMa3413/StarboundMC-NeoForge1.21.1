package com.starboundmc.loot;

import com.mojang.serialization.MapCodec;
import com.starboundmc.StarboundMC;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Global loot modifier serializers feeding the data-driven voxel drops. */
public final class VoxelLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, StarboundMC.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VoxelDropLootModifier>>
            ADD_VOXEL_DROP = LOOT_MODIFIERS.register("add_voxel_drop", () -> VoxelDropLootModifier.CODEC);

    private VoxelLootModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
    }
}
