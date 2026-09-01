package com.starboundmc.client.shipai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaTextSoundCadenceTest {
    @Test
    void soundUsesAlternatingTwoAndThreeCharacterSpacing() {
        NovaTextSoundCadence cadence = new NovaTextSoundCadence();

        assertTrue(cadence.pitchForCodePoint('A') > 0F);
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint('B'));
        assertTrue(cadence.pitchForCodePoint('C') > 0F);
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint('D'));
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint('E'));
        assertTrue(cadence.pitchForCodePoint('F') > 0F);
    }

    @Test
    void whitespaceAndPunctuationStaySilentAndSentenceRestartsCleanly() {
        NovaTextSoundCadence cadence = new NovaTextSoundCadence();

        assertTrue(cadence.pitchForCodePoint('\u4F60') > 0F);
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint(' '));
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint('\u597D'));
        assertEquals(NovaTextSoundCadence.SILENT, cadence.pitchForCodePoint('\u3002'));
        assertTrue(cadence.pitchForCodePoint('\u901A') > 0F);
    }
}
