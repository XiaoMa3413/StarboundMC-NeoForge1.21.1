package com.starboundmc.client.shipai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaBroadcastTimelineTest
{
    @Test
    void completedLineEntersChatHistoryOnlyAfterTheHudCloses()
    {
        NovaBroadcastTimeline timeline = new NovaBroadcastTimeline();
        assertTrue(timeline.enqueue("message.test", "你好"));

        timeline.tick(false);
        assertEquals(NovaBroadcastTimeline.Phase.OPENING, timeline.snapshot().phase());
        advance(timeline, NovaBroadcastTimeline.OPENING_TICKS);
        assertEquals(NovaBroadcastTimeline.Phase.STREAMING, timeline.snapshot().phase());

        assertEquals(1, timeline.tick(false).revealedCount());
        NovaBroadcastTimeline.Step finalCharacter = timeline.tick(false);
        assertEquals(1, finalCharacter.revealedCount());
        assertNull(finalCharacter.completedMessage());
        assertEquals("你好", timeline.snapshot().visibleText());
        assertEquals(NovaBroadcastTimeline.Phase.HOLDING, timeline.snapshot().phase());

        advance(timeline, NovaBroadcastTimeline.COMPLETION_HOLD_TICKS);
        assertEquals(NovaBroadcastTimeline.Phase.CLOSING, timeline.snapshot().phase());
        NovaBroadcastTimeline.Step completed = advanceUntilCompleted(timeline);
        assertNotNull(completed.completedMessage());
        assertEquals("你好", completed.completedMessage().body());
        assertFalse(timeline.snapshot().visible());
        assertEquals(0, timeline.queuedMessageCount());
    }

    @Test
    void longProgressDotsAdvanceOneAtATime()
    {
        NovaBroadcastTimeline timeline = streaming("A......B");

        timeline.tick(false);
        assertEquals("A", timeline.snapshot().visibleText());
        for (int dot = 1; dot <= 6; dot++)
        {
            NovaBroadcastTimeline.Step step = advanceUntilReveal(timeline);
            assertEquals(1, step.revealedCount());
            assertEquals("A" + ".".repeat(dot), timeline.snapshot().visibleText());
        }
    }

    @Test
    void pausedPresentationDoesNotConsumeTextOrTimers()
    {
        NovaBroadcastTimeline timeline = streaming("Signal stable");
        NovaBroadcastTimeline.Snapshot before = timeline.snapshot();

        advance(timeline, 20, true);

        assertEquals(before, timeline.snapshot());
        assertEquals(1, timeline.queuedMessageCount());
    }

    @Test
    void queueLimitDoesNotInterruptTheActiveTransmission()
    {
        NovaBroadcastTimeline timeline = new NovaBroadcastTimeline();
        assertTrue(timeline.enqueue("message.active", "active"));
        timeline.tick(false);

        for (int index = 0; index < NovaBroadcastTimeline.MAX_QUEUED_MESSAGES; index++)
            assertTrue(timeline.enqueue("message." + index, "queued " + index));
        assertFalse(timeline.enqueue("message.overflow", "overflow"));
        assertEquals(NovaBroadcastTimeline.MAX_QUEUED_MESSAGES + 1,
                timeline.queuedMessageCount());
    }

    private static NovaBroadcastTimeline streaming(String body)
    {
        NovaBroadcastTimeline timeline = new NovaBroadcastTimeline();
        assertTrue(timeline.enqueue("message.test", body));
        timeline.tick(false);
        advance(timeline, NovaBroadcastTimeline.OPENING_TICKS);
        assertEquals(NovaBroadcastTimeline.Phase.STREAMING, timeline.snapshot().phase());
        return timeline;
    }

    private static void advance(NovaBroadcastTimeline timeline, int ticks)
    {
        advance(timeline, ticks, false);
    }

    private static void advance(NovaBroadcastTimeline timeline, int ticks, boolean paused)
    {
        for (int tick = 0; tick < ticks; tick++)
            timeline.tick(paused);
    }

    private static NovaBroadcastTimeline.Step advanceUntilCompleted(
            NovaBroadcastTimeline timeline)
    {
        for (int tick = 0; tick <= NovaBroadcastTimeline.CLOSING_TICKS; tick++)
        {
            NovaBroadcastTimeline.Step step = timeline.tick(false);
            if (step.completedMessage() != null)
                return step;
        }
        throw new AssertionError("HUD did not complete its closing phase");
    }

    private static NovaBroadcastTimeline.Step advanceUntilReveal(
            NovaBroadcastTimeline timeline)
    {
        for (int tick = 0; tick <= 4; tick++)
        {
            NovaBroadcastTimeline.Step step = timeline.tick(false);
            if (step.revealedCount() > 0)
                return step;
        }
        throw new AssertionError("Text cadence did not reveal its next code point");
    }
}
