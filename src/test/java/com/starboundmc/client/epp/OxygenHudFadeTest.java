package com.starboundmc.client.epp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OxygenHudFadeTest {
    @Test void safeAirHoldsThenFadesAndDangerRestoresQuickly() {
        var fade = new OxygenHudFade();
        for (int i = 0; i < 10; i++) fade.update(.1f, true);
        for (int i = 0; i < 10; i++) assertEquals(1f, fade.update(.1f, false));
        float alpha = 1;
        for (int i = 0; i < 15; i++) alpha = fade.update(.1f, false);
        assertEquals(0f, alpha);
        assertTrue(fade.update(.1f, true) > .6f);
        assertEquals(1f, fade.update(.1f, true));
    }
    @Test void pauseAndInterruptedSafetyDoNotHideActiveReadout() {
        var fade = new OxygenHudFade();
        fade.update(.1f, true); fade.update(.1f, true);
        for (int i = 0; i < 100; i++) assertEquals(1f, fade.update(0, false));
        for (int i = 0; i < 10; i++) fade.update(.1f, false);
        fade.update(.1f, true);
        for (int i = 0; i < 10; i++) assertEquals(1f, fade.update(.1f, false));
    }
}
