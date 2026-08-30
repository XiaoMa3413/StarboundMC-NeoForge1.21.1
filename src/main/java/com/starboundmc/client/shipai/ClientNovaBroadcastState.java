package com.starboundmc.client.shipai;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayDeque;
import java.util.Deque;

/** Client-side queue and in-place chat typewriter timing for N.O.V.A. transmissions. */
public final class ClientNovaBroadcastState
{
    private static final int MAX_QUEUED_MESSAGES = 16;
    private static final int COMPLETION_HOLD_TICKS = 10;
    private static final int BETWEEN_MESSAGES_TICKS = 4;
    private static final int MIN_PROGRESS_DOTS = 6;

    private static final Deque<String> pendingTranslationKeys = new ArrayDeque<>();
    private static ActiveMessage active;
    private static int pauseTicks;
    private static int completionTicks;
    private static int betweenMessageTicks;

    private ClientNovaBroadcastState()
    {
    }

    /** Queues a localized transmission without interrupting the line already playing. */
    public static void enqueue(String translationKey)
    {
        if (translationKey == null || translationKey.isBlank()
                || pendingTranslationKeys.size() >= MAX_QUEUED_MESSAGES)
            return;
        pendingTranslationKeys.addLast(translationKey);
    }

    /** Advances one mutable vanilla-chat entry once per client tick. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.getChat().isChatFocused())
            return;

        if (active == null)
        {
            if (betweenMessageTicks > 0)
            {
                betweenMessageTicks--;
                return;
            }
            startNext(minecraft);
            return;
        }

        if (active.revealedCodePoints() < active.totalCodePoints())
        {
            if (pauseTicks > 0)
            {
                pauseTicks--;
                return;
            }
            revealNextStep(minecraft);
            return;
        }

        if (++completionTicks < COMPLETION_HOLD_TICKS)
            return;

        active = null;
        completionTicks = 0;
        betweenMessageTicks = BETWEEN_MESSAGES_TICKS;
    }

    /** Completes an unfinished line before clearing session-local queue state. */
    public static void resetConnectionState()
    {
        if (active != null)
            revealRemaining(Minecraft.getInstance());
        pendingTranslationKeys.clear();
        active = null;
        pauseTicks = 0;
        completionTicks = 0;
        betweenMessageTicks = 0;
    }

    static int queuedMessageCount()
    {
        return pendingTranslationKeys.size() + (active == null ? 0 : 1);
    }

    private static void startNext(Minecraft minecraft)
    {
        String translationKey = pendingTranslationKeys.pollFirst();
        if (translationKey == null)
            return;
        String body = Component.translatable(translationKey).getString();
        MutableComponent chatEntry = Component.literal("[N.O.V.A.] ")
                .withStyle(ChatFormatting.AQUA);
        active = new ActiveMessage(body,
                body.codePointCount(0, body.length()), 0, chatEntry);
        minecraft.gui.getChat().addMessage(chatEntry);
        pauseTicks = 0;
        completionTicks = 0;
        revealNextStep(minecraft);
    }

    private static void revealNextStep(Minecraft minecraft)
    {
        int revealed = active.revealedCodePoints();
        int firstCodePoint = codePointAt(active.body(), revealed);
        int budget = isWideCharacter(firstCodePoint) ? 1 : 2;
        if (isProgressDot(active.body(), revealed)
                || (budget > 1 && revealed + 1 < active.totalCodePoints()
                && isProgressDot(active.body(), revealed + 1)))
            budget = 1;
        int nextRevealed = Math.min(active.totalCodePoints(), revealed + budget);
        appendRange(minecraft, revealed, nextRevealed);
        int lastCodePoint = codePointAt(active.body(), nextRevealed - 1);
        active = new ActiveMessage(active.body(), active.totalCodePoints(),
                nextRevealed, active.chatEntry());
        pauseTicks = punctuationPause(lastCodePoint);
    }

    private static void revealRemaining(Minecraft minecraft)
    {
        if (active.revealedCodePoints() >= active.totalCodePoints())
            return;
        appendRange(minecraft, active.revealedCodePoints(), active.totalCodePoints());
        active = new ActiveMessage(active.body(), active.totalCodePoints(),
                active.totalCodePoints(), active.chatEntry());
    }

    private static void appendRange(Minecraft minecraft, int fromCodePoint, int toCodePoint)
    {
        int start = active.body().offsetByCodePoints(0, fromCodePoint);
        int end = active.body().offsetByCodePoints(0, toCodePoint);
        active.chatEntry().append(Component.literal(active.body().substring(start, end))
                .withStyle(ChatFormatting.WHITE));
        minecraft.gui.getChat().rescaleChat();
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

    /** Keeps long status-dot runs visibly progressive without slowing normal ellipses. */
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

    private record ActiveMessage(
            String body,
            int totalCodePoints,
            int revealedCodePoints,
            MutableComponent chatEntry)
    {
    }
}
