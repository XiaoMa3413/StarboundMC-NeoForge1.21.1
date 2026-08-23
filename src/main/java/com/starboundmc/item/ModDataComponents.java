package com.starboundmc.item;

import com.starboundmc.StarboundMC;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StarboundMC.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MatterManipulatorUpgrades>>
            MATTER_MANIPULATOR_UPGRADES = COMPONENTS.registerComponentType(
                    "matter_manipulator_upgrades",
                    builder -> builder.persistent(MatterManipulatorUpgrades.CODEC)
                            .networkSynchronized(MatterManipulatorUpgrades.STREAM_CODEC)
                            .cacheEncoding());

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
