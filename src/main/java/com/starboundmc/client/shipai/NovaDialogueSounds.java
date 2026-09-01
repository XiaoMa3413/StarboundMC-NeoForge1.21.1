package com.starboundmc.client.shipai;

import com.starboundmc.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Plays a quiet, non-positional chirp as N.O.V.A. text becomes visible. */
final class NovaDialogueSounds {
    private static final float VOLUME = 0.16F;

    private final NovaTextSoundCadence cadence = new NovaTextSoundCadence();

    void onCodePointRevealed(int codePoint) {
        float pitch = cadence.pitchForCodePoint(codePoint);
        if (pitch == NovaTextSoundCadence.SILENT)
            return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ModSounds.NOVA_TEXT.get(), pitch, VOLUME));
    }
}
