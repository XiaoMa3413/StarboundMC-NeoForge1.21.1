package com.starboundmc.client.hud;

import com.starboundmc.story.CoreState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudBootControllerTest {
    private final HudBootController controller = HudBootController.INSTANCE;

    @AfterEach
    void resetSingleton() {
        controller.reset();
    }

    @Test
    void initialWakeRunsStartingThenSettlesInSafeMode() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        assertEquals(HudBootController.State.STARTING, controller.state());
        assertTrue(controller.terminalGuidanceActive());
        assertEquals(0F, controller.visorOpacity());

        for (int tick = 0; tick < HudBootController.STARTING_TICKS; tick++)
            controller.tick(false);

        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertEquals(1F, controller.visorOpacity());
        assertTrue(controller.presentation(0F).statusOpacity() > 0F);
    }

    @Test
    void pauseFreezesBootstrapClock() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        controller.tick(true);
        assertEquals(0F, controller.visorOpacity());
        controller.tick(false);
        assertTrue(controller.visorOpacity() > 0F);
    }

    @Test
    void centerPromptLeadsIntoTheScrollingStatusSequence() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        assertEquals(0F, controller.presentation(0F).promptOpacity());
        assertEquals(0, controller.presentation(0F).revealedLines());

        for (int tick = 0; tick < HudBootController.STATUS_START_TICK; tick++)
            controller.tick(false);
        var firstStatus = controller.presentation(0F);
        assertTrue(firstStatus.promptOpacity() > 0F);
        assertEquals(1, firstStatus.revealedLines());

        for (int tick = HudBootController.STATUS_START_TICK;
             tick < HudBootController.STATUS_START_TICK
                     + HudBootController.STATUS_STEP_TICKS * 5
                     + HudBootController.STATUS_SCROLL_TICKS;
             tick++)
            controller.tick(false);
        var finalStatus = controller.presentation(0F);
        assertEquals(HudBootController.STATUS_STEP_COUNT, finalStatus.revealedLines());
        assertTrue(finalStatus.scrollRows() > 1.9F);
        assertEquals(0F, finalStatus.promptOpacity());
        assertFalse(controller.localNavigationActive());
    }

    @Test
    void persistedWakeResumesSafeModeWithoutReplayingBoot() {
        controller.applyServerState(CoreState.OFFLINE, true, false);
        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        assertTrue(controller.localNavigationActive());
        assertTrue(controller.terminalGuidanceActive());
    }

    @Test
    void terminalContactAndOnlineCoreDisableTutorialGuidance() {
        controller.applyServerState(CoreState.OFFLINE, true, true);
        assertFalse(controller.terminalGuidanceActive());
        assertFalse(controller.localNavigationActive());

        controller.applyServerState(CoreState.ONLINE, true, true);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        assertEquals(1F, controller.visorOpacity());
    }
}
