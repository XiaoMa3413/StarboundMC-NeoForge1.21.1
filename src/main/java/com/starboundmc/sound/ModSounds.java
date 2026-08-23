package com.starboundmc.sound;

import com.starboundmc.StarboundMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, StarboundMC.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WARP_START =
            SOUNDS.register("warp_start", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_start")));

    public static final DeferredHolder<SoundEvent, SoundEvent> WARP_LOOP =
            SOUNDS.register("warp_loop", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> WARP_END =
            SOUNDS.register("warp_end", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORTER_USE =
            SOUNDS.register("teleporter_use", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "teleporter_use")));

    public static void register(IEventBus modEventBus)
    {
        SOUNDS.register(modEventBus);
    }
}
