package com.starboundmc.client.shipai;

import com.starboundmc.story.SituationTopic;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientShipAiTerminalStateTest
{
    @BeforeEach
    void resetSession()
    {
        ClientShipAiTerminalState.resetConnectionState();
    }

    @Test
    void aFreshAuthoritativeSessionContainsNoDemoTranscript()
    {
        assertEquals(0, ClientShipAiTerminalState.current().messageCount());
    }

    @Test
    void topicReadIntentIsReleasedOnlyAfterTheResponseCompletes()
    {
        ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();
        SituationTopic topic = SituationTopic.INCIDENT;
        int firstIndex = session.selectOption(
                Component.literal("question"),
                Component.literal("answer"),
                ClientShipAiTerminalState.CompletionIntent.markRead(topic),
                topic);

        assertEquals(0, firstIndex);
        assertTrue(session.isStreaming());
        assertSame(topic, session.selectedTopic());
        assertTrue(session.advanceStream().isNone());

        ClientShipAiTerminalState.CompletionIntent completed = session.completeStream();
        assertEquals(ClientShipAiTerminalState.CompletionKind.MARK_SITUATION_READ,
                completed.kind());
        assertSame(topic, completed.topic());
        assertFalse(session.isStreaming());
        assertTrue(session.completeStream().isNone());
    }

    @Test
    void anAutomaticCueIsQueuedOnlyOnceAcrossRootRebuilds()
    {
        ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();

        assertTrue(session.enqueueCue("first_contact",
                ClientShipAiTerminalState.Speaker.NOVA,
                Component.literal("hello"),
                ClientShipAiTerminalState.CompletionIntent.confirmIdentity()));
        assertFalse(session.enqueueCue("first_contact",
                ClientShipAiTerminalState.Speaker.NOVA,
                Component.literal("hello"),
                ClientShipAiTerminalState.CompletionIntent.confirmIdentity()));
        assertEquals(1, session.messageCount());
    }

    @Test
    void anUnrelatedReplyCannotClearThePendingRequest()
    {
        ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();
        long requestId = session.beginRequest(
                ClientShipAiTerminalState.CompletionKind.MARK_SITUATION_READ);

        assertFalse(session.acknowledge(requestId + 1L));
        assertTrue(session.pendingRequest() != null);
        assertTrue(session.acknowledge(requestId));
        assertFalse(session.pendingRequest() != null);
    }

    @Test
    void rebindingTheSameContainerPreservesPendingRequest()
    {
        ClientShipAiTerminalState.beginTerminalSession(12);
        ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();
        long requestId = session.beginRequest(12,
                ClientShipAiTerminalState.CompletionKind.CONFIRM_IDENTITY);

        ClientShipAiTerminalState.beginTerminalSession(12);

        assertEquals(requestId, session.pendingRequest().requestId());
        assertTrue(session.acknowledge(12, requestId));
    }

    @Test
    void switchingContainersDropsAStalePendingRequest()
    {
        ClientShipAiTerminalState.beginTerminalSession(12);
        ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();
        session.beginRequest(12, ClientShipAiTerminalState.CompletionKind.CONFIRM_IDENTITY);

        ClientShipAiTerminalState.beginTerminalSession(13);

        assertTrue(session.pendingRequest() == null);
    }
}
