package com.starboundmc.sound;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original StarboundMC sound events. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, StarboundMC.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> NOVA_TEXT = SOUND_EVENTS.register(
            "nova_text",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(
                    StarboundMC.MODID, "nova_text")));

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
