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
    void navigationEstablishesBeforeArAndSafeModeWithoutAdvancingWhilePaused() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        int navigationTick = -1, arTick = -1;
        for (int tick = 0; tick < HudBootController.STARTING_TICKS; tick++) {
            if (controller.localNavigationActive() && navigationTick < 0) navigationTick = tick;
            if (controller.worldArActive() && arTick < 0) arTick = tick;
            boolean navigation = controller.localNavigationActive();
            boolean ar = controller.worldArActive();
            controller.tick(true);
            assertEquals(navigation, controller.localNavigationActive());
            assertEquals(ar, controller.worldArActive());
            assertEquals(HudBootController.State.STARTING, controller.state());
            controller.tick(false);
        }
        assertTrue(navigationTick > 0 && arTick > navigationTick);
        assertEquals(HudBootController.State.SAFE_MODE, controller.state());
        assertTrue(controller.localNavigationActive());
        assertTrue(controller.worldArActive());
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
    void capabilityCuesFollowRestorationAndFinishBeforeCommunication() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        assertEquals(HudBootController.Cue.VISUAL_RESTORE, controller.presentation(0).cue());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        advance(HudBootController.NAVIGATION_START_TICK);
        assertEquals(HudBootController.Cue.LOCAL_NAVIGATION, controller.presentation(0).cue());
        assertEquals(1F, controller.presentation(0).calibrationProgress());
        assertTrue(controller.localNavigationActive());
        assertFalse(controller.worldArActive());
        advance(HudBootController.CORE_CHECK_TICK - HudBootController.NAVIGATION_START_TICK);
        assertEquals(HudBootController.Cue.CORE_UNAVAILABLE, controller.presentation(0).cue());
        advance(HudBootController.STARTING_TICKS - HudBootController.CORE_CHECK_TICK);
        assertEquals(HudBootController.Cue.SAFE_MODE, controller.presentation(0).cue());
        assertTrue(controller.defersCommunication());
        advance(HudBootController.SAFE_MODE_HOLD_TICKS + HudBootController.SAFE_MODE_FADE_TICKS);
        assertFalse(controller.defersCommunication());
        assertEquals(0F, controller.presentation(0F).statusOpacity());
        assertTrue(controller.worldArActive());
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
        assertEquals(HudBootController.Cue.PERSONAL_INITIALIZATION, controller.presentation(0F).cue());
        assertEquals(1F, controller.presentation(0F).calibrationProgress());
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

    @Test
    void repeatedOfflineSnapshotDoesNotDismissSafeModeCue() {
        controller.onNovaBroadcast(HudBootController.INITIAL_WAKE_KEY);
        advance(HudBootController.STARTING_TICKS + 5);
        var before = controller.presentation(0);
        controller.applyServerState(CoreState.OFFLINE, true, false, false);
        assertEquals(before, controller.presentation(0));
        assertTrue(controller.defersCommunication());
    }

    @Test
    void lateJoinNeverShowsAnOfflineCueAndOnlyAcknowledgesVisibleCompletion() {
        controller.applyServerState(CoreState.ONLINE, false, false, false);
        for (int tick = 0; tick < HudBootController.PERSONAL_LINK_TICKS + HudBootController.ONLINE_STATUS_TICKS; tick++) {
            var cue = controller.presentation(.5F).cue();
            assertTrue(cue == HudBootController.Cue.PERSONAL_INITIALIZATION
                    || cue == HudBootController.Cue.CORE_SYNCHRONIZING || cue == HudBootController.Cue.ONLINE);
            controller.tick(true);
            assertFalse(controller.consumeCoreLinkReceipt());
            controller.tick(false);
        }
        assertTrue(controller.consumeCoreLinkReceipt());
    }

    private void advance(int ticks) {
        for (int tick = 0; tick < ticks; tick++) controller.tick(false);
    }
}
