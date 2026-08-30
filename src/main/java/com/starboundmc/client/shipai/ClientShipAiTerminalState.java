package com.starboundmc.client.shipai;

import com.starboundmc.story.SituationTopic;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Connection-scoped presentation state for the N.O.V.A. terminal.
 *
 * <p>The server remains authoritative for every story bit. This class keeps
 * only the transient transcript, the currently streamed line and the local
 * topic selection so closing and reopening the terminal does not erase the
 * conversation currently being read.</p>
 */
public final class ClientShipAiTerminalState
{
    private static Session current = new Session();

    private ClientShipAiTerminalState()
    {
    }

    static Session current()
    {
        return current;
    }

    public static void resetConnectionState()
    {
        current = new Session();
    }

    /** Starts a new menu session without discarding the connection transcript. */
    public static void beginTerminalSession(int containerId)
    {
        current.bindContainer(containerId);
    }

    /** Compatibility helper for callers that do not have a menu id yet. */
    public static void beginTerminalSession()
    {
        current.bindContainer(-1);
    }

    enum Speaker
    {
        SYSTEM("gui.starboundmc.ship_ai.speaker.system"),
        NOVA("gui.starboundmc.ship_ai.speaker.nova"),
        PLAYER("gui.starboundmc.ship_ai.speaker.player");

        private final String translationKey;

        Speaker(String translationKey)
        {
            this.translationKey = translationKey;
        }

        String translationKey()
        {
            return translationKey;
        }
    }

    enum CompletionKind
    {
        NONE,
        CONFIRM_IDENTITY,
        MARK_SITUATION_READ,
        ACTIVATE_SURFACE_MISSION
    }

    record CompletionIntent(CompletionKind kind, SituationTopic topic)
    {
        static final CompletionIntent NONE = new CompletionIntent(CompletionKind.NONE, null);

        CompletionIntent
        {
            Objects.requireNonNull(kind, "kind");
            if ((kind == CompletionKind.MARK_SITUATION_READ) != (topic != null))
                throw new IllegalArgumentException("Only topic completion accepts a topic");
        }

        static CompletionIntent confirmIdentity()
        {
            return new CompletionIntent(CompletionKind.CONFIRM_IDENTITY, null);
        }

        static CompletionIntent markRead(SituationTopic topic)
        {
            return new CompletionIntent(CompletionKind.MARK_SITUATION_READ,
                    Objects.requireNonNull(topic, "topic"));
        }

        static CompletionIntent activateSurfaceMission()
        {
            return new CompletionIntent(CompletionKind.ACTIVATE_SURFACE_MISSION, null);
        }

        boolean isNone()
        {
            return kind == CompletionKind.NONE;
        }
    }

    record Message(Speaker speaker, Component body)
    {
        Message
        {
            Objects.requireNonNull(speaker, "speaker");
            Objects.requireNonNull(body, "body");
        }
    }

    private record Transmission(Speaker speaker, Component body, CompletionIntent completion)
    {
    }

    static final class Session
    {
        private final List<Message> messages = new ArrayList<>();
        private final Queue<Transmission> queuedTransmissions = new ArrayDeque<>();
        private final Set<String> scheduledCueIds = new HashSet<>();
        private int streamingIndex = -1;
        private int revealedCodePoints;
        private CompletionIntent streamingCompletion = CompletionIntent.NONE;
        private SituationTopic selectedTopic;
        private boolean autoFollow = true;
        private float manualScrollPixels;
        private long nextRequestId = 1L;
        private PendingRequest pendingRequest;
        private int activeContainerId = -1;

        int messageCount()
        {
            return messages.size();
        }

        boolean hasMessages()
        {
            return !messages.isEmpty();
        }

        Message message(int index)
        {
            return messages.get(index);
        }

        boolean isStreaming()
        {
            return streamingIndex >= 0;
        }

        boolean isTransmitting()
        {
            return isStreaming() || !queuedTransmissions.isEmpty();
        }

        boolean isStreamingLine(int index)
        {
            return index == streamingIndex;
        }

        int streamingIndex()
        {
            return streamingIndex;
        }

        SituationTopic selectedTopic()
        {
            return selectedTopic;
        }

        boolean autoFollow()
        {
            return autoFollow;
        }

