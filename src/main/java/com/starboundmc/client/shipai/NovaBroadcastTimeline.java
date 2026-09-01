package com.starboundmc.client.shipai;

import java.util.ArrayDeque;
import java.util.Deque;

/** Pure queue and timing state for the remote N.O.V.A. communication HUD. */
final class NovaBroadcastTimeline
{
    static final int MAX_QUEUED_MESSAGES = 16;
    static final int OPENING_TICKS = 3;
    static final int COMPLETION_HOLD_TICKS = 60;
    static final int CLOSING_TICKS = 5;
    static final int BETWEEN_MESSAGES_TICKS = 4;
    static final int MIN_PROGRESS_DOTS = 6;

    enum Phase
    {
        IDLE,
        OPENING,
        STREAMING,
        HOLDING,
        CLOSING
    }

    record Snapshot(
            long revision,
            String translationKey,
            String fullText,
            String visibleText,
            Phase phase)
    {
        static final Snapshot IDLE = new Snapshot(0L, "", "", "", Phase.IDLE);

        boolean visible()
        {
            return phase != Phase.IDLE;
        }

        boolean speaking()
        {
            return phase == Phase.STREAMING;
        }
    }

    record CompletedMessage(String translationKey, String body)
    {
    }

    record Step(int firstCodePoint, int secondCodePoint, int revealedCount,
                CompletedMessage completedMessage)
    {
        static final Step NONE = new Step(-1, -1, 0, null);

        int revealedCodePoint(int index)
        {
            if (index < 0 || index >= revealedCount)
                throw new IndexOutOfBoundsException(index);
            return index == 0 ? firstCodePoint : secondCodePoint;
        }
    }

    private record QueuedMessage(String translationKey, String body)
    {
    }

    private record ActiveMessage(
            String translationKey,
            String body,
            int totalCodePoints,
            int revealedCodePoints)
    {
    }

    private final Deque<QueuedMessage> pending = new ArrayDeque<>();
    private ActiveMessage active;
    private Snapshot snapshot = Snapshot.IDLE;
    private Phase phase = Phase.IDLE;
    private long revision;
    private int phaseTicks;
    private int pauseTicks;
    private int betweenMessageTicks;

    boolean enqueue(String translationKey, String body)
    {
        if (translationKey == null || translationKey.isBlank()
                || body == null || body.isBlank()
                || pending.size() >= MAX_QUEUED_MESSAGES)
            return false;
        pending.addLast(new QueuedMessage(translationKey, body));
        return true;
    }

    Step tick(boolean paused)
    {
        if (paused)
            return Step.NONE;

        if (active == null)
        {
            if (betweenMessageTicks > 0)
            {
                betweenMessageTicks--;
                return Step.NONE;
            }
            startNext();
            return Step.NONE;
        }

        return switch (phase)
        {
            case OPENING -> tickOpening();
            case STREAMING -> tickStreaming();
            case HOLDING -> tickHolding();
            case CLOSING -> tickClosing();
            case IDLE -> Step.NONE;
        };
    }

    Snapshot snapshot()
    {
        return snapshot;
    }

    int queuedMessageCount()
    {
        return pending.size() + (active == null ? 0 : 1);
    }

    void reset()
    {
        pending.clear();
        active = null;
        snapshot = Snapshot.IDLE;
        phase = Phase.IDLE;
        revision = 0L;
        phaseTicks = 0;
        pauseTicks = 0;
        betweenMessageTicks = 0;
    }

    private Step tickOpening()
    {
        if (++phaseTicks >= OPENING_TICKS)
            setPhase(Phase.STREAMING);
        return Step.NONE;
    }

    private Step tickStreaming()
    {
        if (pauseTicks > 0)
        {
            pauseTicks--;
            return Step.NONE;
        }

        int revealed = active.revealedCodePoints();
        int firstCodePoint = codePointAt(active.body(), revealed);
        int budget = isWideCharacter(firstCodePoint) ? 1 : 2;
        if (isProgressDot(active.body(), revealed)
                || (budget > 1 && revealed + 1 < active.totalCodePoints()
                && isProgressDot(active.body(), revealed + 1)))
            budget = 1;

        int nextRevealed = Math.min(active.totalCodePoints(), revealed + budget);
        int secondCodePoint = nextRevealed - revealed > 1
                ? codePointAt(active.body(), revealed + 1) : -1;
        int lastCodePoint = codePointAt(active.body(), nextRevealed - 1);
        active = new ActiveMessage(active.translationKey(), active.body(),
                active.totalCodePoints(), nextRevealed);
        pauseTicks = punctuationPause(lastCodePoint);

        if (nextRevealed >= active.totalCodePoints())
            setPhase(Phase.HOLDING);
        else
            refreshSnapshot();

        return new Step(firstCodePoint, secondCodePoint,
                nextRevealed - revealed, null);
    }

    private Step tickHolding()
    {
        if (++phaseTicks >= COMPLETION_HOLD_TICKS)
            setPhase(Phase.CLOSING);
        return Step.NONE;
    }

    private Step tickClosing()
    {
        if (++phaseTicks < CLOSING_TICKS)
            return Step.NONE;

        CompletedMessage completed = new CompletedMessage(
                active.translationKey(), active.body());
        active = null;
        phase = Phase.IDLE;
        phaseTicks = 0;
        pauseTicks = 0;
        betweenMessageTicks = BETWEEN_MESSAGES_TICKS;
        refreshSnapshot();
        return new Step(-1, -1, 0, completed);
    }

    private void startNext()
    {
        QueuedMessage next = pending.pollFirst();
        if (next == null)
            return;
        int totalCodePoints = next.body().codePointCount(0, next.body().length());
        active = new ActiveMessage(next.translationKey(), next.body(), totalCodePoints, 0);
        phase = Phase.OPENING;
        phaseTicks = 0;
        pauseTicks = 0;
        refreshSnapshot();
    }

    private void setPhase(Phase nextPhase)
    {
        phase = nextPhase;
        phaseTicks = 0;
        refreshSnapshot();
    }

    private void refreshSnapshot()
    {
        revision++;
        if (active == null)
        {
            snapshot = new Snapshot(revision, "", "", "", Phase.IDLE);
            return;
        }
        int visibleEnd = active.body().offsetByCodePoints(0, active.revealedCodePoints());
        snapshot = new Snapshot(revision, active.translationKey(), active.body(),
                active.body().substring(0, visibleEnd), phase);
    }

    private static int codePointAt(String text, int codePointIndex)
    {
        return text.codePointAt(text.offsetByCodePoints(0, codePointIndex));
    }

    private static boolean isWideCharacter(int codePoint)
    {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    /** Keeps long status-dot runs visibly progressive without slowing ordinary ellipses. */
    private static boolean isProgressDot(String text, int codePointIndex)
    {
        if (codePointAt(text, codePointIndex) != '.')
            return false;

        int total = text.codePointCount(0, text.length());
        int runStart = codePointIndex;
        while (runStart > 0 && codePointAt(text, runStart - 1) == '.')
            runStart--;
        int runEnd = codePointIndex + 1;
        while (runEnd < total && codePointAt(text, runEnd) == '.')
            runEnd++;
        return runEnd - runStart >= MIN_PROGRESS_DOTS;
    }

    private static int punctuationPause(int codePoint)
    {
        return switch (codePoint)
        {
            case '.', '!', '?', '。', '！', '？' -> 3;
            case ',', ';', ':', '，', '；', '：' -> 1;
            default -> 0;
        };
    }
}
