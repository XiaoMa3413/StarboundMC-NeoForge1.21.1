package com.starboundmc.client.shipai;

import com.starboundmc.story.NovaTask;
import com.starboundmc.story.NovaTaskProgress;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NovaTerminalViewStateTest {
    private static final NovaTaskProgress REPAIR = new NovaTaskProgress(NovaTaskProgress.SCHEMA, 2,
            NovaTask.CONTACT.mask() | NovaTask.SURFACE.mask(), 0, 0, NovaTask.REPAIR.id(), "");

    @Test
    void firstOpenStartsInCommsAndLaterSnapshotsDoNotReplaceTheReadingPage() {
        var view = new NovaTerminalViewState();
        view.synchronize(REPAIR, true);
        assertEquals(NovaTerminalViewState.Page.COMMS, view.page());
        assertEquals(NovaTask.REPAIR, view.selected());
        view.show(NovaTerminalViewState.Page.ARCHIVE);
        view.archiveScroll(75);
        view.synchronize(REPAIR, true);
        assertEquals(NovaTerminalViewState.Page.ARCHIVE, view.page());
        assertEquals(75, view.archiveScroll());
        view.synchronize(REPAIR, false);
        assertEquals(NovaTerminalViewState.Page.COMMS, view.page());
    }

    @Test
    void taskQuestionReturnsToTheSameExpandedTaskAndReadingPosition() {
        var view = new NovaTerminalViewState();
        view.synchronize(REPAIR, true);
        view.openTask(NovaTask.LIFE_SUPPORT, REPAIR);
        view.taskScroll(83);
        view.historyOpen(true);
        view.askSelected();
        assertEquals(NovaTerminalViewState.Page.COMMS, view.page());
        assertEquals(NovaTask.LIFE_SUPPORT, view.inquiry());
        assertFalse(view.historyOpen());
        view.returnToTask(REPAIR);
        assertEquals(NovaTerminalViewState.Page.TASKS, view.page());
        assertEquals(NovaTask.LIFE_SUPPORT, view.selected());
        assertTrue(view.expanded());
        assertEquals(83, view.taskScroll());
    }

    @Test
    void askingNextEndsTheInquiryWithoutLosingTaskReadingPosition() {
        var view = new NovaTerminalViewState();
        view.openTask(NovaTask.REPAIR, REPAIR);
        view.taskScroll(83);
        view.askSelected();
        view.returnToTask(REPAIR);
        view.historyOpen(true);

        view.askNext();

        assertNull(view.inquiry());
        assertEquals(NovaTerminalViewState.Page.COMMS, view.page());
        assertFalse(view.historyOpen());
        assertEquals(NovaTask.REPAIR, view.selected());
        assertEquals(83, view.taskScroll());
    }

    @Test
    void completedFilterOpensAnUnclaimedRewardAndAllowsReadingLockedTasks() {
        var view = new NovaTerminalViewState();
        view.synchronize(REPAIR, true);
        view.filter(true, REPAIR);
        assertEquals(NovaTask.SURFACE, view.selected());
        view.openTask(NovaTask.LUNAR_SORTIE, REPAIR);
        assertFalse(NovaTask.LUNAR_SORTIE.available(REPAIR.completedMask()));
        assertEquals(NovaTask.LUNAR_SORTIE, view.selected());
        assertFalse(view.completedFilter());
        assertTrue(view.expanded());
    }

    @Test
    void reopeningOrResizingPreservesNavigationButDisconnectClearsIt() {
        ClientShipAiTerminalState.resetConnectionState();
        ClientShipAiTerminalState.beginTerminalSession(9);
        var view = ClientShipAiTerminalState.current().view();
        view.openTask(NovaTask.LIFE_SUPPORT, REPAIR);
        view.taskScroll(45);
        ClientShipAiTerminalState.beginTerminalSession(9);
        ClientShipAiTerminalState.beginTerminalSession(10);
        assertSame(view, ClientShipAiTerminalState.current().view());
        assertEquals(NovaTerminalViewState.Page.TASKS, view.page());
        assertEquals(45, view.taskScroll());
        ClientShipAiTerminalState.resetConnectionState();
        assertEquals(NovaTerminalViewState.Page.COMMS, ClientShipAiTerminalState.current().view().page());
        assertEquals(0, ClientShipAiTerminalState.current().view().taskScroll());
    }

    @Test
    void currentReplyProjectsTheLatestNonPlayerMessageWithoutChangingTheTranscript() {
        ClientShipAiTerminalState.resetConnectionState();
        var session = ClientShipAiTerminalState.current();
        assertEquals(-1, session.latestReplyIndex());
        session.selectOption(Component.literal("question"), Component.literal("reply"),
                ClientShipAiTerminalState.CompletionIntent.NONE, null);
        assertEquals(1, session.latestReplyIndex());
        session.advanceStream();
        assertEquals("r", session.visibleStreamingText());
        assertEquals(2, session.messageCount());
        session.completeStream();
        session.enqueueCue("status", ClientShipAiTerminalState.Speaker.SYSTEM, Component.literal("status"),
                ClientShipAiTerminalState.CompletionIntent.NONE);
        assertEquals(2, session.latestReplyIndex());
        assertEquals(3, session.messageCount());
    }

    @Test
    void rebuildingTheTaskPageCannotReuseAnEarlierActionIdentity() {
        ClientShipAiTerminalState.resetConnectionState();
        ClientShipAiTerminalState.beginTerminalSession(9);
        var session = ClientShipAiTerminalState.current();
        long first = session.nextActionRequestId();
        ClientShipAiTerminalState.beginTerminalSession(9);
        long second = ClientShipAiTerminalState.current().nextActionRequestId();
        long dialogue = session.beginRequest(9, ClientShipAiTerminalState.CompletionKind.CONFIRM_IDENTITY);
        assertTrue(second > first);
        assertTrue(dialogue > second);
    }
}
