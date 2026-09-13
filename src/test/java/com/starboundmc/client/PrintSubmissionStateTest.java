package com.starboundmc.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrintSubmissionStateTest {
    @Test
    void blocksDuplicateClicksUntilAcknowledgedEvenAfterDelay() {
        var state = new PrintSubmissionState();
        assertTrue(state.begin(100));
        assertFalse(state.begin(101));
        assertTrue(state.delayed(5_100));
        assertEquals(PrintSubmissionState.Status.WAITING, state.status(60_000));
        assertFalse(state.begin(60_001));
        state.complete(true, 60_002);
        assertEquals(PrintSubmissionState.Status.ACCEPTED, state.status(60_003));
        assertFalse(state.waiting());
        assertEquals(PrintSubmissionState.Status.IDLE, state.status(64_002));
        assertTrue(state.begin(64_003));
    }

    @Test
    void rejectionUnlocksRetryAndUnsolicitedAcknowledgementsDoNothing() {
        var state = new PrintSubmissionState();
        state.complete(true, 10);
        assertEquals(PrintSubmissionState.Status.IDLE, state.status(11));
        state.begin(20);
        state.complete(false, 30);
        assertEquals(PrintSubmissionState.Status.REJECTED, state.status(31));
        assertTrue(state.begin(32));
        assertEquals(PrintSubmissionState.Status.WAITING, state.status(33));
    }
}
