package com.starboundmc.client.shipai;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client-side presentation state for queued N.O.V.A. remote transmissions. */
public final class ClientNovaBroadcastState
{
    private static final NovaBroadcastTimeline timeline = new NovaBroadcastTimeline();
    private static NovaDialogueSounds dialogueSounds = new NovaDialogueSounds();
    private static int textPulseSequence;

    private ClientNovaBroadcastState()
    {
    }

    /** Queues a localized transmission without interrupting the line already playing. */
    public static void enqueue(String translationKey)
    {
        if (translationKey == null || translationKey.isBlank())
            return;
        timeline.enqueue(translationKey, Component.translatable(translationKey).getString());
    }

    /** Advances the HUD typewriter once per client tick. */
    public static void tick()
    {
        Minecraft minecraft = Minecraft.getInstance();
        NovaBroadcastTimeline.Step step = timeline.tick(isPresentationPaused(minecraft));
        for (int index = 0; index < step.revealedCount(); index++)
        {
            int codePoint = step.revealedCodePoint(index);
            dialogueSounds.onCodePointRevealed(codePoint);
            textPulseSequence++;
        }

        NovaBroadcastTimeline.CompletedMessage completed = step.completedMessage();
        if (completed != null && minecraft.player != null)
            minecraft.gui.getChat().addMessage(historyMessage(completed.body()));
    }

    /** Clears connection-local queue, animation and sound cadence state. */
    public static void resetConnectionState()
    {
        timeline.reset();
        dialogueSounds = new NovaDialogueSounds();
        textPulseSequence = 0;
    }

    static NovaBroadcastTimeline.Snapshot snapshot()
    {
        return timeline.snapshot();
    }

    static int textPulseSequence()
    {
        return textPulseSequence;
    }

    static boolean shouldRender()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return timeline.snapshot().visible() && !isPresentationPaused(minecraft);
    }

    static int queuedMessageCount()
    {
        return timeline.queuedMessageCount();
    }

    private static boolean isPresentationPaused(Minecraft minecraft)
    {
        return minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui
                || minecraft.screen != null;
    }

    private static Component historyMessage(String body)
    {
        return Component.literal("[N.O.V.A.] ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(body).withStyle(ChatFormatting.WHITE));
    }
}
