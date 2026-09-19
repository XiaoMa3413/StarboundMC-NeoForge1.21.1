package com.starboundmc.client.epp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudVisibilityFadeTest {
    @Test void startsHiddenAndFadesBothWays() {
        var fade = new HudVisibilityFade();
        assertEquals(0, fade.update(.1f, false));
        assertEquals(.4f, fade.update(.1f, true), .001f);
        fade.update(.1f, true);
        assertEquals(1, fade.update(.1f, true));
        assertEquals(.75f, fade.update(.1f, false), .001f);
        for (int i = 0; i < 4; i++) fade.update(.1f, false);
        assertEquals(0, fade.update(.1f, false));
    }

    @Test void pauseAndRapidReentryPreserveContinuousOpacity() {
        var fade = new HudVisibilityFade();
        fade.update(.1f, true);
        assertEquals(.4f, fade.update(0, false), .001f);
        assertEquals(.15f, fade.update(.1f, false), .001f);
        assertEquals(.55f, fade.update(.1f, true), .001f);
    }
}