        void setAutoFollow(boolean autoFollow)
        {
            this.autoFollow = autoFollow;
        }

        float manualScrollPixels()
        {
            return manualScrollPixels;
        }

        void setManualScrollPixels(float manualScrollPixels)
        {
            this.manualScrollPixels = Math.max(0.0F, manualScrollPixels);
        }

        PendingRequest pendingRequest()
        {
            return pendingRequest;
        }

        void bindContainer(int containerId)
        {
            if (containerId >= 0 && activeContainerId >= 0 && activeContainerId != containerId)
                pendingRequest = null;
            activeContainerId = containerId;
        }

        long beginRequest(CompletionKind kind)
        {
            return beginRequest(activeContainerId, kind);
        }

        long beginRequest(int containerId, CompletionKind kind)
        {
            if (pendingRequest != null)
                throw new IllegalStateException("A terminal action is already pending");
            long requestId = nextRequestId;
            nextRequestId = nextRequestId == Long.MAX_VALUE ? 1L : nextRequestId + 1L;
            pendingRequest = new PendingRequest(containerId, requestId, kind);
            return requestId;
        }

        boolean acknowledge(long requestId)
        {
            return acknowledge(activeContainerId, requestId);
        }

        boolean acknowledge(int containerId, long requestId)
        {
            if (pendingRequest == null || pendingRequest.requestId() != requestId)
                return false;
            if (containerId >= 0 && pendingRequest.containerId() >= 0
                    && pendingRequest.containerId() != containerId)
                return false;
            pendingRequest = null;
            return true;
        }

        String visibleStreamingText()
        {
            if (!isStreaming())
                return "";
            int[] codePoints = localizedBody(streamingIndex).codePoints().toArray();
            int visibleLength = Math.min(revealedCodePoints, codePoints.length);
            return new String(codePoints, 0, visibleLength);
        }

        boolean enqueueCue(String cueId, Speaker speaker, Component body,
                           CompletionIntent completion)
        {
            Objects.requireNonNull(cueId, "cueId");
            if (!scheduledCueIds.add(cueId))
                return false;
            enqueue(speaker, body, completion);
            return true;
        }

        int selectOption(Component option, Component response,
                         CompletionIntent completion, SituationTopic topic)
        {
            if (isTransmitting())
                return -1;
            int firstNewIndex = messages.size();
            messages.add(new Message(Speaker.PLAYER, Objects.requireNonNull(option, "option")));
            selectedTopic = topic;
            enqueue(Speaker.NOVA, response, completion);
            return firstNewIndex;
        }

        /** @return the server intent unlocked by the completed line, if any. */
        CompletionIntent advanceStream()
        {
            if (!isStreaming())
                return CompletionIntent.NONE;
            String body = localizedBody(streamingIndex);
            int length = body.codePointCount(0, body.length());
            revealedCodePoints = Math.min(length, revealedCodePoints + 1);
            return revealedCodePoints >= length ? finishStream() : CompletionIntent.NONE;
        }

        CompletionIntent completeStream()
        {
            return isStreaming() ? finishStream() : CompletionIntent.NONE;
        }

        private void enqueue(Speaker speaker, Component body, CompletionIntent completion)
        {
            queuedTransmissions.add(new Transmission(
                    Objects.requireNonNull(speaker, "speaker"),
                    Objects.requireNonNull(body, "body"),
                    Objects.requireNonNull(completion, "completion")));
            startNextTransmission();
        }

        private CompletionIntent finishStream()
        {
            CompletionIntent completed = streamingCompletion;
            streamingIndex = -1;
            revealedCodePoints = 0;
            streamingCompletion = CompletionIntent.NONE;
            startNextTransmission();
            return completed;
        }

        private void startNextTransmission()
        {
            if (isStreaming())
                return;
            Transmission next = queuedTransmissions.poll();
            if (next == null)
                return;
            messages.add(new Message(next.speaker(), next.body()));
            streamingIndex = messages.size() - 1;
            revealedCodePoints = 0;
            streamingCompletion = next.completion();
        }

        private String localizedBody(int index)
        {
            return messages.get(index).body().getString();
        }
    }

    record PendingRequest(int containerId, long requestId, CompletionKind kind)
    {
    }
}
