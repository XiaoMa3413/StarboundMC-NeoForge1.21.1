package com.starboundmc.sound;

import com.starboundmc.StarboundMC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StarboundMC.MODID);

    public static final RegistryObject<SoundEvent> WARP_START =
            SOUNDS.register("warp_start", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_start")));

    public static final RegistryObject<SoundEvent> WARP_LOOP =
            SOUNDS.register("warp_loop", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_loop")));

    public static final RegistryObject<SoundEvent> WARP_END =
            SOUNDS.register("warp_end", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "warp_end")));

    public static final RegistryObject<SoundEvent> TELEPORTER_USE =
            SOUNDS.register("teleporter_use", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "teleporter_use")));

    public static void register(IEventBus modEventBus)
    {
        SOUNDS.register(modEventBus);
    }
}
