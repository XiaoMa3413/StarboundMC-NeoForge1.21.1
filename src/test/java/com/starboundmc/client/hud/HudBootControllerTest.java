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
        controller.applyServerState(CoreState.OFFLINE, true, false, false);
        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        assertTrue(controller.localNavigationActive());
        assertTrue(controller.terminalGuidanceActive());
    }

    @Test
    void terminalContactAndOnlineCoreDisableTutorialGuidance() {
        controller.applyServerState(CoreState.OFFLINE, true, true, false);
        assertFalse(controller.terminalGuidanceActive());
        assertFalse(controller.localNavigationActive());

        controller.applyServerState(CoreState.ONLINE, true, true, true);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        assertEquals(1F, controller.visorOpacity());
    }

    @Test
    void rebootWaitsForServerTruthThenCompletesAndAcknowledgesOnce() {
        controller.applyServerState(CoreState.OFFLINE, true, true, false);
        controller.applyServerState(CoreState.REBOOTING, true, true, false);
        assertEquals(HudBootController.State.LINKING, controller.state());
        assertTrue(controller.defersCommunication());
        assertFalse(controller.presentation(0F).personalLink());
        advance(500);
        assertEquals(HudBootController.State.LINKING, controller.state());
        assertFalse(controller.consumeCoreLinkReceipt());

        controller.applyServerState(CoreState.ONLINE, true, true, false);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        assertEquals(1F, controller.presentation(0F).statusOpacity());
        advance(HudBootController.ONLINE_STATUS_TICKS - 1);
        assertFalse(controller.consumeCoreLinkReceipt());
        advance(1);
        assertFalse(controller.defersCommunication());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        assertTrue(controller.consumeCoreLinkReceipt());
        assertFalse(controller.consumeCoreLinkReceipt());
        controller.applyServerState(CoreState.ONLINE, true, true, false);
        advance(100);
        assertFalse(controller.consumeCoreLinkReceipt());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
    }

    @Test
    void onlineLateJoinUsesPersonalInitializationWithoutOfflineBoot() {
        controller.applyServerState(CoreState.ONLINE, false, false, false);
        assertEquals(HudBootController.State.LINKING, controller.state());
        assertTrue(controller.presentation(0F).personalLink());
        assertEquals(0F, controller.presentation(0F).promptOpacity());
        assertFalse(controller.terminalGuidanceActive());
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        assertEquals(HudBootController.State.LINKING, controller.state());
        advance(HudBootController.PERSONAL_LINK_TICKS);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        advance(HudBootController.ONLINE_STATUS_TICKS);
        assertTrue(controller.consumeCoreLinkReceipt());
    }

    @Test
    void pauseFreezesLinkAndCompletionButDoesNotDelayCoreTruth() {
        controller.applyServerState(CoreState.REBOOTING, true, true, false);
        advance(12);
        controller.tick(true);
        var before = controller.presentation(0F);
        for (int tick = 0; tick < 200; tick++) controller.tick(true);
        assertEquals(before, controller.presentation(.8F));
        controller.applyServerState(CoreState.ONLINE, true, true, false);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        for (int tick = 0; tick < 200; tick++) controller.tick(true);
        assertEquals(1F, controller.presentation(.8F).statusOpacity());
        assertFalse(controller.consumeCoreLinkReceipt());
        advance(HudBootController.ONLINE_STATUS_TICKS);
        assertTrue(controller.consumeCoreLinkReceipt());
    }

    @Test
    void reconnectSkipsCompletedLinkAndRecoversInterruptedLink() {
        controller.applyServerState(CoreState.ONLINE, false, false, true);
        assertEquals(HudBootController.State.ONLINE, controller.state());
        assertFalse(controller.defersCommunication());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        controller.reset();
        controller.applyServerState(CoreState.REBOOTING, true, true, false);
        assertEquals(HudBootController.State.LINKING, controller.state());
        controller.reset();
        controller.applyServerState(CoreState.ONLINE, true, true, false);
        assertTrue(controller.presentation(0F).personalLink());
    }

    @Test
    void terminalContactBeforeWakeEnablesHudAndSuppressesLateEmergency() {
        controller.applyServerState(CoreState.OFFLINE, false, true, false);
        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertEquals(1F, controller.visorOpacity());
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        controller.onNovaBroadcast(HudBootController.TERMINAL_REMINDER_KEY);
        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertFalse(controller.terminalGuidanceActive());
    }

    @Test
    void sharedRebootInterruptsEmergencyAndDuplicateSnapshotsDoNotRestartLink() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        advance(10);
        controller.applyServerState(CoreState.REBOOTING, true, false, false);
        advance(15);
        var before = controller.presentation(0F);
        controller.applyServerState(CoreState.REBOOTING, true, false, false);
        assertEquals(before, controller.presentation(0F));
        assertFalse(controller.terminalGuidanceActive());
        controller.applyServerState(CoreState.ONLINE, true, false, false);
        advance(30);
        before = controller.presentation(0F);
        controller.applyServerState(CoreState.ONLINE, true, false, false);
        assertEquals(before, controller.presentation(0F));
    }

    @Test
    void playersShareCoreTruthButKeepIndependentPresentationReceipts() {
        var returning = new HudBootController();
        returning.applyServerState(CoreState.ONLINE, true, true, true);
        controller.applyServerState(CoreState.ONLINE, false, false, false);
        assertFalse(returning.defersCommunication());
        assertTrue(controller.defersCommunication());
        assertEquals(HudBootController.State.ONLINE, returning.state());
        assertEquals(HudBootController.State.LINKING, controller.state());
    }

    private void advance(int ticks) {
        for (int tick = 0; tick < ticks; tick++) controller.tick(false);
    }
}
